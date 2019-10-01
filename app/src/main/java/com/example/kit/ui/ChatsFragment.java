package com.example.kit.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.kit.R;
import com.example.kit.adapters.ChatroomRecyclerAdapter;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.models.UChatroom;
import com.example.kit.models.UserLocation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
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

    ListenerRegistration mChatsEventListener;
    ChatroomRecyclerAdapter.ChatroomRecyclerClickListener listener;

    private static int adapterPostition;

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
        adapterPostition = ((LinearLayoutManager)mChatroomRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
    }

    @Override
    public void onResume() {
        super.onResume();
        mChatroomRecyclerView.getLayoutManager().scrollToPosition(adapterPostition);
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(View v){
        v.findViewById(R.id.fab).setOnClickListener(this);
        mChatroomRecyclerView = v.findViewById(R.id.chatrooms_recycler_view);
        mChatroomRecyclerAdapter = new ChatroomRecyclerAdapter(mChatrooms, this);
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
                mChatroomRecyclerAdapter = new ChatroomRecyclerAdapter(mChatrooms, listener);
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
        if(v.getId() == R.id.fab){
            navNewMessageActivity();
        }
    }

    /*
    ----------------------------- nav ---------------------------------
    */

    private void navNewMessageActivity(){
        Intent intent = new Intent(mActivity, NewMessageActivity.class);
        intent.putExtra(CONTACTS_LIST, mContacts);
        startActivityForResult(intent, 1);
    }

    private void navChatroomActivity(UChatroom chatroom){
        Intent intent = new Intent(mActivity, ChatroomActivity.class);
        intent.putExtra(CHATROOM, chatroom);
        intent.putExtra(CONTACTS_HASH_MAP, mId2Contact);
        intent.putExtra(USER_LOCATION, mUserLocation);
        startActivityForResult(intent, 1);
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
