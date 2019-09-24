package com.example.kit.ui;

import android.app.ActivityManager;
import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.kit.R;
import com.example.kit.models.Contact;
import com.example.kit.models.UChatroom;
import com.example.kit.models.User;
import com.example.kit.models.UserLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.example.kit.services.LocationService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import static com.example.kit.Constants.ERROR_DIALOG_REQUEST;
import static com.example.kit.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.example.kit.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

public class MainActivity extends AppCompatActivity implements
        ContactsFragment.ContactsCallback,
        ChatsFragment.ChatroomsCallback{

    //TODO
    // chat crashes on orientation change and locations are not updated properly
    //TODO
    // 'fragment not attached to context' error upon rapid switches of fragments
    //TODO
    // launch different fragments for private chats (no user list)
    // and for group chats (with user list)
    //TODO
    // actionbar functionality - additional menu options if necessary
    //TODO
    // try to move the location check to here
    //TODO
    // should we make this the launcher activity that redirects to login if necessary?
    //TODO
    // settings Activity
    //TODO
    // profile fragment - discussion on how it should look/implementation.
    // should we have different activities for viewing others profile and self profile?
    //TODO
    // what happens when someone is added to a group chat and not everyone have approved
    // everyones requests.
    //TODO
    // search fragment?
    //TODO
    // add meeting invites as a new message type
    //TODO
    // Login activity isn't perfect yet
    //TODO
    // fix ImageListFragment (as a part of ProfileFragment)
    //TODO
    // does the AddContactsActivity only contain an AlertDialog?
    // if so, why is it an Activity? should be more than a dialog, fragment probably
    //TODO
    // what is NewMessageActivity?
    // TODO
    //  merge activity_login.xml & activity_username.xml
    //TODO
    //  transitions between activities
    //TODO
    // top right button on map not working

    //TODO
    // should we hold references to all active recyclerViews here?

    //Tag
    private static final String TAG = "MainActivity";

    //Fragments
    private static final String LOADING_FRAG = "LOADING_FRAG";
    private static final String CHATS_FRAG = "CHATS_FRAG";
    private static final String CONATCTS_FRAG = "CONTACTS_FRAG";
    private static final String PROFLE_FRAG = "PROFILE_FRAG";
    private FragmentTransaction ft;

    //Firebase
    protected FirebaseFirestore mDb;

    //Location
    private static boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationClient;
    protected UserLocation mUserLocation;
    private boolean mLocationFetched;

    //Contacts
    private ArrayList<Contact> mContacts = new ArrayList<>();
    private HashMap<String, Contact> mId2Contact = new HashMap<>();
    private Set<String> mContactIds = new HashSet<>();
    private boolean mCotactsFetched;

    //Chatrooms
    private ArrayList<UChatroom> mChatrooms = new ArrayList<>();
    private Set<String> mChatroomIds = new HashSet<>();
    private boolean mChatroomsFetched;

    private ListenerRegistration mContactEventListener;
    private ListenerRegistration mChatroomEventListener;

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDb = FirebaseFirestore.getInstance();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLocationPermission();
        initLoadingView();
        initMessageService();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkMapServices()) {
            if (isLocationPermissionGranted()) {
                getUserDetails();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mContactEventListener != null){
            mContactEventListener.remove();
        }
        if(mChatroomEventListener != null){
            mChatroomEventListener.remove();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_bar, menu);
        return true;
    }


    /*
    ----------------------------- init ---------------------------------
    */

    private void initLoadingView(){
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.upper_toolbar));
        setTitle(R.string.fragment_chats);
        ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, LoadingFragment.newInstance(), LOADING_FRAG)
                .commit();
    }

    private void initView () {
        initNavigationBar();
    }

    private void initNavigationBar () {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navi_bar);
        bottomNav.setVisibility(View.VISIBLE);
        replaceFragment(ChatsFragment.newInstance(), CHATS_FRAG);
        bottomNav.setOnNavigationItemSelectedListener
                (new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
//                            case R.id.action_search:{
//                                replaceFragment(SearchFragment.newInstance(), SEARCH_FRAG);
//                                return true;
//                            }
                            case R.id.action_chats: {
                                replaceFragment(ChatsFragment.newInstance(), CHATS_FRAG);
                                setTitle(R.string.fragment_chats);
                                return true;
                            }
                            case R.id.action_contacts: {
                                replaceFragment(ContactsRequestsPendingFragment.newInstance(), CONATCTS_FRAG);
                                setTitle(R.string.fragment_contacts);
                                return true;
                            }
                            case R.id.action_profile: {
                                replaceFragment(ProfileFragment.newInstance(), PROFLE_FRAG);
                                setTitle(R.string.fragment_profile);
                                return true;
                            }
                        }
                        return false;
                    }
                });
    }

    private void replaceFragment (Fragment newFragment, String tag){
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, newFragment, tag).commit();
    }

    private void initMessageService () {
        Intent intent = new Intent("com.example.kit.services.MyFirebaseMessagingService");
        intent.setPackage("com.example.kit");
        this.startService(intent);
    }

    private void startLocationService(){
        if(!isLocationServiceRunning()){
            Intent serviceIntent = new Intent(this, LocationService.class);
            Log.d(TAG, "startLocationService: ASASAS");

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                Log.d(TAG, "startLocationService: sasa26+");

                MainActivity.this.startForegroundService(serviceIntent);
            }else{
                Log.d(TAG, "startLocationService: :'(");
                startService(serviceIntent);
            }
        }
    }

    private void checkReady(){
        if(mCotactsFetched && mLocationFetched && mChatroomsFetched){
            initView();
        }
    }

    /*
    ----------------------------- onClick ---------------------------------
    */

        @Override
        public boolean onOptionsItemSelected (MenuItem item){
            // Handle action bar item clicks here. The action bar will
            // automatically handle clicks on the Home/Up button, so long
            // as you specify a parent activity in AndroidManifest.xml.
            int id = item.getItemId();
            if (id == R.id.action_settings) {
                navSettingsActivity();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

    /*
    ----------------------------- ContactsCallback ---------------------------------
    */

    @Override
    public ArrayList<Contact> getContacts() {
        return mContacts;
    }

    @Override
    public Set<String> getContactIds() {
        return mContactIds;
    }

    @Override
    public HashMap<String, Contact> getId2Contact() {
        return mId2Contact;
    }

    /*
    ----------------------------- Chatrooms Callback ---------------------------------
    */

    @Override
    public Set<String> getChatroomIds() {
        return mChatroomIds;
    }

    @Override
    public ArrayList<UChatroom> getChatrooms() {
        return mChatrooms;
    }

    @Override
    public UserLocation getUserLocation(){
        return mUserLocation;
    }

    /*
    ----------------------------- nav ---------------------------------
    */

    private void navSettingsActivity () {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /*
    ----------------------------- DB ---------------------------------
    */

    protected void getUserDetails () {
        if (mUserLocation == null) {
            mUserLocation = new UserLocation();
            DocumentReference userRef = mDb.collection(getString(R.string.collection_users))
                    .document(FirebaseAuth.getInstance().getUid());

            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: successfully set the user client.");
                        User user = task.getResult().toObject(User.class);
                        mUserLocation.setUser(user);
                        getLastKnownLocation();
                    }
                }
            });
        } else {
            getLastKnownLocation();
        }
    }

    private void saveUserLocation () {
        if (mUserLocation != null) {
            DocumentReference locationRef = mDb
                    .collection(getString(R.string.collection_user_locations))
                    .document(FirebaseAuth.getInstance().getUid());

            locationRef.set(mUserLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "saveUserLocation: \ninserted user location into database." +
                                "\n latitude: " + mUserLocation.getGeo_point().getLatitude() +
                                "\n longitude: " + mUserLocation.getGeo_point().getLongitude());
                    }
                    mLocationFetched = true;
                    checkReady();
                }
            });
        }
    }

    private void fetchContacts(){
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        mDb.setFirestoreSettings(settings);
        CollectionReference contactsCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_contacts));
        mContactEventListener = contactsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if(queryDocumentSnapshots != null){
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        Contact contact = doc.toObject(Contact.class);
                        if(!mContactIds.contains(contact.getCid())){
                            mContactIds.add(contact.getCid());
                            mContacts.add(contact);
                            mId2Contact.put(contact.getCid(), contact);
                        }
                    }
                    Log.d(TAG, "onEvent: number of contacts: " + mContacts.size());
                }
                mCotactsFetched = true;
                checkReady();
            }
        });
    }

    private void fetchChatrooms(){
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        mDb.setFirestoreSettings(settings);
        CollectionReference chatroomsCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_user_chatrooms));
        mChatroomEventListener = chatroomsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if(queryDocumentSnapshots != null){
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        UChatroom chatroom = doc.toObject(UChatroom.class);
                        if(!mChatroomIds.contains(chatroom.getChatroom_id())){
                            mChatroomIds.add(chatroom.getChatroom_id());
                            mChatrooms.add(chatroom);
                        }
                    }
                    Log.d(TAG, "onEvent: number of chatrooms: " + mChatrooms.size());
//                    mChatroomRecyclerAdapter.notifyDataSetChanged();
                }
                mChatroomsFetched = true;
                checkReady();
            }
        });
    }

       /*
    ----------------------------- Location ---------------------------------
    */

    protected boolean isLocationPermissionGranted () {
        return mLocationPermissionGranted;
    }

    protected void setLocationPermissionGranted (boolean permissionGranted){
        mLocationPermissionGranted = permissionGranted;
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("com.example.kit.services.LocationService".equals(service.service.getClassName())) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running.");
        return false;
    }

    protected void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            setLocationPermissionGranted(true);
            startLocationService();
            getUserDetails();
            fetchContacts();
            fetchChatrooms();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void getLastKnownLocation () {
        Log.d(TAG, "getLastKnownLocation: called.");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    if (location != null) {
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mUserLocation.setGeo_point(geoPoint);
                        mUserLocation.setTimestamp(null);
                        saveUserLocation();
                    }
                }
            }
        });

    }

    protected boolean checkMapServices () {
        return (isServicesOK() && isMapsEnabled());
    }

    private void buildAlertMessageNoGps () {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private boolean isMapsEnabled () {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private boolean isServicesOK () {
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS:
                {
                    getLocationPermission();
                    break;
                }
        }
    }
}