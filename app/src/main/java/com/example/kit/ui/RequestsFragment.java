package com.example.kit.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.models.User;
import com.example.kit.services.RequestHandler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class RequestsFragment extends DBGeoFragment implements ContactRecyclerAdapter.ContactsRecyclerClickListener,
        RequestsDialogFragment.OnInputSelected {

    private static RequestHandler rHandler = new RequestHandler();
    private static final String TAG = "RequestsFragment";

    private String m_Text;
    private RequestsCallback getData;
    private ArrayList<Contact> mRequests;
    private ContactRecyclerAdapter mContactRecyclerAdapter;
    private RecyclerView mRequestsRecyclerView;


    public RequestsFragment() {
        // Required empty public constructor
    }

    public static RequestsFragment newInstance() {
        RequestsFragment fragment = new RequestsFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_requests, container, false);
        initView(rootView);
        return rootView;
    }

    private void initView(View v){
        mRequestsRecyclerView = v.findViewById(R.id.requests_recycler_view);
        mContactRecyclerAdapter = new ContactRecyclerAdapter(mRequests, this);
        mRequestsRecyclerView.setAdapter(mContactRecyclerAdapter);
        mRequestsRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
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


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        getData = (RequestsCallback)context;
        mRequests = getData.getRequests();
    }


    public String getName(){
        return "Requests";
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }



    @Override
    public void onContactSelected(final int position) {
        RequestsDialogFragment requestDialog = new RequestsDialogFragment(Constants.GET_ACCEPT_REQUEST, mRequests.get(position), getActivity());
        requestDialog.setTargetFragment(RequestsFragment.this, 1);
        requestDialog.show(getFragmentManager(), "RequestsDialogFragment");
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
    public void onContactLongClick(final int position) {
        RequestsDialogFragment requestDialog = new RequestsDialogFragment(Constants.GET_REMOVE_REQUEST, mRequests.get(position), getActivity());
        requestDialog.setTargetFragment(RequestsFragment.this, 1);
        requestDialog.show(getFragmentManager(), "RequestsDialogFragment");
    }

    @Override
    public void requestAccepted(String display_name, Contact contact) {
        RequestHandler.handleRequest(contact, display_name, true);
    }

    @Override
    public void requestRemoved(Contact contact) {
        RequestHandler.handleRequest(contact, "", false);
    }


    public interface RequestsCallback {
        ArrayList<Contact> getRequests();
    }

    public Activity getA(){
        return getActivity();
    }

}

