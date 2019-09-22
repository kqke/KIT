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
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.models.UserLocation;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.firebase.auth.FirebaseAuth;

import static com.example.kit.Constants.MAPVIEW_BUNDLE_KEY;

public class UserListFragment extends MapFragment
        implements OnMapReadyCallback,
        ContactRecyclerAdapter.ContactsRecyclerClickListener{

    private static final String TAG = "UserListFragment";

    //widgets
    private RecyclerView mUserListRecyclerView;

    //RecyclerView
    private ContactRecyclerAdapter mUserRecyclerAdapter;


    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public static UserListFragment newInstance() {
        return new UserListFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mContactLocations.size() == 0) {
            if (getArguments() != null) {
                mContactList = getArguments().getParcelableArrayList(getString(R.string.intent_user_list));
                mContactLocations = getArguments().getParcelableArrayList(getString(R.string.intent_user_locations));
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
        for (UserLocation ul : mContactLocations) {
            if (ul.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())) {
                mUserLocation = ul;
            }
        }
    }

    private void initUserListRecyclerView() {
        mUserRecyclerAdapter = new ContactRecyclerAdapter(mContactList, this);
        mUserListRecyclerView.setAdapter(mUserRecyclerAdapter);
        mUserListRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

        /*
    ----------------------------- nav ---------------------------------
    */

    private void navContactActivity(Contact contact){
        //TODO
        // ContactActivity is deprecated
//        Intent intent = new Intent(mActivity, ContactActivity.class);
//        intent.putExtra(getString(R.string.intent_contact), contact);
//        startActivityForResult(intent, 0);
    }

    @Override
    public void onContactSelected(int position) {
        //TODO
        // ContactActivity is deprecated
        navContactActivity(mContactList.get(position));
    }

    /*
    ----------------------------- Map ---------------------------------
    */

    protected void initGoogleMap(Bundle savedInstanceState) {
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);
    }
}