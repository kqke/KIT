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

import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.models.User;
import com.example.kit.util.FCM;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import static android.widget.LinearLayout.HORIZONTAL;


public class ContactsFragment extends DBGeoFragment implements
        ContactRecyclerAdapter.ContactsRecyclerClickListener,
        View.OnClickListener,
        ContactsDialogFragment.OnInputSelected
{

    //TODO
    // fix Contacts recycler adapter so it wont check the unseen checkbox onlongclick

    //Tag
    private static final String TAG = "ContactsFragment";

    //RecyclerView
    private ContactRecyclerAdapter mContactRecyclerAdapter;
    private RecyclerView mContactRecyclerView;

    //Contacts
    private HashMap<String, Contact> mContacts = new HashMap<>();
    private ArrayList<Contact> mRecyclerList = new ArrayList<>();

    //Contacts Callback
    ContactsCallback getData;

    //Listeners
    private ListenerRegistration mContactEventListener;

    //Vars
    private ContactsFragment mContactFragment;

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
        mContactFragment = this;
        getData = (ContactsCallback)context;
        mContacts = getData.getId2Contact();
        mRecyclerList = new ArrayList<>(mContacts.values());
        if (mContactRecyclerAdapter != null){
            mContactRecyclerAdapter.notifyDataSetChanged();
        }
        initListener();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mContactEventListener.remove();
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
        mContactRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
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

    private void initListener(){
        CollectionReference contactsCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_contacts));
        mContactEventListener = contactsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        Contact contact = doc.toObject(Contact.class);
                        mContacts.put(contact.getCid(), contact);
                        mRecyclerList = new ArrayList<>(mContacts.values());
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            public void run() {
                                mContactRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
                                        mContactFragment, R.layout.layout_contact_list_item);
                                mContactRecyclerView.setAdapter(mContactRecyclerAdapter);
                                mContactRecyclerAdapter.notifyDataSetChanged();
                                mContactRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
                                DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
                                mContactRecyclerView.addItemDecoration(itemDecor);
                                mContactRecyclerAdapter.notifyDataSetChanged();
                            }
                        });

                        Log.d(TAG, "onEvent: number of contacts: " + mContacts.size());

                    }
                }
            }
        });
    }


    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onContactSelected(int position) {
        getData.initContactFragment(mRecyclerList.get(position).getCid());
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.fab){
            //TODO
            // add a new contact dialog (Fragment?/some sort of a bubble?)
            User user = ((UserClient) (getActivity().getApplicationContext())).getUser();
            ContactsDialogFragment contactsFragment = new ContactsDialogFragment(Constants.GET_SEARCH_REQUEST, user,
                    getActivity(), mContactFragment);
            contactsFragment.setTargetFragment(ContactsFragment.this, 1);
            contactsFragment.show(getFragmentManager(), "ContactsDialogFragment");
        }
    }

    @Override
    public void onContactLongClick(int pos) {
        // TODO
        // will it have any significance here?
//        ContactsDialogFragment contactsFragment = new ContactsDialogFragment(Constants.GET_REMOVE_REQUEST, mRecyclerList.get(pos),
//                getActivity(), mContactFragment);
//        contactsFragment.setTargetFragment(ContactsFragment.this, 1);
//        contactsFragment.show(getFragmentManager(), "ContactsDialogFragment");
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

//    //TODO
//    // is this sufficient UI wise?
//    private void newContactDialog(){
//        ContactsFragment contactsFragment = new ContactsFragment(Constants.GET_REMOVE_REQUEST, ,
//                getActivity(), mContactFragment);
//        contactsFragment.setTargetFragment(ContactsFragment.this, 1);
//        contactsFragment.show(getFragmentManager(), "RequestsDialogFragment");
//    }

    /*
    ----------------------------- DB ---------------------------------
    */

    @Override
    public void addContact(String display_name, final User contactUser) {
        final User user = ((UserClient) (getActivity().getApplicationContext())).getUser();
        Contact contact = new Contact(display_name, contactUser.getUsername(), null, contactUser.getUser_id(), contactUser.getStatus());
        mDb.collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).collection(getString(R.string.collection_pending)).document(contactUser.getUser_id()).set(contact).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "DocumentSnapshot successfully written!");
                DocumentReference contactRef =
                        mDb.collection(getString(R.string.collection_users)).document(contactUser.getUser_id()).collection(getString(R.string.collection_requests)).document(user.getUser_id());
                contactRef.set(new Contact(user.getUsername(), user.getEmail(), user.getAvatar(), user.getUser_id(), user.getStatus())).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        FCM.send_FCM_Notification(contactUser.getToken(), "Friend Request", user.getUsername() + " has sent " +
                                "you a friend request");

                    }
                });


            }
        });
    }

    @Override
    public void search(final String username){
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
                            ContactsDialogFragment contactsDialog = new ContactsDialogFragment(Constants.GET_DISPLAY_NAME,
                                    user, getActivity(), mContactFragment);
                            contactsDialog.setTargetFragment(ContactsFragment.this, 1);
                            contactsDialog.show(getFragmentManager(), "RequestsDialogFragment");
                        }
                    }
                }
            }
        });
    }

    @Override
    public void remove(final Contact contact) {
        mDb.collection(getString(R.string.collection_users)).document(contact.getCid()).collection(getString(R.string.collection_contacts)).document(FirebaseAuth.getInstance().getUid()).delete();
        mDb.collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).collection(getString(R.string.collection_contacts)).document(contact.getCid()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                mContacts.remove(contact.getCid());
                mRecyclerList = new ArrayList<>(mContacts.values());
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    public void run() {
                        mContactRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
                                mContactFragment, R.layout.layout_contact_list_item);
                        mContactRecyclerView.setAdapter(mContactRecyclerAdapter);
                        mContactRecyclerAdapter.notifyDataSetChanged();
                        mContactRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
                        DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
                        mContactRecyclerView.addItemDecoration(itemDecor);
                        mContactRecyclerAdapter.notifyDataSetChanged();
                    }
                });
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
