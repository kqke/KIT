package com.example.kit.ui;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.kit.R;
import com.example.kit.adapters.ChatroomRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.models.UChatroom;
import com.example.kit.models.UserLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.example.kit.Constants.CHATROOM;
import static com.example.kit.Constants.CONTACTS_HASH_MAP;
import static com.example.kit.Constants.CONTACTS_LIST;
import static com.example.kit.Constants.USER_LOCATION;


public class ChatsFragment extends DBGeoFragment implements
        ChatroomRecyclerAdapter.ChatroomRecyclerClickListener,
        View.OnClickListener
{

    //Tag
    private static final String TAG = "ChatsFragment";

    //RecyclerView
    private ChatroomRecyclerAdapter mChatroomRecyclerAdapter;
    private RecyclerView mChatroomRecyclerView;

    //Chat rooms
    private ArrayList<UChatroom> mChatrooms = new ArrayList<>();
    private Set<String> mChatroomIds = new HashSet<>();

    //Contacts
    private ArrayList<Contact> mContacts = new ArrayList<>();
    private HashMap<String, Contact> mId2Contact = new HashMap<>();

    //Location
    protected UserLocation mUserLocation;

    //Callback
    ContactsFragment.ContactsCallback getContactsData;
    ChatroomsCallback getChatroomsData;


    //TODO
    // create images for when there are no chats
    //TODO
    // swipe functionality to recyclerview items? archive/star etc.
    //TODO
    // images for chats (maybe in chatroom activity, anyways chatroom should have an image)

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public ChatsFragment() {
        super();
    }

    public static ChatsFragment newInstance() {
        return new ChatsFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        getContactsData =  (ContactsFragment.ContactsCallback)context;
        getChatroomsData = (ChatroomsCallback)context;
        mId2Contact = getContactsData.getId2Contact();
        mContacts = getContactsData.getContacts();
        mChatrooms = getChatroomsData.getChatrooms();
        mChatroomIds = getChatroomsData.getChatroomIds();
        mUserLocation = getChatroomsData.getUserLocation();

    }

    @Override
    public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
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

    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(View v){
        v.findViewById(R.id.fab).setOnClickListener(this);
        mChatroomRecyclerView = v.findViewById(R.id.chatrooms_recycler_view);
        mChatroomRecyclerAdapter = new ChatroomRecyclerAdapter(mChatrooms, this);
        mChatroomRecyclerView.setAdapter(mChatroomRecyclerAdapter);
        mChatroomRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
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
            //TODO
            // build a new chat/chat room (Fragment?)
            navContactMessageActivity();
        }
    }

    /*
    ----------------------------- nav ---------------------------------
    */

    private void navContactMessageActivity(){
        Intent intent = new Intent(getActivity(), NewMessageActivity.class);
        intent.putExtra(CONTACTS_LIST, mContacts);
        startActivityForResult(intent, 1);
    }

    private void navChatroomActivity(UChatroom chatroom){
        Intent intent = new Intent(getActivity(), ChatroomActivity.class);
        intent.putExtra(CHATROOM, chatroom);
        intent.putExtra(CONTACTS_HASH_MAP, mId2Contact);
        intent.putExtra(USER_LOCATION, mUserLocation);
        startActivityForResult(intent, 1);
    }



    public interface ChatroomsCallback{
        ArrayList<UChatroom> getChatrooms();
        Set<String> getChatroomIds();
        UserLocation getUserLocation();
    }
}
