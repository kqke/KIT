package com.example.kit.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kit.R;
import com.example.kit.adapters.UserRecyclerAdapter;
import com.example.kit.models.ClusterMarker;
import com.example.kit.models.User;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.models.UserLocation;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import static com.example.kit.Constants.MAPVIEW_BUNDLE_KEY;

public class UserListFragment extends MapFragment
        implements OnMapReadyCallback,
        ContactRecyclerAdapter.ContactsRecyclerClickListener{

    private static final String TAG = "UserListFragment";

    //widgets
    private RecyclerView mUserListRecyclerView;

    //RecyclerView
    private ContactRecyclerAdapter mUserRecyclerAdapter;


    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public static UserListFragment newInstance() {
        return new UserListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mContactLocations.size() == 0) {
            if (getArguments() != null) {
                mContactList = getArguments().getParcelableArrayList(getString(R.string.intent_user_list));
                mContactLocations = getArguments().getParcelableArrayList(getString(R.string.intent_user_locations));
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);
        mUserListRecyclerView = view.findViewById(R.id.user_list_recycler_view);
        mMapView = view.findViewById(R.id.user_list_map);
        initUserListRecyclerView();
        initGoogleMap(savedInstanceState);
        setUserPosition();
        return view;
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void setUserPosition() {
        for (UserLocation ul : mContactLocations) {
            if (ul.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())) {
                mUserLocation = ul;
            }
        }
    }

    private void initUserListRecyclerView() {
        mUserRecyclerAdapter = new ContactRecyclerAdapter(mContactList,
                this, R.layout.layout_contact_list_item);
        mUserListRecyclerView.setAdapter(mUserRecyclerAdapter);
        mUserListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

        /*
    ----------------------------- nav ---------------------------------
    */

    private void navContactActivity(Contact contact){
        //TODO
        // ContactActivity is deprecated
//        Intent intent = new Intent(mActivity, ContactActivity.class);
//        intent.putExtra(getString(R.string.intent_contact), contact);
//        startActivityForResult(intent, 0);
    }

    @Override
    public void onContactSelected(int position) {
        //TODO
        // ContactActivity is deprecated
        navContactActivity(mContactList.get(position));
    }

    @Override
    public void onAcceptSelected(int position) {

    }

    @Override
    public void onRejectSelected(int position) {

    }

    @Override
    public void onDeleteSelected(int position) {

    }

    @Override
    public void onContactLongClick(int position) {

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


    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;

    private void startUserLocationsRunnable(){
        Log.d(TAG, "startUserLocationsRunnable: starting runnable for retrieving updated locations.");
        mHandler.postDelayed(mRunnable = new Runnable() {
            @Override
            public void run() {
                retrieveUserLocations();
                mHandler.postDelayed(mRunnable, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    private void stopLocationUpdates(){
        mHandler.removeCallbacks(mRunnable);
    }

    private void retrieveUserLocations(){
        Log.d(TAG, "retrieveUserLocations: retrieving location of all users in the chatroom.");

        try{
            for(final ClusterMarker clusterMarker: mClusterMarkers){
                DocumentReference userLocationRef = FirebaseFirestore.getInstance()
                        .collection(getString(R.string.collection_user_locations))
                        .document(clusterMarker.getUser().getUser_id());
                userLocationRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
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
                                    }
                                } catch (NullPointerException e) {
                                    Log.e(TAG, "retrieveUserLocations: NullPointerException: " + e.getMessage());
                                }
                            }
                        }
                    }
                });
            }
        }catch (IllegalStateException e){
            Log.e(TAG, "retrieveUserLocations: Fragment was destroyed during Firestore query. Ending query." + e.getMessage() );
        }
    }
}
