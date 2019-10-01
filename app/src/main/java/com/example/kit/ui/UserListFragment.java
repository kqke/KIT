package com.example.kit.ui;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.kit.R;
import com.example.kit.adapters.UserRecyclerAdapter;
import com.example.kit.models.ClusterMarker;
import com.example.kit.models.User;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.models.UserLocation;
import com.example.kit.util.ViewWeightAnimationWrapper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.maps.GeoApiContext;

import java.util.ArrayList;

import static com.example.kit.Constants.FRIENDS;
import static com.example.kit.Constants.MAPVIEW_BUNDLE_KEY;
import static com.example.kit.Constants.NOT_FRIENDS;

public class UserListFragment extends MapFragment
        implements OnMapReadyCallback,
        ContactRecyclerAdapter.ContactsRecyclerClickListener,
        View.OnClickListener {

    private static final String TAG = "UserListFragment";

    //widgets
    private RecyclerView mUserListRecyclerView;

    //RecyclerView
    private ContactRecyclerAdapter mUserRecyclerAdapter;

    //Callback
    ChatFragment.ChatroomCallback callback;
    UserListCallback ulCallback;

    //map
    RelativeLayout mMapContainer;
    ImageButton expandBut;
    ImageButton resetBut;

    //vars
    ArrayList<User> mUserList;
    private static final int MAP_LAYOUT_STATE_CONTRACTED = 0;
    private static final int MAP_LAYOUT_STATE_EXPANDED = 1;
    private int mMapLayoutState = 0;


    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public static UserListFragment newInstance() {
        return new UserListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null){
            mMapLayoutState = savedInstanceState.getInt("mstate");
            mUserList = (ArrayList<User>)savedInstanceState.getSerializable("userList");
        }
        if (mContactLocations.size() == 0) {
            if (getArguments() != null) {
                mContactList = getArguments().getParcelableArrayList(getString(R.string.intent_user_list));
                mContactLocations = getArguments().getParcelableArrayList(getString(R.string.intent_user_locations));
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        callback = (ChatFragment.ChatroomCallback) context;
        ulCallback = (UserListCallback) context;
        mUserList = callback.getUserList();
        mContactList = ulCallback.getContacts();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);
        mUserListRecyclerView = view.findViewById(R.id.user_list_recycler_view);
        mMapView = view.findViewById(R.id.user_list_map);
        mMapContainer = view.findViewById(R.id.ul_map_container);
        expandBut = view.findViewById(R.id.btn_full_screen_map);
        resetBut = view.findViewById(R.id.ul_btn_reset_map);
        expandBut.setOnClickListener(this);
        resetBut.setOnClickListener(this);
        initUserListRecyclerView();
        initGoogleMap(savedInstanceState);
        setUserPosition();
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("mstate", mMapLayoutState);
        outState.putSerializable("userList", mUserList);
        outState.putSerializable("contactList", mContactList);
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
        boolean found;
        for (User user: mUserList){
            found = false;
            for (Contact contact: mContactList){
                if (contact.getCid().equals(user.getUser_id())){
                    found = true;
                    break;
                }
            }
            if (!found){
                mContactList.add(new Contact(user.getUsername(), user.getEmail(), user.getAvatar(), user.getUser_id(), NOT_FRIENDS));
            }
        }
        mUserRecyclerAdapter = new ContactRecyclerAdapter(mContactList,
                this, R.layout.layout_contact_list_item, getContext());
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
        String state = "";
        Contact cont = mContactList.get(position);
        if (cont.getCid().equals(FirebaseAuth.getInstance().getUid())){
            ulCallback.navSettingsActivity();
            return;

        }
        if (cont.getStatus().equals(NOT_FRIENDS)){
            state = NOT_FRIENDS;
        }
        else {
            state = FRIENDS;
        }
        ulCallback.navContactFragment(mContactList.get(position), state);
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
        if (mGeoApiContext == null){
            mGeoApiContext = new GeoApiContext.Builder().apiKey(getString(R.string.google_maps_api_key)).build();
        }
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

    private void expandMapAnimation(){
        ViewWeightAnimationWrapper mapAnimationWrapper = new ViewWeightAnimationWrapper(mMapContainer);
        ObjectAnimator mapAnimation = ObjectAnimator.ofFloat(mapAnimationWrapper,
                "weight",
                50,
                100);
        mapAnimation.setDuration(800);

        ViewWeightAnimationWrapper recyclerAnimationWrapper = new ViewWeightAnimationWrapper(mUserListRecyclerView);
        ObjectAnimator recyclerAnimation = ObjectAnimator.ofFloat(recyclerAnimationWrapper,
                "weight",
                50,
                0);
        recyclerAnimation.setDuration(800);

        recyclerAnimation.start();
        mapAnimation.start();

        Log.d(TAG, "expandMapAnimation: map weight ");
    }

    private void contractMapAnimation(){
        ViewWeightAnimationWrapper mapAnimationWrapper = new ViewWeightAnimationWrapper(mMapContainer);
        ObjectAnimator mapAnimation = ObjectAnimator.ofFloat(mapAnimationWrapper,
                "weight",
                100,
                50);
        mapAnimation.setDuration(800);

        ViewWeightAnimationWrapper recyclerAnimationWrapper = new ViewWeightAnimationWrapper(mUserListRecyclerView);
        ObjectAnimator recyclerAnimation = ObjectAnimator.ofFloat(recyclerAnimationWrapper,
                "weight",
                0,
                50);
        recyclerAnimation.setDuration(800);

        recyclerAnimation.start();
        mapAnimation.start();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        super.onMapReady(map);
        if (mMapLayoutState == MAP_LAYOUT_STATE_EXPANDED){
            expandMapAnimation();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_full_screen_map:{
                if(mMapLayoutState == MAP_LAYOUT_STATE_CONTRACTED){
                    mMapLayoutState = MAP_LAYOUT_STATE_EXPANDED;
                    expandMapAnimation();
                }
                else if(mMapLayoutState == MAP_LAYOUT_STATE_EXPANDED){
                    mMapLayoutState = MAP_LAYOUT_STATE_CONTRACTED;
                    contractMapAnimation();
                }
                break;
            }

            case R.id.ul_btn_reset_map:{
                addMapMarkers();
            }
        }
    }

    public interface UserListCallback{
        void navContactFragment(Contact contact, String state);
        ArrayList<Contact> getContacts();
        void navSettingsActivity();
    }
}
