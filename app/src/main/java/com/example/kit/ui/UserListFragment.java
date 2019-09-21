package com.example.kit.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kit.R;
import com.example.kit.adapters.UserRecyclerAdapter;
import com.example.kit.models.User;
import com.example.kit.models.UserLocation;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class UserListFragment extends MapFragment
        implements OnMapReadyCallback {

    private static final String TAG = "UserListFragment";

    //widgets
    private RecyclerView mUserListRecyclerView;

    //RecyclerView
    private UserRecyclerAdapter mUserRecyclerAdapter;

    //vars
    private ArrayList<User> mUserList = new ArrayList<>();
    private ArrayList<UserLocation> mUserLocations = new ArrayList<>();

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public static UserListFragment newInstance() {
        return new UserListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mUserLocations.size() == 0) {
            if (getArguments() != null) {
                mUserList = getArguments().getParcelableArrayList(getString(R.string.intent_user_list));
                mUserLocations = getArguments().getParcelableArrayList(getString(R.string.intent_user_locations));
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_list, container, false);
        mUserListRecyclerView = view.findViewById(R.id.user_list_recycler_view);
        mMapView = view.findViewById(R.id.user_list_map);
        initUserListRecyclerView();
        initGoogleMap(savedInstanceState);
        setUserPosition();
        return view;
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void setUserPosition() {
        for (UserLocation ul : mUserLocations) {
            if (ul.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())) {
                mUserLocation = ul;
            }
        }
    }

    private void initUserListRecyclerView() {
        mUserRecyclerAdapter = new UserRecyclerAdapter(mUserList);
        mUserListRecyclerView.setAdapter(mUserRecyclerAdapter);
        mUserListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }
}