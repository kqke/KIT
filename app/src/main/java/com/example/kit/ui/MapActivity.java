package com.example.kit.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.adapters.UserRecyclerAdapter;
import com.example.kit.models.ClusterMarker;
import com.example.kit.models.Contact;
import com.example.kit.models.User;
import com.example.kit.models.UserLocation;
import com.example.kit.util.MyClusterManagerRenderer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;

import static com.example.kit.Constants.MAPVIEW_BUNDLE_KEY;

public class MapActivity extends AppCompatActivity{

    private static final String TAG = "MapActivity";

    //widgets
    private RecyclerView mUserListRecyclerView;
    private MapView mMapView;


    //vars
    private ListenerRegistration mContactListEventListener;
    private FirebaseFirestore mDb;
    private ArrayList<Contact> mContactList = new ArrayList<>();
    private ArrayList<UserLocation> mContactLocations = new ArrayList<>();
    private UserLocation mUserPosition;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMapView = findViewById(R.id.user_list_map);
        mDb = FirebaseFirestore.getInstance();
        setContentView(R.layout.activity_map);
        getContacts();
        Log.d(TAG, "before: ");
        setUserPosition();
        Log.d(TAG, "after: ");

    }

    private void inflateMapFragment(){
        MapFragment fragment = MapFragment.newInstance();
        Bundle bundle = new Bundle();
        bundle.putParcelable("userPos", mUserPosition);
        bundle.putParcelableArrayList(getString(R.string.intent_user_list), mContactList);
        bundle.putParcelableArrayList(getString(R.string.intent_user_locations), mContactLocations);
        fragment.setArguments(bundle);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
        transaction.replace(R.id.map_container, fragment, getString(R.string.fragment_map_frag));
        transaction.addToBackStack(getString(R.string.fragment_map_frag));
        transaction.commit();
    }

    private void getContacts() {
        CollectionReference contactsRef = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_contacts));

        mContactListEventListener = contactsRef
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "onEvent: Listen failed.", e);
                            return;
                        }

                        if (queryDocumentSnapshots != null) {

                            // Clear the list and add all the users again
                            mContactList.clear();
                            mContactList = new ArrayList<>();

                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                Contact contact = doc.toObject(Contact.class);
                                mContactList.add(contact);
                                System.out.println(contact.getCid());
                                getContactLocation(contact);
                            }

                            Log.d(TAG, "onEvent: user list size: " + mContactList.size());
                        }
                    }
                });
    }

    private void getContactLocation(Contact contact) {
        DocumentReference locRef = mDb.collection(getString(R.string.collection_user_locations)).document(contact.getCid());

        locRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().toObject(UserLocation.class) != null) {
                        mContactLocations.add(task.getResult().toObject(UserLocation.class));
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getContacts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mContactListEventListener != null){
            mContactListEventListener.remove();
        }
    }

    private void setUserPosition() {
        DocumentReference userLocRef =
                mDb.collection(getString(R.string.collection_user_locations)).document(FirebaseAuth.getInstance().getUid());
        userLocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful()){return;}
                mUserPosition = task.getResult().toObject(UserLocation.class);
                mContactLocations.add(mUserPosition);
                inflateMapFragment();
            }
        });
    }

}


//
//    private void setCameraView() {
//        double lat = mUserPosition.getGeo_point().getLatitude();
//        double lon = mUserPosition.getGeo_point().getLongitude();
//        double bb = lat - .1;
//        double lb = lon - .1;
//        double tb = lat + .1;
//        double rb = lon + .1;
//
//        mMapBoundary = new LatLngBounds(new LatLng(bb, lb), new LatLng(tb, rb));
//
//        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mMapBoundary, 0));
//    }
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.activity_map, container, false);
//        mMapView = view.findViewById(R.id.user_list_map);
//
//        initUserListRecyclerView();
//        initGoogleMap(savedInstanceState);
//
//        setUserPosition();
//
//        return view;
//    }
//
//    private Handler mHandler = new Handler();
//    private Runnable mRunnable;
//    private static final int LOCATION_UPDATE_INTERVAL = 3000;
//
//    private void startUserLocationsRunnable() {
//        Log.d(TAG, "startUserLocationsRunnable: starting runnable for retrieving updated locations.");
//        mHandler.postDelayed(mRunnable = new Runnable() {
//            @Override
//            public void run() {
//                retrieveUserLocations();
//                mHandler.postDelayed(mRunnable, LOCATION_UPDATE_INTERVAL);
//            }
//        }, LOCATION_UPDATE_INTERVAL);
//    }
//
//    private void stopLocationUpdates() {
//        mHandler.removeCallbacks(mRunnable);
//    }
//
//
//    private void retrieveUserLocations() {
//        Log.d(TAG, "retrieveUserLocations: retrieving location of all users in the chatroom.");
//        try {
//            for (final ClusterMarker clusterMarker : mClusterMarkers) {
//                DocumentReference userLocationRef = FirebaseFirestore.getInstance()
//                        .collection(getString(R.string.collection_user_locations))
//                        .document(clusterMarker.getUser().getUser_id());
//                userLocationRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                        if (task.isSuccessful()) {
//                            final UserLocation updatedUserLocation = task.getResult().toObject(UserLocation.class);
//// update the location
//                            for (int i = 0; i < mClusterMarkers.size(); i++) {
//                                try {
//                                    if (mClusterMarkers.get(i).getUser().getUser_id().equals(updatedUserLocation.getUser().getUser_id())) {
//                                        LatLng updatedLatLng = new LatLng(
//                                                updatedUserLocation.getGeo_point().getLatitude(),
//                                                updatedUserLocation.getGeo_point().getLongitude()
//                                        );
//                                        mClusterMarkers.get(i).setPosition(updatedLatLng);
//                                        mClusterManagerRenderer.setUpdateMarker(mClusterMarkers.get(i));
//                                    }
//                                } catch (NullPointerException e) {
//                                    Log.e(TAG, "retrieveUserLocations: NullPointerException: " + e.getMessage());
//                                }
//                            }
//                        }
//                    }
//                });
//            }
//        } catch (IllegalStateException e) {
//            Log.e(TAG, "retrieveUserLocations: Fragment was destroyed during Firestore query. Ending query." + e.getMessage());
//        }
//    }
//
//    private void initGoogleMap(Bundle savedInstanceState) {
//        // *** IMPORTANT ***
//        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
//        // objects or sub-Bundles.
//        Bundle mapViewBundle = null;
//        if (savedInstanceState != null) {
//            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
//        }
//
//        mMapView.onCreate(mapViewBundle);
//
//        mMapView.getMapAsync(this);
//    }
//
//    private void initUserListRecyclerView() {
////        mUserRecyclerAdapter = new UserRecyclerAdapter(mUserList);
//        mUserListRecyclerView.setAdapter(mUserRecyclerAdapter);
//        mUserListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
//    }
//
//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//
//        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
//        if (mapViewBundle == null) {
//            mapViewBundle = new Bundle();
//            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
//        }
//
//        mMapView.onSaveInstanceState(mapViewBundle);
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        mMapView.onResume();
//        startUserLocationsRunnable();
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        mMapView.onStart();
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        mMapView.onStop();
//    }
//
//    @Override
//    public void onMapReady(GoogleMap map) {
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED
//                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        map.setMyLocationEnabled(true);
//        mGoogleMap = map;
//        addMapMarkers();
//    }
//
//    @Override
//    public void onPause() {
//        mMapView.onPause();
//        super.onPause();
//    }
//
//    @Override
//    public void onDestroy() {
//        mMapView.onDestroy();
//        super.onDestroy();
//        stopLocationUpdates();
//    }
//
//    @Override
//    public void onLowMemory() {
//        super.onLowMemory();
//        mMapView.onLowMemory();
//    }
//
//    private void addMapMarkers() {
//
//        if (mGoogleMap != null) {
//
//            if (mClusterManager == null) {
//                mClusterManager = new ClusterManager<ClusterMarker>(this.getApplicationContext(), mGoogleMap);
//            }
//            if (mClusterManagerRenderer == null) {
//                mClusterManagerRenderer = new MyClusterManagerRenderer(
//                        this,
//                        mGoogleMap,
//                        mClusterManager
//                );
//                mClusterManager.setRenderer(mClusterManagerRenderer);
//            }
//
//            System.out.println(mContactLocations.toString());
//            for (UserLocation contactLocation : mContactLocations) {
//
//                Log.d(TAG, "addMapMarkers: location: " + contactLocation.getGeo_point().toString());
//                try {
//                    String snippet = "";
//                    if (contactLocation.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())) {
//                        snippet = "This is you";
//                    } else {
//                        snippet = "Determine route to " + contactLocation.getUser().getUsername() + "?";
//                    }
//
//                    int avatar = R.drawable.cartman_cop; // set the default avatar
//                    try {
//                        avatar = Integer.parseInt(contactLocation.getUser().getAvatar());
//                    } catch (NumberFormatException e) {
//                        Log.d(TAG, "addMapMarkers: no avatar for " + contactLocation.getUser().getUsername() + ", setting default.");
//                    }
//                    ClusterMarker newClusterMarker = new ClusterMarker(
//                            new LatLng(contactLocation.getGeo_point().getLatitude(), contactLocation.getGeo_point().getLongitude()),
//                            contactLocation.getUser().getUsername(),
//                            snippet,
//                            avatar,
//                            contactLocation.getUser()
//                    );
//                    mClusterManager.addItem(newClusterMarker);
//                    mClusterMarkers.add(newClusterMarker);
//
//                } catch (NullPointerException e) {
//                    Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage());
//                }
//
//            }
//            mClusterManager.cluster();
//
//            setCameraView();
//        }
//    }
//}
