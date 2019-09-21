package com.example.kit.ui;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.UChatroom;
import com.example.kit.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


public class ChatroomActivity extends AppCompatActivity
    implements ChatFragment.ChatroomCallback
{

    //Tag
    private static final String TAG = "ChatroomActivity";

    //Firebase
    private FirebaseFirestore mDb;

    //vars
    public UChatroom mChatroom;

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDb = FirebaseFirestore.getInstance();
        getIncomingIntent();
        initView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chatroom_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(){
        setContentView(R.layout.activity_chatroom);
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        setChatroomName();
        FragmentManager t = getSupportFragmentManager();
        ChatMapViewPagerAdapter mAdapter = new ChatMapViewPagerAdapter(t);
        ViewPager mPager = findViewById(R.id.view_pager);
        mPager.setAdapter(mAdapter);
    }

    private void getIncomingIntent(){
        //TODO
        // this is entered upon orientation change
        if(getIntent().hasExtra(getString(R.string.intent_uchatroom))){
            mChatroom = getIntent().getParcelableExtra(getString(R.string.intent_uchatroom));
            joinChatroom();
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
            }
        });
        left();
    }

    private void joinChatroom(){
        String cid = mChatroom.getChatroom_id();
        String uid = FirebaseAuth.getInstance().getUid();
        DocumentReference joinChatroomRef = mDb
                .collection(getString(R.string.collection_chatrooms))
                .document(mChatroom.getChatroom_id())
                .collection(getString(R.string.collection_chatroom_user_list))
                .document(uid);
        User user = ((UserClient)(getApplicationContext())).getUser();
        joinChatroomRef.set(user); // Don't care about listening for completion.
    }

    /*
    ----------------------------- nav ---------------------------------
    */

    public void left() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra("left", true);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    /*
    ----------------------------- utils ---------------------------------
    */

    private void hideSoftKeyboard(){
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public UChatroom getChatroom(){
        return mChatroom;
    }

    /*
   ----------------------------- ViewPagerAdapter ---------------------------------
   */
    public static class ChatMapViewPagerAdapter extends FragmentPagerAdapter {

        private String[] mTitles = new String[] {
                "Chat",
                "Map"
        };

        private ChatMapViewPagerAdapter(FragmentManager manager) {
            super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            switch(position){
                case 0:
                    return ChatFragment.newInstance();
                case 1:
                    return UserListFragment.newInstance();
            }
            return null; // not reachable
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