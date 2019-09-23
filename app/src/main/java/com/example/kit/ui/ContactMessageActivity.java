package com.example.kit.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Chatroom;
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
import com.google.android.material.snackbar.Snackbar;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import static com.example.kit.Constants.CHATROOM;
import static com.example.kit.Constants.CONTACTS_HASH_MAP;
import static com.example.kit.Constants.CONTACTS_LIST;
import static com.example.kit.Constants.ERROR_DIALOG_REQUEST;
import static com.example.kit.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.example.kit.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;
import static com.example.kit.Constants.USER_LOCATION;

public class ContactMessageActivity extends AppCompatActivity implements
        View.OnClickListener,
        ContactRecyclerAdapter.ContactsRecyclerClickListener {

    private static final String TAG = "MainActivity";

    //Firebase
    protected FirebaseFirestore mDb;

    //widgets
    private ProgressBar mProgressBar;

    //vars
    private ArrayList<Contact> mContacts = new ArrayList<>();
    private Set<String> mContactIds = new HashSet<>();
    private ContactRecyclerAdapter mContactRecyclerAdapter;
    private RecyclerView mContactRecyclerView;

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDb = FirebaseFirestore.getInstance();
        getIncomingIntent();
        setContentView(R.layout.activity_contact_message);
        mProgressBar = findViewById(R.id.progressBarCM);
        mContactRecyclerView = findViewById(R.id.contact_msg_recycler_view);

        findViewById(R.id.fab_group_message).setOnClickListener(this);

        initSupportActionBar();
        initContactRecyclerView();
    }

    private void getIncomingIntent(){
        //TODO
        // this is entered upon orientation change
        Intent intent = getIntent();
        if(intent.hasExtra(CONTACTS_LIST)){
            mContacts = getIntent().getParcelableArrayListExtra(CONTACTS_LIST);
        }
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initContactRecyclerView() {
        mContactRecyclerAdapter = new ContactRecyclerAdapter(mContacts, this);
        mContactRecyclerView.setAdapter(mContactRecyclerAdapter);
        mContactRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        initSearchView();
    }

    private void initSearchView(){
        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String queryString) {
                mContactRecyclerAdapter.getFilter().filter(queryString);
                return false;
            }
            @Override
            public boolean onQueryTextChange(String queryString) {
                mContactRecyclerAdapter.getFilter().filter(queryString);
                return false;
            }
        });
    }

    private void initSupportActionBar() {
        setTitle("Contacts");
    }

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_group_message:
//                startGroupChat();
        }
    }

    private void buildNewChatroom(String cid1, String cid2, final String display_name, final boolean isGroup){
//        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
//                .setTimestampsInSnapshotsEnabled(true)
//                .build();
//        mDb.setFirestoreSettings(settings);
        final Chatroom chatroom;
        final String first, second;
        final String chatroom_id;
        if (stringCompare(cid1, cid2) > 0) {
            first = cid2;
            second = cid1;
        } else {
            first = cid1;
            second = cid2;
        }
        chatroom_id = first + second;

        chatroom = new Chatroom(first, second, chatroom_id);

        DocumentReference newChatroomRef = mDb
                .collection(getString(R.string.collection_chatrooms))
                .document(chatroom_id);

        newChatroomRef.set(chatroom).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                hideDialog();

                if(task.isSuccessful()){
                    addUserToChatroom(chatroom_id, first, second, isGroup);
                    addUserToChatroom(chatroom_id, second, first, isGroup);
                    UChatroom uchat = new UChatroom(display_name, first + second, isGroup, 2);
                    navChatActivity(uchat);
                }else{
                    View parentLayout = findViewById(android.R.id.content);
                    Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void navChatActivity(UChatroom uchat){
        Intent intent = new Intent(ContactMessageActivity.this, ChatroomActivity.class);
        intent.putExtra(getString(R.string.intent_uchatroom), uchat);
        startActivityForResult(intent, 0);
    }

    private void navNewGroupActivity(String cid, String groupname){
        Intent intent = new Intent(ContactMessageActivity.this, AddContactsActivity.class);
        intent.putExtra(getString(R.string.intent_contact), cid);
        intent.putExtra("gn", groupname);
        startActivityForResult(intent, 0);
    }

    private void newGroupChat(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter a group name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!input.getText().toString().equals("")){
                }
                else {
                    Toast.makeText(ContactMessageActivity.this, "Enter a group name", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public void onContactSelected(int position) {
        final int pos = position;
        String uid1 = FirebaseAuth.getInstance().getUid();
        String uid2 = mContacts.get(position).getCid();
        final String first, second;
        if (stringCompare(uid1, uid2) > 0){
            first = uid2;
            second = uid1;
        } else {
            first = uid1;
            second = uid2;
        }
        CollectionReference chatroomsRef =
                mDb.collection(getString(R.string.collection_users)).document(uid1).collection(getString(R.string.collection_contacts));
        Query query = chatroomsRef.whereEqualTo("chatroom_id", first + second);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot documentSnapshot : task.getResult()) {
                        String cid = documentSnapshot.getString("chatroom_id");

                        if (cid.equals(first + second)) {
                            UChatroom uchat = documentSnapshot.toObject(UChatroom.class);
                            Log.d(TAG, "User Exists");
                            User user = documentSnapshot.toObject(User.class);
                            Log.d(TAG, "onComplete: Barak contact id" + user.getUser_id());
                            navChatActivity(uchat);
                        }
                    }
                }
                if (task.getResult().isEmpty()){
                    buildNewChatroom(first, second, mContacts.get(pos).getName(), false);
                }
            }
        });
    }

    private void signOut(){
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
//            case R.id.action_sign_out:{
//                signOut();
//                return true;
//            }
            case R.id.action_profile:{
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            default:{
                return super.onOptionsItemSelected(item);
            }
        }

    }

    private void showDialog(){
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        mProgressBar.setVisibility(View.GONE);
    }

    // This method compares two strings
    // lexicographically without using
    // library functions
    public static int stringCompare(String str1, String str2)
    {
        int l1 = str1.length();
        int l2 = str2.length();
        int lmin = Math.min(l1, l2);

        for (int i = 0; i < lmin; i++) {
            int str1_ch = (int)str1.charAt(i);
            int str2_ch = (int)str2.charAt(i);

            if (str1_ch != str2_ch) {
                return str1_ch - str2_ch;
            }
        }
        // Edge case for strings like
        // String 1="Geeks" and String 2="Geeksforgeeks"
        if (l1 != l2) {
            return l1 - l2;
        }
        // If none of the above conditions is true,
        // it implies both the strings are equal
        else {
            return 0;
        }
    }

    private void addUserToChatroom(final String cid, final String uid, final String contact_id, final boolean isGroup){
        mDb.collection(getString(R.string.collection_users)).document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful()){return;}
                User user = task.getResult().toObject(User.class);
                DocumentReference joinChatroomRef = mDb
                        .collection(getString(R.string.collection_chatrooms))
                        .document(cid)
                        .collection(getString(R.string.collection_chatroom_user_list))
                        .document(uid);
                joinChatroomRef.set(user); // Don't care about listening for completion.

                DocumentReference contactRef =
                        mDb.collection(getString(R.string.collection_users)).document(uid).collection(getString(R.string.collection_contacts)).document(contact_id);

                contactRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (!task.isSuccessful()){
                            return;
                        }
                        Contact contact = task.getResult().toObject(Contact.class);
                        String display_name = contact.getName();
                        UChatroom uchat = new UChatroom(display_name, cid, isGroup);
                        DocumentReference userChatRef = mDb
                                .collection(getString(R.string.collection_users))
                                .document(uid)
                                .collection(getString(R.string.collection_user_chatrooms))
                                .document(cid);

                        userChatRef.set(uchat);
                    }
                });

            }
        });

            }


}