package com.example.kit.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.kit.R;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static android.widget.LinearLayout.HORIZONTAL;


public class ContactsFragment extends DBGeoFragment implements
        ContactRecyclerAdapter.ContactsRecyclerClickListener,
        View.OnClickListener
{

    //TODO
    // fix Contacts recycler adapter so it wont check the unseen checkbox onlongclick

    //Tag
    private static final String TAG = "ContactsFragment";

    //RecyclerView
    private ContactRecyclerAdapter mContactRecyclerAdapter;
    private RecyclerView mContactRecyclerView;

    //Contacts
    private ArrayList<Contact> mContacts = new ArrayList<>();
    private Set<String> mContactIds = new HashSet<>();

    //Contacts Callback
    ContactsCallback getData;

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public ContactsFragment() {
        super();
    }

    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        getData = (ContactsCallback)context;
        mContacts = getData.getContacts();
        mContactIds = getData.getContactIds();
        if (mContactRecyclerAdapter != null){
            mContactRecyclerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        initView(view);
        return view;
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(View v){
        v.findViewById(R.id.fab).setOnClickListener(this);
        mContactRecyclerView = v.findViewById(R.id.contact_recycler_view);
        mContactRecyclerAdapter = new ContactRecyclerAdapter(mContacts,
                this, R.layout.layout_contact_list_item);
        mContactRecyclerView.setAdapter(mContactRecyclerAdapter);
        mContactRecyclerAdapter.notifyDataSetChanged();
        mContactRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
        mContactRecyclerView.addItemDecoration(itemDecor);
        v.findViewById(R.id.fab).setOnClickListener(this);
        initSearchView(v);
    }

    private void initSearchView(View v){
        SearchView searchView = v.findViewById(R.id.search_view);
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
    public void onContactSelected(int position) {
        getData.initContactFragment(mContacts.get(position).getCid());
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.fab){
            //TODO
            // add a new contact dialog (Fragment?/some sort of a bubble?)
            newContactDialog();
        }
    }

    @Override
    public void onContactLongClick(int pos) {
        // TODO
        // will it have any significance here?
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
    ----------------------------- nav ---------------------------------
    */

    private void navContactActivity(Contact contact){
        //TODO
        // ContactActivity is deprecated
//        Intent intent = new Intent(mActivity, ContactActivity.class);
//        intent.putExtra(getString(R.string.intent_contact), contact);
//        startActivityForResult(intent, 0);
    }

    private void navAddContactActivity(User user){
        Intent intent = new Intent(mActivity, AddContactsActivity.class);
        intent.putExtra(getString(R.string.intent_contact), user);
        startActivityForResult(intent, 0);
    }

    //TODO
    // is this sufficient UI wise?
    private void newContactDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle("Enter a username");

        final EditText input = new EditText(mActivity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!input.getText().toString().equals("")){
                    search(input.getText().toString());
                }
                else {
                    Toast.makeText(mActivity, "Enter a username", Toast.LENGTH_SHORT).show();
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

    /*
    ----------------------------- DB ---------------------------------
    */

    protected void search(final String username){
        CollectionReference usersRef = mDb.collection("Users");
        Query query = usersRef.whereEqualTo("username", username);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot documentSnapshot : task.getResult()) {
                        String uname = documentSnapshot.getString("username");
                        if (uname.equals(username)) {
                            Log.d(TAG, "User Exists");
                            User user = documentSnapshot.toObject(User.class);
                            Log.d(TAG, "onComplete: Barak contact id" + user.getUser_id());
                            navAddContactActivity(user);
                        }
                    }
                }
            }
        });
    }

    /*
    ----------------------------- Contacts Callback ---------------------------------
    */

    public interface ContactsCallback {
        ArrayList<Contact> getContacts();
        Set<String> getContactIds();
        HashMap<String, Contact> getId2Contact();
        void initContactFragment(String contactID);
    }
}
