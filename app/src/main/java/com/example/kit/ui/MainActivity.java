package com.example.kit.ui;

import android.app.ActivityManager;
import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import de.hdodenhof.circleimageview.CircleImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.Contact;
import com.example.kit.models.UChatroom;
import com.example.kit.models.User;
import com.example.kit.models.UserLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.example.kit.services.LocationService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.example.kit.Constants.AUTH;
import static com.example.kit.Constants.BACK_STACK_ROOT_TAG;
import static com.example.kit.Constants.CONTACT;
import static com.example.kit.Constants.CONTACT_STATE;
import static com.example.kit.Constants.ERROR_DIALOG_REQUEST;
import static com.example.kit.Constants.FRIENDS;
import static com.example.kit.Constants.INCOGNITO;
import static com.example.kit.Constants.MY_PREFERENCES;
import static com.example.kit.Constants.MY_REQUEST_PENDING;
import static com.example.kit.Constants.NOT_FRIENDS;
import static com.example.kit.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.example.kit.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;
import static com.example.kit.Constants.THEIR_REQUEST_PENDING;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        ContactsFragment.ContactsCallback,
        ChatsFragment.ChatroomsCallback,
        RequestsFragment.RequestsCallback,
        MapFragment.MapCallBack,
        RequestsDialogFragment.OnInputSelected,
        PendingFragment.PendingCallback,
        ContactFragment.ContactCallback
{

    //Tag
    private static final String TAG = "MainActivity";

    //Fragments
    private static final String LOADING_FRAG = "LOADING_FRAG";
    private static final String CHATS_FRAG = "CHATS_FRAG";
    private static final String MAP_FRAG = "MAP_FRAG";
    private static final String CONTACTS_FRAG = "CONTACTS_FRAG";
    private static final String CONTACT_FRAG = "CONTACT_FRAG";
    private FragmentTransaction ft;
    private ChatsFragment mChatsFragment;
    private ContactsRequestsPendingFragment mContactsFragment;
    private MapFragment mMapFragment;

    //Firebase
    protected FirebaseFirestore mDb;
    private FirebaseAuth mAuth;

    //Location
    private static boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationClient;
    protected UserLocation mUserLocation;
    private boolean mLocationFetched;

    //Contacts
    private ArrayList<Contact> mContacts = new ArrayList<>();
    private ArrayList<UserLocation> mContactLocations = new ArrayList<>();
    private HashMap<String, Contact> mId2Contact = new HashMap<>();
    private Set<String> mContactIds = new HashSet<>();
    private boolean mContactsFetched;

    //Requests
    private HashMap<String, Contact> mRequests = new HashMap<>();
    private boolean mRequestsFetched;

    //Pending
    private HashMap<String, Contact> mPending = new HashMap<>();
    private boolean mPendingFetched;

    //Chatrooms
    private ArrayList<UChatroom> mChatrooms = new ArrayList<>();
    private HashSet<String> mChatroomIds = new HashSet<>();
    private boolean mChatroomsFetched;

    private static Fragment curFragment;
    private static String curString;

    SharedPreferences.OnSharedPreferenceChangeListener spChanged;
    private static boolean incognito;

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        incognito = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE).getBoolean(INCOGNITO, false);
        if(incognito) {
            setTheme(R.style.AppThemeIncognito);
        }
        super.onCreate(savedInstanceState);
        if (curFragment == null){
            curFragment = ChatsFragment.getInstance();
        }
        if (curString == null){
            curString = CHATS_FRAG;
        }
        initFullLoadingView();
        mDb = FirebaseFirestore.getInstance();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initPreferenceListener();
        setupFirebaseAuth();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (checkMapServices()) {
            if (isLocationPermissionGranted()) {
//                getUserDetails();
//                fetchContacts();
//                fetchRequests();
//                fetchPending();
//                fetchChatrooms();
            }
        }
    }

    @Override
    protected void onRestart() {
        boolean inc = getSharedPreferences(MY_PREFERENCES, MODE_PRIVATE).getBoolean(INCOGNITO, false);
        if (inc != incognito){
            incognito = !incognito;
            recreate();
        }
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        SharedPreferences sp = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        sp.unregisterOnSharedPreferenceChangeListener(spChanged);
        super.onSaveInstanceState(outState);
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initFullLoadingView(){
        setContentView(R.layout.layout_loading_screen);
    }

    private void initPreferenceListener(){
        SharedPreferences sp = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        spChanged = new
                SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                                          String key) {
                       if(key.equals(INCOGNITO)){
                           recreate();
                       }
                    }
                };
        sp.registerOnSharedPreferenceChangeListener(spChanged);
    }

    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: started.");
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .build();
            mDb.setFirestoreSettings(settings);

            DocumentReference userRef = mDb.collection(getString(R.string.collection_users))
                    .document(user.getUid());

            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        User user = task.getResult().toObject(User.class);
                        System.out.println(user);
                        if(user == null || user.getUsername() == null)
                        {
                            Log.d(TAG, "FirebaseAuth: no username defined");
                            navLoginActivity(true);
                        }
                        else {
                            Log.d(TAG, "FirebaseAuth:signed_in:" + user.getUser_id());
                            setCurrentUser(user);
                            initLoadingView();
                            updateToken();
                            getLocationPermission();
                            initMessageService();
                        }
                    }
                }
            });
        }
        else {
            // User is signed out
            Log.d(TAG, "FirebaseAuth:signed_out");
            navLoginActivity(false);
        }
    }

    private void setCurrentUser(User user){
        Toast.makeText(MainActivity.this,
                "Authenticated with: " + user.getEmail(),
                Toast.LENGTH_SHORT).show();
        ((UserClient) (getApplicationContext())).setUser(user);
        Log.d(TAG, "setCurrentUser: successfully set the user client.");
    }

    private void initLoadingView () {
        setContentView(R.layout.activity_main);
        initToolbar();
        if(!isDestroyed() && !isFinishing()) {
            ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_container, LoadingFragment.newInstance(), LOADING_FRAG)
                     .commit();
        }
    }

    private void initToolbar(){
        Toolbar toolbar = findViewById(R.id.upper_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        if(incognito)
            findViewById(R.id.incognito).setVisibility(View.VISIBLE);
        else
            findViewById(R.id.incognito).setVisibility(View.GONE);
    }

    private void retrieveProfileImage(){
        RequestOptions requestOptions = new RequestOptions()
                .error(R.drawable.cartman_cop)
                .placeholder(R.drawable.cartman_cop);
        String avatar = "";
        try{
            avatar = (((UserClient)getApplicationContext()).getUser().getAvatar());
        }catch (NumberFormatException e){
            Log.e(TAG, "retrieveProfileImage: no avatar image. Setting default. " + e.getMessage() );
        }

        Glide.with(getApplicationContext())
                .setDefaultRequestOptions(requestOptions)
                .load(avatar)
                .into((CircleImageView)findViewById(R.id.image_choose_avatar));
    }

    private void initView() {
        retrieveProfileImage();
        initNavigationBar();
    }

    private void initNavigationBar () {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navi_bar);
        bottomNav.setVisibility(View.VISIBLE);
        if (curString.equals(MAP_FRAG)){
            curFragment = MapFragment.newInstance();
        }
        replaceFragment(curFragment, curString, false);
        bottomNav.setOnNavigationItemSelectedListener
                (new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        switch (item.getItemId()) {
                            case R.id.action_chats: {
                                if(!(currentFragment instanceof ChatsFragment)) {
                                    curFragment = ChatsFragment.getInstance();
                                    curString = CHATS_FRAG;
                                    setTitle(R.string.fragment_chats);
                                    replaceFragment(curFragment, curString, false);
                                }
                                return true;
                            }
                            case R.id.action_map: {
                                if(!(currentFragment instanceof MapFragment)) {
                                    curFragment = MapFragment.newInstance();
                                    curString = MAP_FRAG;
                                    setTitle(R.string.fragment_map);
                                    replaceFragment(curFragment, curString, false);
                                }
                                return true;
                            }
                            case R.id.action_contacts: {
                                if(!(currentFragment instanceof ContactsRequestsPendingFragment)) {
                                    curFragment = ContactsRequestsPendingFragment.getInstance();
                                    curString = CONTACTS_FRAG;
                                    setTitle(R.string.fragment_contacts);
                                    replaceFragment(curFragment, curString, false);
                                }
                                return true;
                            }
                        }
                        return false;
                    }
                });
    }

    private void replaceFragment (Fragment newFragment, String tag, boolean stack){
        if(!isDestroyed() && !isFinishing()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if(stack) {
                ft.replace(R.id.fragment_container, newFragment, tag).addToBackStack(BACK_STACK_ROOT_TAG).commit();
            }
            else {
                ft.replace(R.id.fragment_container, newFragment, tag).commit();
            }
        }
    }

    private void initMessageService() {
        Intent intent = new Intent("com.example.kit.services.MyFirebaseMessagingService");
        intent.setPackage("com.example.kit");
        this.startService(intent);
    }

    private void startLocationService() {
        if (!isLocationServiceRunning()) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            Log.d(TAG, "startLocationService: ASASAS");

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Log.d(TAG, "startLocationService: sasa26+");

                MainActivity.this.startForegroundService(serviceIntent);
            } else {
                Log.d(TAG, "startLocationService: :'(");
                startService(serviceIntent);
            }
        }
    }

    private void checkReady () {

        if (mContactsFetched &&
                mLocationFetched &&
                mChatroomsFetched &&
                mRequestsFetched &&
                mPendingFetched) {
            mContactLocations.add(mUserLocation);
            initView();
    }
}

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.image_choose_avatar){
            navSettingsActivity();
        }
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

    @Override
    public void initContactFragment(String contactID, String state) {
        fetchUser(contactID, state);
    }

    @Override
    public void removeContact(Contact contact) {
        mContactIds.remove(contact.getCid());
        mId2Contact.remove(contact.getCid());
        mContacts.remove(contact);
        mRequests.remove(contact.getCid());
        mPending.remove(contact.getCid());
    }

    @Override
    public void addContact(Contact contact, String type) {
        switch (type) {
            case FRIENDS: {
                mContacts.add(contact);
                mContactIds.add(contact.getCid());
                mId2Contact.put(contact.getCid(), contact);
                break;
            }

            case MY_REQUEST_PENDING: {
                mPending.put(contact.getCid(), contact);
                break;
            }

            case THEIR_REQUEST_PENDING: {
                mRequests.put(contact.getCid(), contact);
                break;
            }

        }
    }

    @Override
    public HashMap<String, UserLocation> getContactLocations(String contact_id) {
        HashMap<String, UserLocation> ret = new HashMap<>();
        String uid = FirebaseAuth.getInstance().getUid();
        String cid;
        for (UserLocation ul: mContactLocations){
            cid = ul.getUser().getUser_id();
            if (cid.equals(contact_id) || cid.equals(uid) ){
                ret.put(cid, ul);
            }
        }
        return ret;
    }

    /*
    ----------------------------- Chatrooms Callback ---------------------------------
    */

    @Override
    public HashSet<String> getChatroomIds() {
        return mChatroomIds;
    }

    @Override
    public ArrayList<UChatroom> getChatrooms() {
        return mChatrooms;
    }

    @Override
    public UserLocation getUserLocation() {
        return mUserLocation;
    }

    /*
    ----------------------------- Maps Callback ---------------------------------
    */

    @Override
    public ArrayList<UserLocation> getUserLocations () {
        return mContactLocations;
    }

    /*
    ----------------------------- Requests Callback ---------------------------------
    */

    @Override
    public HashMap<String, Contact> getRequests() {
        return mRequests;
    }


    /*
    ----------------------------- Pending Callback ---------------------------------
    */

    @Override
    public HashMap<String, Contact> getPending() {
        return mPending;
    }

    /*
    ----------------------------- Requests Dialog Fragment Callback ---------------------------------
    */

    @Override
    public void requestAccepted(String display_name, Contact contact) {
        handleRequest(contact, display_name, true);
    }

    @Override
    public void requestRemoved(Contact contact) {
        handleRequest(contact, "", false);
    }

    /*
    ----------------------------- nav ---------------------------------
    */

    private void navLoginActivity(boolean isAuth){
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(AUTH, isAuth);
        startActivity(intent);
    }

    private void navSettingsActivity(){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void navContactFragment(Contact contact, String state, boolean stack){
        ContactFragment contactFrag = ContactFragment.newInstance();
        Bundle args = new Bundle();
        args.putParcelable(CONTACT, contact);
        args.putString(CONTACT_STATE, state);
        contactFrag.setArguments(args);
        replaceFragment(contactFrag, CONTACT_FRAG, stack);
    }

    /*
    ----------------------------- DB ---------------------------------
    */

    private void updateToken(){
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String newToken = instanceIdResult.getToken();
                Log.e("newToken", newToken);
                sendRegistrationToServer(newToken);
            }
        });
    }

    private void sendRegistrationToServer(String token){
        try{
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            DocumentReference usersRef = FirebaseFirestore.getInstance()
                    .collection(getString(R.string.collection_users))
                    .document(mAuth.getUid());
            usersRef.set(data, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "TOKEN successfully written!");
                }
            });
        }catch (NullPointerException e){
            Log.e(TAG, "User instance is null, stopping notification service.");
            Log.e(TAG, "saveToken: NullPointerException: "  + e.getMessage() );
        }
    }

    protected void getUserDetails () {
        if (mUserLocation == null) {
            mUserLocation = new UserLocation();
            DocumentReference userRef = mDb.collection(getString(R.string.collection_users))
                    .document(mAuth.getUid());

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
                    .document(mAuth.getUid());

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

    private void fetchContacts () {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        mDb.setFirestoreSettings(settings);
        CollectionReference contactsCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(mAuth.getUid())
                .collection(getString(R.string.collection_contacts));
        contactsCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    if (task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Contact contact = doc.toObject(Contact.class);
                            if (!mContactIds.contains(contact.getCid())) {
                                mContactIds.add(contact.getCid());
                                mContacts.add(contact);
                                mId2Contact.put(contact.getCid(), contact);
                                getContactLocation(contact, task.getResult().size());
                            }
                        }
                        if(task.getResult().size() == 0){
                            mContactsFetched = true;
                            checkReady();
                        }
                        Log.d(TAG, "onEvent: number of contacts: " + mContacts.size());
                    }
                }
            }
        });
    }

    private void getContactLocation(final Contact user, final int numTotal){
        DocumentReference locRef = mDb.collection(getString(R.string.collection_user_locations)).document(user.getCid());
        locRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    UserLocation userLocation = task.getResult().toObject(UserLocation.class);
                    if (userLocation != null){
                        userLocation.getUser().setUsername(user.getName());
                        mContactLocations.add(userLocation);
                        if (mContactLocations.size() == numTotal){
                            mContactsFetched = true;
                            checkReady();
                        }
                    }
                }
            }
        });
    }

    private void fetchRequests () {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        mDb.setFirestoreSettings(settings);
        CollectionReference requestsCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(mAuth.getUid())
                .collection(getString(R.string.collection_requests));
        requestsCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    if(task.getResult() != null){
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Contact contact = doc.toObject(Contact.class);
                            mRequests.put(contact.getCid(), contact);
                        }
                        Log.d(TAG, "onEvent: number of contacts: " + mRequests.size());
                        mRequestsFetched = true;
                        checkReady();
                    }
                }
            }
        });
    }

    private void fetchChatrooms() {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        mDb.setFirestoreSettings(settings);
        CollectionReference chatroomsCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(mAuth.getUid())
                .collection(getString(R.string.collection_user_chatrooms));
        chatroomsCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    if(task.getResult() != null){
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            UChatroom chatroom = doc.toObject(UChatroom.class);
                            if (!mChatroomIds.contains(chatroom.getChatroom_id())) {
                                mChatroomIds.add(chatroom.getChatroom_id());
                                mChatrooms.add(chatroom);
                            }
                        }
                        Log.d(TAG, "onEvent: number of chatrooms: " + mChatrooms.size());
                        mChatroomsFetched = true;
                        checkReady();
                    }
                }
            }
        });
    }

    private void fetchPending(){
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder().build();
        mDb.setFirestoreSettings(settings);
        CollectionReference pendingCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(mAuth.getUid())
                .collection(getString(R.string.collection_pending));
        pendingCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    if(task.getResult() != null){
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Contact contact = doc.toObject(Contact.class);
                            mPending.put(contact.getCid(), contact);
                        }
                        Log.d(TAG, "onEvent: number of contacts: " + mContacts.size());
                        mPendingFetched = true;
                        checkReady();
                    }
                }
            }
        });
    }

    private void fetchUser(final String userID, final String state){
        if(state != null){
            final Contact contact = new Contact();
            DocumentReference userRef = mDb.collection(getString(R.string.collection_users))
                    .document(userID);
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: successfully found contact.");
                        User user = task.getResult().toObject(User.class);
                        contact.setCid(userID);
                        contact.setName(user.getUsername());
                        contact.setAvatar(user.getAvatar());
                        contact.setStatus(user.getStatus());
                        contact.setToken(user.getToken());
                        navContactFragment(contact, state, false);
                    }
                }
            });
        }
        if(mContactIds.contains(userID)){
            navContactFragment(mId2Contact.get(userID), FRIENDS, true);
        }

        else if (mRequests.containsKey(userID)){
            navContactFragment(mRequests.get(userID), THEIR_REQUEST_PENDING, true);
        }
//        for(Contact request : mRequests.values()){
//            if(request.getCid().equals(userID)){
//                navContactFragment(request, THEIR_REQUEST_PENDING, true);
//                return;
//            }
//        }
        else if (mPending.containsKey(userID)){
            navContactFragment(mPending.get(userID), MY_REQUEST_PENDING, true);
        }
//        for(Contact pending : mPending.values()){
//            if(pending.getCid().equals(userID)){
//                navContactFragment(pending, MY_REQUEST_PENDING, true);
//                return;
//            }
//        }

        else {
            final Contact contact = new Contact();
            DocumentReference userRef = mDb.collection(getString(R.string.collection_users))
                    .document(userID);
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "onComplete: successfully found contact.");
                        User user = task.getResult().toObject(User.class);
                        contact.setCid(userID);
                        contact.setName(user.getUsername());
                        contact.setAvatar(user.getAvatar());
                        contact.setStatus(user.getStatus());
                        contact.setToken(user.getToken());
                        navContactFragment(contact, NOT_FRIENDS, true);
                    }
                }
            });
        }
    }


    /*
    ----------------------------- Location ---------------------------------
    */

    protected boolean isLocationPermissionGranted() {
        return mLocationPermissionGranted;
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.example.kit.services.LocationService".equals(service.service.getClassName())) {
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
            mLocationPermissionGranted = true;
            startLocationService();
            getUserDetails();
            fetchContacts();
            fetchRequests();
            fetchPending();
            fetchChatrooms();
        } else if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        else{
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void getLastKnownLocation() {
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

    protected boolean checkMapServices() {
        return (isServicesOK() && isMapsEnabled());
    }

    private void buildAlertMessageNoGps() {
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

    private boolean isMapsEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private boolean isServicesOK() {
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
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                getLocationPermission();
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                getLocationPermission();
                break;
            }
        }
    }

    private void handleRequest (final Contact contact, final String display_name,
                                final boolean accepted){
        final FirebaseFirestore fs = FirebaseFirestore.getInstance();
        final String uid = mAuth.getUid();
        final DocumentReference userRef = fs.collection(Constants.COLLECTION_USERS).document(uid);
        if (accepted) {
            userRef.collection(Constants.COLLECTION_CONTACTS).document(contact.getCid()).set(new Contact(display_name,
                    contact.getUsername(), contact.getAvatar(), contact.getCid(), contact.getStatus())).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    userRef.collection(Constants.COLLECTION_REQUESTS).document(contact.getCid()).delete();
                    final DocumentReference contactRef =
                            fs.collection(Constants.COLLECTION_USERS).document(contact.getCid());
                    contactRef.collection(Constants.COLLECTION_PENDING).document(mAuth.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (!task.isSuccessful()) {
                                return;
                            }
                            final Contact ucontact = task.getResult().toObject(Contact.class);
                            contactRef.collection(Constants.COLLECTION_CONTACTS).document(ucontact.getCid()).set(ucontact).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (!task.isSuccessful()) {
                                        return;
                                    }
                                    contactRef.collection(Constants.COLLECTION_PENDING).document(ucontact.getCid()).delete();
                                    mRequests.remove(contact.getCid());
                                    replaceFragment(ContactsRequestsPendingFragment.newInstance(), CONTACTS_FRAG, false);
                                    setTitle(R.string.fragment_contacts);
                                }
                            });
                        }
                    });
                }

            });
        } else {
            userRef.collection(Constants.COLLECTION_REQUESTS).document(contact.getCid()).delete();
            fs.collection(Constants.COLLECTION_USERS).document(contact.getCid()).collection(Constants.COLLECTION_PENDING).document(uid).delete();
            mRequests.remove(contact.getCid());
            replaceFragment(ContactsRequestsPendingFragment.newInstance(), CONTACTS_FRAG, false);
            setTitle(R.string.fragment_contacts);

        }
    }
}