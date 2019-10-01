package com.example.kit.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.util.RequestHandler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

import static android.widget.LinearLayout.HORIZONTAL;

public class RequestsFragment extends DBGeoFragment implements
        ContactRecyclerAdapter.ContactsRecyclerClickListener,
        RequestsDialogFragment.OnInputSelected {

    //Tag
    private static final String TAG = "RequestsFragment";

    //RecyclerView
    private ContactRecyclerAdapter mRequestsRecyclerAdapter;
    private RecyclerView mRequestsRecyclerView;
    private ArrayList<Contact> mRecyclerList;

    //Requests
    private HashMap<String, Contact> mRequests;

    //Contacts Callback
    ContactsFragment.ContactsCallback initContactFragment;

    //Listener
    private ListenerRegistration mRequestEventListener;

    //vars
    private RequestsFragment mRequestFragment;

//    private static RequestHandler rHandler = new RequestHandler();

    /*
    ----------------------------- Lifecycle ---------------------------------
    */
    public RequestsFragment() {
        super();
    }

    public static RequestsFragment newInstance() {
        RequestsFragment fragment = new RequestsFragment();
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mRequestFragment = this;
        RequestsCallback getData = (RequestsCallback)context;
        mRequests = getData.getRequests();
        mRecyclerList = new ArrayList<>(mRequests.values());
        initContactFragment = (ContactsFragment.ContactsCallback)context;
        initListener();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_requests, container, false);
        initView(rootView);
        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mRequestEventListener.remove();
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(View v){
        mRequestsRecyclerView = v.findViewById(R.id.requests_recycler_view);
        mRequestsRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
                this, R.layout.layout_requests_list_item, getContext());
        mRequestsRecyclerView.setAdapter(mRequestsRecyclerAdapter);
        mRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
        mRequestsRecyclerView.addItemDecoration(itemDecor);
        mRequestsRecyclerAdapter.notifyDataSetChanged();
        initSearchView(v);
    }

    private void initSearchView(View v){
        SearchView searchView = v.findViewById(R.id.requests_search_view);
        ImageView icon = searchView.findViewById(R.id.search_button);
        icon.setColorFilter(Color.BLACK);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String queryString) {
                mRequestsRecyclerAdapter.getFilter().filter(queryString);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String queryString) {
                mRequestsRecyclerAdapter.getFilter().filter(queryString);
                return false;
            }
        });
    }

    private void initListener(){
        CollectionReference requestsCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_requests));
        mRequestEventListener = requestsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    Contact contact;
                    //Instead of simply using the entire query snapshot
                    //See the actual changes to query results between query snapshots (added, removed, and modified)
                    for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {
                        switch (doc.getType()) {

                            case ADDED:

                                //Call the model to populate it with document
                                contact = doc.getDocument().toObject(Contact.class);
                                mRequests.put(contact.getCid(), contact);
                                mRecyclerList = new ArrayList<>(mRequests.values());
                                notifyRecyclerView();

                                Log.d(TAG,"THIS SHOULD BE CALLED");

                                   /* //Just call this method once
                                    if (noContent.isShown()){
                                        //This will be called only if user added some new post
                                        announcementList.add(annonPost);
                                        announcementRecyclerAdapter.notifyDataSetChanged();
                                        noContent.setVisibility(View.GONE);
                                        label.setVisibility(View.VISIBLE);
                                    }*/

                                break;

                            case MODIFIED:
                                contact = doc.getDocument().toObject(Contact.class);
                                mRequests.put(contact.getCid(), contact);
                                mRecyclerList = new ArrayList<>(mRequests.values());
                                notifyRecyclerView();
                                break;

                            case REMOVED:
                                contact = doc.getDocument().toObject(Contact.class);
                                mRequests.remove(contact.getCid());
                                mRecyclerList = new ArrayList<>(mRequests.values());
                                notifyRecyclerView();
                        }
                    }

                }

            }

//                if (queryDocumentSnapshots != null) {
//                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
//                        Contact contact = doc.toObject(Contact.class);
//                        mRequests.put(contact.getCid(), contact);
//                        mRecyclerList = new ArrayList<>(mRequests.values());
//                        new Handler(Looper.getMainLooper()).post(new Runnable() {
//                            public void run() {
//                                mRequestsRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
//                                       mRequestFragment, R.layout.layout_requests_list_item, getContext());
//                                mRequestsRecyclerView.setAdapter(mRequestsRecyclerAdapter);
//                                mRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
//                                DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
//                                mRequestsRecyclerView.addItemDecoration(itemDecor);
//                                mRequestsRecyclerAdapter.notifyDataSetChanged();
//                            }
//                        });
//
//                    }
//                    Log.d(TAG, "onEvent: number of contacts: " + mRequests.size());
//                    mRequestsRecyclerAdapter.notifyDataSetChanged();
//                }
//            }
        });
    }

    private void notifyRecyclerView(){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                mRequestsRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
                       mRequestFragment, R.layout.layout_requests_list_item, getContext());
                mRequestsRecyclerView.setAdapter(mRequestsRecyclerAdapter);
                mRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
                DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
                mRequestsRecyclerView.addItemDecoration(itemDecor);
                mRequestsRecyclerAdapter.notifyDataSetChanged();
            }
        });
    }

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onContactSelected(final int position) {
        initContactFragment.initContactFragment(mRecyclerList.get(position).getCid(), null);
    }
//        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
//        builder.setTitle("Accept Friend Request?");
//        builder.setPositiveButton("ACCEPT", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//
//                handleRequest(mRequests.get(position), true);
//            }
//        });
//        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//        builder.show();
//    }


    @Override
    public void onAcceptSelected(int position) {
        RequestsDialogFragment requestDialog = new RequestsDialogFragment(Constants.GET_ACCEPT_REQUEST, mRecyclerList.get(position),
                getActivity(), this);
        requestDialog.setTargetFragment(RequestsFragment.this, 1);
        requestDialog.show(getFragmentManager(), "RequestsDialogFragment");
    }

    @Override
    public void onRejectSelected(int position) {
        RequestsDialogFragment requestDialog = new RequestsDialogFragment(Constants.GET_REMOVE_REQUEST, mRecyclerList.get(position),
                getActivity(), this);
        requestDialog.setTargetFragment(RequestsFragment.this, 1);
        requestDialog.show(getFragmentManager(), "RequestsDialogFragment");
    }

    @Override
    public void onDeleteSelected(int position) {

    }

    @Override
    public void onContactLongClick(final int position) {

    }


    @Override
    public void requestAccepted(String display_name, Contact contact) {
        RequestHandler.handleRequest(contact, display_name, true);
        for (Contact cont: mRecyclerList) {
            if (cont.getCid().equals(contact.getCid())) {
                mRecyclerList.remove(contact);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        mRequestsRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
                                mRequestFragment, R.layout.layout_requests_list_item, getContext());
                        mRequestsRecyclerView.setAdapter(mRequestsRecyclerAdapter);
                        mRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
                        DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
                        mRequestsRecyclerView.addItemDecoration(itemDecor);
                        mRequestsRecyclerAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    @Override
    public void requestRemoved(Contact contact) {
        RequestHandler.handleRequest(contact, "", false);
        for (Contact cont: mRecyclerList) {
            if (cont.getCid().equals(contact.getCid())) {
                mRecyclerList.remove(contact);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        mRequestsRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
                                mRequestFragment, R.layout.layout_requests_list_item, getContext());
                        mRequestsRecyclerView.setAdapter(mRequestsRecyclerAdapter);
                        mRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
                        DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
                        mRequestsRecyclerView.addItemDecoration(itemDecor);
                        mRequestsRecyclerAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    /*
    ----------------------------- Requests Callback ---------------------------------
    */

    public interface RequestsCallback {
        HashMap<String, Contact> getRequests();
    }

}

