package com.example.kit.ui;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.kit.R;
import com.example.kit.adapters.ChatroomRecyclerAdapter;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Chatroom;
import com.example.kit.models.Contact;
import com.example.kit.models.UChatroom;
import com.example.kit.models.User;
import com.example.kit.models.UserLocation;
import com.example.kit.util.UsernameValidator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.MarkerManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import static android.widget.LinearLayout.HORIZONTAL;
import static com.example.kit.Constants.CHATROOM;
import static com.example.kit.Constants.CONTACTS_HASH_MAP;
import static com.example.kit.Constants.CONTACTS_LIST;
import static com.example.kit.Constants.USER_LOCATION;


public class ChatsFragment extends DBGeoFragment implements
        ChatroomRecyclerAdapter.ChatroomRecyclerClickListener,
        ContactRecyclerAdapter.ContactsRecyclerClickListener,
        View.OnClickListener
{

    //TODO
    // create images for when there are no chats
    //TODO
    // swipe functionality to recyclerview items? archive/star etc.
    //TODO
    // images for chats (maybe in chatroom activity, anyways chatroom should have an image)

    //Tag
    private static final String TAG = "ChatsFragment";

    //RecyclerView
    private ChatroomRecyclerAdapter mChatroomRecyclerAdapter;
    private RecyclerView mChatroomRecyclerView;

    //Chat rooms
    private ArrayList<UChatroom> mChatrooms = new ArrayList<>();
    private HashSet<String> mChatroomIds = new HashSet<>();

    //Contacts
    private ArrayList<Contact> mContacts = new ArrayList<>();
    private HashMap<String, Contact> mId2Contact = new HashMap<>();

    //Location
    private UserLocation mUserLocation;

    //New Chat Dialog
    private ContactRecyclerAdapter mContactRecyclerAdapter;
    private RecyclerView mContactRecyclerView;
    private Button fab;
    private AlertDialog alertDialog;

    ListenerRegistration mChatsEventListener;
    ChatroomRecyclerAdapter.ChatroomRecyclerClickListener listener;

//    private int adapterPostition;

    private static ChatsFragment instance;

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public ChatsFragment() {
        super();
    }

    public static ChatsFragment getInstance(){
        if (instance == null){
            instance = new ChatsFragment();
        }
        return instance;
    }

    public static ChatsFragment newInstance() {
        return new ChatsFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        ContactsFragment.ContactsCallback getContactsData = (ContactsFragment.ContactsCallback)context;
        ChatroomsCallback getChatroomsData = (ChatroomsCallback)context;
        mId2Contact = getContactsData.getId2Contact();
        mContacts = getContactsData.getContacts();
        mChatrooms = getChatroomsData.getChatrooms();
        Collections.sort(mChatrooms, new Comparator<UChatroom>() {
            @Override
            public int compare(UChatroom u1, UChatroom u2) {

                if (u1.getTime_last_sent().getTime() < u2.getTime_last_sent().getTime())
                    return 1;
                if (u1.getTime_last_sent().getTime() > u2.getTime_last_sent().getTime())
                    return -1;
                return 0;
            }
        });
        Collections.reverse(mChatrooms);
        mChatroomIds = getChatroomsData.getChatroomIds();
        mUserLocation = getChatroomsData.getUserLocation();
        listener = this;
        initListener();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mChatsEventListener.remove();
    }

    @Override
    public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null){
            mChatroomIds = (HashSet<String>)savedInstanceState.getSerializable("chatids");
            mChatrooms = (ArrayList<UChatroom>)savedInstanceState.getSerializable("mChatrooms");
            mId2Contact = (HashMap<String, Contact>)savedInstanceState.getSerializable("locs");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chats, container, false);
        initView(view);
        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("mChatrooms",mChatrooms);
        outState.putSerializable("chatids", mChatroomIds);
        outState.putSerializable("users", mId2Contact);


    }

    @Override
    public void onPause() {
        super.onPause();
//        int i = ((LinearLayoutManager)mChatroomRecyclerView.getLayoutManager()).findLastCompletelyVisibleItemPosition();
//        adapterPostition =
    }

    @Override
    public void onResume() {
        super.onResume();
//        mChatroomRecyclerView.getLayoutManager().scrollToPosition(adapterPostition);
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(View v){
        v.findViewById(R.id.fab).setOnClickListener(this);
        mChatroomRecyclerView = v.findViewById(R.id.chatrooms_recycler_view);
        mChatroomRecyclerAdapter = new ChatroomRecyclerAdapter(mChatrooms, this, mActivity);
        mChatroomRecyclerView.setAdapter(mChatroomRecyclerAdapter);
        mChatroomRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
        mChatroomRecyclerView.addItemDecoration(itemDecor);
        initSearchView(v);
    }

    private void initSearchView(View v){
        SearchView searchView = v.findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String queryString) {
                mChatroomRecyclerAdapter.getFilter().filter(queryString);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String queryString) {
                mChatroomRecyclerAdapter.getFilter().filter(queryString);
                return false;
            }
        });
    }

    private void initListener(){
        CollectionReference chatsCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_user_chatrooms));
        mChatsEventListener = chatsCollection.addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        final UChatroom uChatroom = doc.toObject(UChatroom.class);
                        if (!mChatroomIds.contains(uChatroom.getChatroom_id())) {
                            mChatrooms.add(uChatroom);
                            mChatroomIds.add(uChatroom.getChatroom_id());
                            notifyRecyclerAdapter();
                        }
                        else {
                            for (int i = 0; i < mChatrooms.size() ; i++) {
                                UChatroom uChat = mChatrooms.get(i);
                                if (uChat.getChatroom_id().equals(uChatroom.getChatroom_id())){
                                    mChatrooms.remove(i);
                                    mChatrooms.add(uChatroom);
                                    notifyRecyclerAdapter();
                                }
                            }

                        }

                        Log.d(TAG, "onEvent: number of contacts: " + mContacts.size());

                    }
                }
            }
        });
    }

    private void notifyRecyclerAdapter(){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                Collections.sort(mChatrooms, new Comparator<UChatroom>() {
                    @Override
                    public int compare(UChatroom u1, UChatroom u2) {
                        if (u1.getTime_last_sent().getTime() < u2.getTime_last_sent().getTime())
                            return 1;
                        if (u1.getTime_last_sent().getTime() > u2.getTime_last_sent().getTime())
                            return -1;
                        return 0;
                    }
                });
                mChatroomRecyclerAdapter = new ChatroomRecyclerAdapter(mChatrooms, listener, mActivity);
                mChatroomRecyclerView.setAdapter(mChatroomRecyclerAdapter);
                mChatroomRecyclerAdapter.notifyDataSetChanged();
                mChatroomRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
                DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
                mChatroomRecyclerView.addItemDecoration(itemDecor);
                mChatroomRecyclerAdapter.notifyDataSetChanged();
            }
        });
    }

     /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onChatroomSelected(int position) {
        navChatroomActivity(mChatrooms.get(position));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                newChatDialog();
                break;
            case R.id.group_message_btn:
                alertDialog.dismiss();
                startGroupChat(mContactRecyclerAdapter.getCheckedContacts());
                break;
        }
    }

    /*
    ----------------------------- nav ---------------------------------
    */

    private void newChatDialog(){
        final View dialogView = View.inflate(mActivity, R.layout.dialog_new_message, null);
        alertDialog = new AlertDialog.Builder(mActivity).create();
        mContactRecyclerView = dialogView.findViewById(R.id.contact_msg_recycler_view);
        fab = dialogView.findViewById(R.id.group_message_btn);
        fab.setOnClickListener(this);
        mContactRecyclerAdapter = new ContactRecyclerAdapter(mContacts,
                this, R.layout.layout_contact_list_item, mActivity);
        mContactRecyclerView.setAdapter(mContactRecyclerAdapter);
        mContactRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        fab.setEnabled(false);
        SearchView searchView = dialogView.findViewById(R.id.search_view);
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
        alertDialog.setView(dialogView);
        alertDialog.show();
    }

    private void navChatroomActivity(UChatroom chatroom){
        Intent intent = new Intent(mActivity, ChatroomActivity.class);
        intent.putExtra(CHATROOM, chatroom);
        intent.putExtra(CONTACTS_HASH_MAP, mId2Contact);
        intent.putExtra(USER_LOCATION, mUserLocation);
        intent.putExtra(CONTACTS_LIST, mContacts);
        startActivityForResult(intent, 1);
    }

    /*
    ----------------------------- Contacts Recycler ---------------------------------
    */

    @Override
    public void onContactSelected(int position) {
        alertDialog.dismiss();
        startPrivateChat(position);
    }

    @Override
    public void onContactLongClick(int pos) {
        if(mContactRecyclerAdapter.isWithCheckBoxes()){
            fab.setEnabled(true);
            fab.setVisibility(View.VISIBLE);
        }
        else{
            fab.setEnabled(false);
            fab.setVisibility(View.INVISIBLE);
        }
        for (int childCount = mContactRecyclerView.getChildCount(), i = 0; i < childCount; ++i) {
            final ContactRecyclerAdapter.ViewHolder holder =
                    (ContactRecyclerAdapter.ViewHolder)mContactRecyclerView
                            .getChildViewHolder(mContactRecyclerView.getChildAt(i));
            if(mContactRecyclerAdapter.isWithCheckBoxes()){
                holder.getCheckBox().setVisibility(View.VISIBLE);
            }
            else{
                holder.getCheckBox().setVisibility(View.GONE);
                holder.getCheckBox().setChecked(false);
            }
        }
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

        /*
    ----------------------------- DB ---------------------------------
    */

    public void startPrivateChat(int position) {
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
                            navChatroomActivity(uchat);
                        }
                    }
                }
                if (task.getResult().isEmpty()){
                    buildPrivateChatroom(first, second, mContacts.get(pos).getName());
                }
            }
        });
    }

    private void buildPrivateChatroom(String cid1, String cid2, final String display_name){
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

                if(task.isSuccessful()){
                    addUserToPrivateChatroom(chatroom_id, first, second);
                    addUserToPrivateChatroom(chatroom_id, second, first);
                    UChatroom uchat = new UChatroom(display_name, first + second, false, 2);
                    navChatroomActivity(uchat);
                }else{
//                    View parentLayout = findViewById(android.R.id.content);
//                    Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void buildGroupChatroom(final String display_name, final ArrayList<Contact> members){
        final Chatroom chatroom;
        final String chatroom_id;

        chatroom = new Chatroom();
        chatroom.setGroup_name(display_name);
        chatroom.setGroup(true);
        chatroom.setNumUsers(members.size() + 1);

        DocumentReference newChatroomRef = mDb
                .collection(getString(R.string.collection_chatrooms))
                .document();

        chatroom_id = newChatroomRef.getId();

        newChatroomRef.set(chatroom).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    addUserToGroupChatroom(chatroom_id, FirebaseAuth.getInstance().getUid(), display_name);
                    for(Contact member : members){
                        addUserToGroupChatroom(chatroom_id, member.getCid(), display_name);
                    }
                    UChatroom uchat = new UChatroom(display_name, chatroom_id, true, chatroom.getNumUsers());
                    navChatroomActivity(uchat);
                }else{
//                    View parentLayout = findViewById(android.R.id.content);
//                    Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void startGroupChat(final ArrayList<Contact> chatContacts){
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mActivity);
        builder.setTitle("Enter a group name");

        final EditText input = new EditText(mActivity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String groupName = input.getText().toString();
                if(!groupName.equals("")){
                    UsernameValidator validator = new UsernameValidator();
                    if(validator.validate(groupName))
                    {
                        buildGroupChatroom(groupName, chatContacts);
                    }
                    else{
                        Toast.makeText(mActivity,
                                "Enter a valid group name", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(mActivity,
                            "Name cannot be empty", Toast.LENGTH_SHORT).show();
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

    private void addUserToPrivateChatroom(final String cid, final String uid, final String contact_id){
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
                        UChatroom uchat = new UChatroom(display_name, cid, false);
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

    private void addUserToGroupChatroom(final String cid, final String uid, final String display_name){
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

                UChatroom uchat = new UChatroom(display_name, cid, true);
                DocumentReference userChatRef = mDb
                        .collection(getString(R.string.collection_users))
                        .document(uid)
                        .collection(getString(R.string.collection_user_chatrooms))
                        .document(cid);
                userChatRef.set(uchat);
            }
        });
    }

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

    /*
    ----------------------------- Chatrooms Callback ---------------------------------
    */

    public interface ChatroomsCallback{
        ArrayList<UChatroom> getChatrooms();
        HashSet<String> getChatroomIds();
        UserLocation getUserLocation();
    }
}
