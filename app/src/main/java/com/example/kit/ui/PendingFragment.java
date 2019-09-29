package com.example.kit.ui;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.util.RequestHandler;

import java.util.ArrayList;

import static android.widget.LinearLayout.HORIZONTAL;

public class PendingFragment extends DBGeoFragment implements
        ContactRecyclerAdapter.ContactsRecyclerClickListener
{

    //Tag
    private static final String TAG = "PendingFragment";

    //RecyclerView
    private ContactRecyclerAdapter mContactRecyclerAdapter;
    private RecyclerView mPendingRecyclerView;

    //Requests
    private ArrayList<Contact> mPending;

    //Contacts Callback
    ContactsFragment.ContactsCallback initContactFragment;

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
        initContactFragment = (ContactsFragment.ContactsCallback)context;
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
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(View v){
        mPendingRecyclerView = v.findViewById(R.id.pending_recycler_view);
        mContactRecyclerAdapter = new ContactRecyclerAdapter(mPending,
                this, R.layout.layout_pending_list_item);
        mPendingRecyclerView.setAdapter(mContactRecyclerAdapter);
        mPendingRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
        mPendingRecyclerView.addItemDecoration(itemDecor);
        mContactRecyclerAdapter.notifyDataSetChanged();
        initSearchView(v);
    }

    private void initSearchView(View v){
        SearchView searchView = v.findViewById(R.id.pending_search_view);
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
        ArrayList<Contact> getPending();
    }

}
