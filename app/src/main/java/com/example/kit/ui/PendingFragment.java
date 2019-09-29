package com.example.kit.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.kit.R;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

import static android.widget.LinearLayout.HORIZONTAL;

public class PendingFragment extends DBGeoFragment implements
        ContactRecyclerAdapter.ContactsRecyclerClickListener
{

    //Tag
    private static final String TAG = "PendingFragment";

    //RecyclerView
    private ContactRecyclerAdapter mPendingRecyclerAdapter;
    private RecyclerView mPendingRecyclerView;
    private ArrayList<Contact> mRecyclerList;

    //Requests
    private HashMap<String, Contact> mPending;

    //Contacts Callback
    ContactsFragment.ContactsCallback initContactFragment;

    //Listener
    private ListenerRegistration mPendingEventListener;


//    private static RequestHandler rHandler = new RequestHandler();

    /*
    ----------------------------- Lifecycle ---------------------------------
    */
    public PendingFragment() {
        super();
    }

    public static RequestsFragment newInstance() {
        RequestsFragment fragment = new RequestsFragment();
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        PendingCallback getData = (PendingCallback)context;
        mPending = getData.getPending();
        mRecyclerList = new ArrayList<>(mPending.values());
        initContactFragment = (ContactsFragment.ContactsCallback)context;
        initListener();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_pending, container, false);
        initView(rootView);
        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPendingEventListener.remove();
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(View v){
        mPendingRecyclerView = v.findViewById(R.id.pending_recycler_view);
        mPendingRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
                this, R.layout.layout_pending_list_item);
        mPendingRecyclerView.setAdapter(mPendingRecyclerAdapter);
        mPendingRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
        mPendingRecyclerView.addItemDecoration(itemDecor);
        mPendingRecyclerAdapter.notifyDataSetChanged();
        initSearchView(v);
    }

    private void initSearchView(View v){
        SearchView searchView = v.findViewById(R.id.pending_search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String queryString) {
                mPendingRecyclerAdapter.getFilter().filter(queryString);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String queryString) {
                mPendingRecyclerAdapter.getFilter().filter(queryString);
                return false;
            }
        });
    }

    private void initListener(){
        CollectionReference pendingCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_pending));
        mPendingEventListener = pendingCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Contact contact = doc.toObject(Contact.class);
                        mPending.put(contact.getCid(), contact);
                        mRecyclerList = new ArrayList<>(mPending.values());
                        mPendingRecyclerView.getAdapter().notifyDataSetChanged();
                    }
                    Log.d(TAG, "onEvent: number of contacts: " + mPending.size());

                }
            }
        });
    }

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onContactSelected(final int position) {
        initContactFragment.initContactFragment(mPending.get(position).getCid());
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
    }

    @Override
    public void onRejectSelected(int position) {
    }

    @Override
    public void onDeleteSelected(int position) {
        //TODO
        // we want to delete a pending request
    }

    @Override
    public void onContactLongClick(final int position) {

    }


    /*
    ----------------------------- Requests Callback ---------------------------------
    */

    public interface PendingCallback {
        HashMap<String, Contact> getPending();
    }

}
