package com.example.kit.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.util.RequestHandler;

import java.util.ArrayList;

import static android.widget.LinearLayout.HORIZONTAL;

public class RequestsFragment extends DBGeoFragment implements
        ContactRecyclerAdapter.ContactsRecyclerClickListener,
        RequestsDialogFragment.OnInputSelected {

    //Tag
    private static final String TAG = "RequestsFragment";

    //RecyclerView
    private ContactRecyclerAdapter mContactRecyclerAdapter;
    private RecyclerView mRequestsRecyclerView;

    //Requests
    private ArrayList<Contact> mRequests;

    //Contacts Callback
    ContactsFragment.ContactsCallback initContactFragment;

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
        RequestsCallback getData = (RequestsCallback)context;
        mRequests = getData.getRequests();
        initContactFragment = (ContactsFragment.ContactsCallback)context;
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
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(View v){
        mRequestsRecyclerView = v.findViewById(R.id.requests_recycler_view);
        mContactRecyclerAdapter = new ContactRecyclerAdapter(mRequests,
                this, R.layout.layout_requests_list_item);
        mRequestsRecyclerView.setAdapter(mContactRecyclerAdapter);
        mRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
        mRequestsRecyclerView.addItemDecoration(itemDecor);
        mContactRecyclerAdapter.notifyDataSetChanged();
        initSearchView(v);
    }

    private void initSearchView(View v){
        SearchView searchView = v.findViewById(R.id.requests_search_view);
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

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onContactSelected(final int position) {
        initContactFragment.initContactFragment(mRequests.get(position).getCid());
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
        RequestsDialogFragment requestDialog = new RequestsDialogFragment(Constants.GET_ACCEPT_REQUEST, mRequests.get(position), getActivity());
        requestDialog.setTargetFragment(RequestsFragment.this, 1);
        requestDialog.show(getFragmentManager(), "RequestsDialogFragment");
    }

    @Override
    public void onRejectSelected(int position) {
        RequestsDialogFragment requestDialog = new RequestsDialogFragment(Constants.GET_REMOVE_REQUEST, mRequests.get(position), getActivity());
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
    }

    @Override
    public void requestRemoved(Contact contact) {
        RequestHandler.handleRequest(contact, "", false);
    }

    /*
    ----------------------------- Requests Callback ---------------------------------
    */

    public interface RequestsCallback {
        ArrayList<Contact> getRequests();
    }

}

