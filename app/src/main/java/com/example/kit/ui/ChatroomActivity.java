package com.example.kit.ui;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import com.example.kit.R;
import com.example.kit.models.Contact;
import com.example.kit.models.UChatroom;
import com.example.kit.models.User;
import com.example.kit.models.UserLocation;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.kit.Constants.BACK_STACK_ROOT_TAG;
import static com.example.kit.Constants.CHATROOM;
import static com.example.kit.Constants.CONTACT;
import static com.example.kit.Constants.CONTACTS_HASH_MAP;
import static com.example.kit.Constants.CONTACTS_LIST;
import static com.example.kit.Constants.CONTACT_STATE;
import static com.example.kit.Constants.INCOGNITO;
import static com.example.kit.Constants.MY_PREFERENCES;
import static com.example.kit.Constants.USER_LOCATION;


public class ChatroomActivity extends AppCompatActivity implements
        ChatFragment.ChatroomCallback,
        MapFragment.MapCallBack,
        View.OnClickListener,
        UserListFragment.UserListCallback
{

    //Tag
    private static final String TAG = "ChatroomActivity";

    //Firebase
    private FirebaseFirestore mDb;

    //vars
    private ListenerRegistration mUserListEventListener;
    private UserLocation userPos;
    public UChatroom mChatroom;
    private HashMap<String, Contact> mContacts = new HashMap<>();
    private ArrayList<User> mUserList = new ArrayList<>();
    private ArrayList<Contact> contacts = new ArrayList<>();
    private ArrayList<UserLocation> mUserLocations = new ArrayList<>();
    private ArrayList<String> mUserTokens = new ArrayList<>();

    private static final String CONTACT_FRAG = "CONTACT_FRAG";

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE).getBoolean(INCOGNITO, false))
            setTheme(R.style.AppThemeIncognito);
        super.onCreate(savedInstanceState);
        mDb = FirebaseFirestore.getInstance();
        getIncomingIntent();
        getChatroomUsers();
        initLoadingView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mUserListEventListener != null){
            mUserListEventListener.remove();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chatroom_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initLoadingView(){
        setContentView(R.layout.activity_chatroom);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        setChatroomName();
    }

    private void initView(){
        ChatMapViewPagerAdapter mAdapter = new ChatMapViewPagerAdapter(getSupportFragmentManager());
        ViewPager mPager = findViewById(R.id.chatroom_view_pager);
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        mPager.setVisibility(View.VISIBLE);
        mPager.setAdapter(mAdapter);
    }

    private void getIncomingIntent(){
        Intent intent = getIntent();
        if(intent.hasExtra(CONTACTS_HASH_MAP)){
            mContacts = (HashMap<String, Contact>)getIntent().getSerializableExtra(CONTACTS_HASH_MAP);
        }
        if(intent.hasExtra(USER_LOCATION)){
            userPos = getIntent().getParcelableExtra(USER_LOCATION);
        }
        if(intent.hasExtra(CHATROOM)){
            mChatroom = getIntent().getParcelableExtra(CHATROOM);
            mDb.collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).collection(getString(R.string.collection_user_chatrooms)).document(mChatroom.getChatroom_id()).update("read_last_message", true);

        }
        if (intent.hasExtra(CONTACTS_LIST)){
            contacts = getIntent().getParcelableArrayListExtra(CONTACTS_LIST);
        }
    }

    private void setChatroomName(){
        getSupportActionBar().setTitle(mChatroom.getDisplay_name());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.view_pager:{
                hideSoftKeyboard();
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:{
                UserListFragment fragment =
                        (UserListFragment) getSupportFragmentManager().findFragmentByTag(getString(R.string.fragment_user_list));
                if(fragment != null){
                    if(fragment.isVisible()){
                        getSupportFragmentManager().popBackStack();
                        return true;
                    }
                }
                finish();
                return true;
            }
            case R.id.action_chatroom_leave:{
                leaveChatroom();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void leaveChatroom(){
        CollectionReference chatrooms = mDb.collection("Chatrooms");
        final DocumentReference chatroom = chatrooms.document(mChatroom.getChatroom_id());
        CollectionReference user_list = chatroom.collection("User List");
        DocumentReference joinChatroomRef = user_list.document(FirebaseAuth.getInstance().getUid());
        joinChatroomRef.delete();
        final int[] count = new int[1];
        user_list.orderBy("email").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }

                if(queryDocumentSnapshots != null){
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        count[0] += 1;
                    }
                }
                if (count[0] == 0){
                    System.out.println("check34");
                    chatroom.delete();
                }
                left();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.chatroom_container).setVisibility(View.INVISIBLE);
    }

    //    private void joinChatroom(){
//        String uid = FirebaseAuth.getInstance().getUid();
//        DocumentReference joinChatroomRef = mDb
//                .collection(getString(R.string.collection_chatrooms))
//                .document(mChatroom.getChatroom_id())
//                .collection(getString(R.string.collection_chatroom_user_list))
//                .document(uid);
//        User user = ((UserClient)(getApplicationContext())).getUser();
//        joinChatroomRef.set(user); // Don't care about listening for completion.
//    }

    /*
    ----------------------------- nav ---------------------------------
    */

    public void left() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("left", true);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void getChatroomUsers(){
        CollectionReference usersRef = mDb
                .collection(getString(R.string.collection_chatrooms))
                .document(mChatroom.getChatroom_id())
                .collection(getString(R.string.collection_chatroom_user_list));

        mUserListEventListener = usersRef
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "onEvent: Listen failed.", e);
                            return;
                        }

                        if(queryDocumentSnapshots != null){

                            // Clear the list and add all the users again
                            mUserList.clear();
                            mUserList = new ArrayList<>();

                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                User user = doc.toObject(User.class);
                                if (mContacts.containsKey(user.getUser_id())){
                                    user.setUsername(mContacts.get(user.getUser_id()).getName());
                                }
                                mUserList.add(user);
                                mUserTokens.add(user.getToken());
                                System.out.println(user.getUser_id());
                                getUserLocation(user);
                            }

                            Log.d(TAG, "onEvent: user list size: " + mUserList.size());
                        }
                    }
                });
    }

    private void getUserLocation(final User user){
        DocumentReference locRef = mDb.collection(getString(R.string.collection_user_locations)).document(user.getUser_id());
        locRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    UserLocation userLocation = task.getResult().toObject(UserLocation.class);
                    if (userLocation != null){
                        if(user.getUser_id().equals(FirebaseAuth.getInstance().getUid())){
                            userPos = userLocation;
                        }
                        userLocation.getUser().setUsername(user.getUsername());
                        mUserLocations.add(userLocation);
                        if (mUserLocations.size() == mChatroom.getNumUsers()){
                            initView();
                        }
                    }
                }
            }
        });
    }

    /*
    ----------------------------- utils ---------------------------------
    */

    @Override
    public void navSettingsActivity(){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }


    @Override
    public void navContactFragment(Contact contact, String state){
        ContactFragment contactFrag = ContactFragment.newInstance();
        Bundle args = new Bundle();
        args.putParcelable(CONTACT, contact);
        args.putString(CONTACT_STATE, state);
        contactFrag.setArguments(args);
        replaceFragment(contactFrag, CONTACT_FRAG, true);
    }

    private void replaceFragment (Fragment newFragment, String tag, boolean stack){
        if(!isDestroyed() && !isFinishing()) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            if(stack) {
                findViewById(R.id.chatroom_container).setVisibility(View.VISIBLE);
                ft.replace(R.id.chatroom_container, newFragment, tag).addToBackStack(BACK_STACK_ROOT_TAG).commit();
            }
        }
    }

    @Override
    public ArrayList<Contact> getContacts() {
        return contacts;
    }

    private void hideSoftKeyboard(){
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public UChatroom getChatroom(){
        return mChatroom;
    }

    @Override
    public ArrayList<User> getUserList() {
        return mUserList;
    }

    @Override
    public ArrayList<UserLocation> getUserLocations() {
        return mUserLocations;
    }

    @Override
    public ArrayList<String> getUserTokens() {
        return mUserTokens;
    }

    @Override
    public UserLocation getUserLocation() {
        return userPos;
    }

    @Override
    public Contact getContact(String id) {
        return mContacts.get(id);
    }

    /*
            ----------------------------- ViewPagerAdapter ---------------------------------
            */
    public static class ChatMapViewPagerAdapter extends FragmentPagerAdapter {

        private String[] mTitles = new String[] {
                "Chat",
                "Map"
        };

        private List<Fragment> fragments;

        private ChatMapViewPagerAdapter(FragmentManager manager) {
            super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.fragments = new ArrayList<>();
            this.fragments.add(0, ChatFragment.newInstance());
            this.fragments.add(1, UserListFragment.newInstance());
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }
    }
}