package com.example.kit.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.kit.R;
import com.example.kit.models.ClusterMarker;
import com.example.kit.models.Contact;
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

public class MapFragment extends DBGeoFragment
        implements OnMapReadyCallback {

    //TODO
    // navigate to Profile bubble when user is clicked

    //Tag
    private static final String TAG = "MapFragment";

    //Map
    protected GoogleMap mGoogleMap;
    protected LatLngBounds mMapBoundary;
    protected ClusterManager<ClusterMarker> mClusterManager;
    protected MyClusterManagerRenderer mClusterManagerRenderer;
    protected ArrayList<ClusterMarker> mClusterMarkers = new ArrayList<>();

    //Runnable
    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;

    //Vars
    protected ListenerRegistration mContactListEventListener;
    protected ArrayList<Contact> mContactList = new ArrayList<>();
    protected ArrayList<UserLocation> mContactLocations = new ArrayList<>();

    //Widgets
    protected MapView mMapView;

    //Callbacks
    MapCallBack getData;

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public MapFragment() {
        super();
    }

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        Log.d(TAG, "before: ");
        Log.d(TAG, "after: ");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        init(view);
        initGoogleMap(savedInstanceState);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        startUserLocationsRunnable();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if(mContactListEventListener != null){
            mContactListEventListener.remove();
        }
        stopLocationUpdates();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mMapView.onSaveInstanceState(mapViewBundle);
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void init(View v){
        mMapView = v.findViewById(R.id.frag_map);
    }

    /*
    ----------------------------- DB ---------------------------------
    */

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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        getData =  (MapCallBack)context;
        mUserLocation = getData.getUserPos();
        mContactLocations = getData.getUserLocations();
        setCameraView();
    }


    private void startUserLocationsRunnable() {
        Log.d(TAG, "startUserLocationsRunnable: starting runnable for retrieving updated locations.");
        mHandler.postDelayed(mRunnable = new Runnable() {
            @Override
            public void run() {
                retrieveUserLocations();
                mHandler.postDelayed(mRunnable, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    private void stopLocationUpdates() {
        mHandler.removeCallbacks(mRunnable);
    }

    private void retrieveUserLocations() {
        Log.d(TAG, "retrieveUserLocations: retrieving location of all users in the chatroom");
        try {
            for (final ClusterMarker clusterMarker : mClusterMarkers) {
                DocumentReference userLocationRef = FirebaseFirestore.getInstance()
                        .collection(getString(R.string.collection_user_locations))
                        .document(clusterMarker.getUser().getUser_id());
                userLocationRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            final UserLocation updatedUserLocation = task.getResult().toObject(UserLocation.class);
                            // update the location
                            for (int i = 0; i < mClusterMarkers.size(); i++) {
                                try {
                                    if (mClusterMarkers.get(i).getUser().getUser_id().equals(updatedUserLocation.getUser().getUser_id())) {
                                        LatLng updatedLatLng = new LatLng(
                                                updatedUserLocation.getGeo_point().getLatitude(),
                                                updatedUserLocation.getGeo_point().getLongitude()
                                        );
                                        mClusterMarkers.get(i).setPosition(updatedLatLng);
                                        mClusterManagerRenderer.setUpdateMarker(mClusterMarkers.get(i));
                                    }
                                } catch (NullPointerException e) {
                                    Log.e(TAG, "retrieveUserLocations: NullPointerException: " + e.getMessage());
                                }
                            }
                        }
                    }
                });
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "retrieveUserLocations: Fragment was destroyed during Firestore query. Ending query." + e.getMessage());
        }
    }

    /*
    ----------------------------- Map ---------------------------------
    */

    protected void initGoogleMap(Bundle savedInstanceState) {
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "onMapReady: ");
        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        mGoogleMap = map;
        addMapMarkers();
    }

    private void addMapMarkers() {
        if (mGoogleMap != null) {
            if (mClusterManager == null) {
                mClusterManager = new ClusterManager<ClusterMarker>(mActivity.getApplicationContext(), mGoogleMap);
            }
            if (mClusterManagerRenderer == null) {
                mClusterManagerRenderer = new MyClusterManagerRenderer(
                        mActivity,
                        mGoogleMap,
                        mClusterManager
                );
                mClusterManager.setRenderer(mClusterManagerRenderer);
            }
            System.out.println(mContactLocations.toString());
            for (UserLocation userLocation : mContactLocations) {
                Log.d(TAG, "addMapMarkers: location: " + userLocation.getGeo_point().toString());
                try {
                    String snippet = "";
                    if (userLocation.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())) {
                        snippet = "This is you";
                    } else {
                        snippet = "Determine route to " + userLocation.getUser().getUsername() + "?";
                    }

                    int avatar = R.drawable.cartman_cop; // set the default avatar
                    try {
                        avatar = Integer.parseInt(userLocation.getUser().getAvatar());
                    } catch (NumberFormatException e) {
                        Log.d(TAG, "addMapMarkers: no avatar for " + userLocation.getUser().getUsername() + ", setting default.");
                    }
                    ClusterMarker newClusterMarker = new ClusterMarker(
                            new LatLng(userLocation.getGeo_point().getLatitude(), userLocation.getGeo_point().getLongitude()),
                            userLocation.getUser().getUsername(),
                            snippet,
                            avatar,
                            userLocation.getUser()
                    );
                    mClusterManager.addItem(newClusterMarker);
                    mClusterMarkers.add(newClusterMarker);

                } catch (NullPointerException e) {
                    Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage());
                }
            }
            mClusterManager.cluster();
            setCameraView();
        }
    }

    private void setCameraView() {
        if(mUserLocation != null) {
            double lat = mUserLocation.getGeo_point().getLatitude();
            double lon = mUserLocation.getGeo_point().getLongitude();
            double bb = lat - .1;
            double lb = lon - .1;
            double tb = lat + .1;
            double rb = lon + .1;
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = 4;
            mMapBoundary = new LatLngBounds(new LatLng(bb, lb), new LatLng(tb, rb));
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mMapBoundary, width, height, padding));
        }
    }

    public interface MapCallBack {
        UserLocation getUserPos();
        ArrayList<UserLocation> getUserLocations();
    }
}

