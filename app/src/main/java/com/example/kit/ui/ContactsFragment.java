package com.example.kit.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
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
import com.example.kit.UserClient;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import static android.app.Activity.RESULT_OK;
import static com.example.kit.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.example.kit.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;


public class ContactsFragment extends DBGeoFragment implements
        ContactRecyclerAdapter.ContactsRecyclerClickListener,
        View.OnClickListener
{

    //Tag
    private static final String TAG = "ContactsFragment";

    //RecyclerView
    private ContactRecyclerAdapter mContactRecyclerAdapter;
    private RecyclerView mContactRecyclerView;

    //Contacts
    private ArrayList<Contact> mContacts = new ArrayList<>();
    private Set<String> mContactIds = new HashSet<>();

    //Callback
    ContactsCallback getData;

    private String m_Text;

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
    }

    @Override
    public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        getContacts();
//    }

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
        mContactRecyclerView = v.findViewById(R.id.contact_recycler_view);
        mContactRecyclerAdapter = new ContactRecyclerAdapter(mContacts, this);
        mContactRecyclerView.setAdapter(mContactRecyclerAdapter);
        mContactRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
    }

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onContactSelected(int position) {
        //TODO
        // ContactActivity is deprecated
        navContactActivity(mContacts.get(position));
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.fab){
            //TODO
            // add a new contact dialog (Fragment?/some sort of a bubble?)
            newContactDialog();
        }
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

//    @Override
//    public void onRequestPermissionsResult(int requestCode,
//                                           @NonNull String permissions[],
//                                           @NonNull int[] grantResults) {
//        switch (requestCode) {
//            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    setLocationPermissionGranted(true);
//                }
//            }
//        }
//    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        Log.d(TAG, "onActivityResult: called.");
//        switch (requestCode) {
//            case PERMISSIONS_REQUEST_ENABLE_GPS: {
//                if(isLocationPermissionGranted()){
//                    getContacts();
//                    getUserDetails();
//                }
//                else{
//                    getLocationPermission();
//                }
//                break;
//            }
//            //TODO
//            // better result codes
//            case 1: {
//                if (data.getBooleanExtra(getString(R.string.intent_contact), true)) {
//                    mContactRecyclerAdapter.notifyDataSetChanged();
//                }
//                break;
//            }
//            case 0: {
//                if (resultCode == RESULT_OK){
//                    if ((boolean)data.getExtras().get("left")){
//                        mContactRecyclerAdapter.notifyItemRangeChanged(0, mContactRecyclerAdapter.getItemCount());
//                    }
//                }
//            }
//        }
//    }

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


    private void handleRequest(final Contact contact, boolean accepted){
        final FirebaseFirestore fs = FirebaseFirestore.getInstance();
        final String uid = FirebaseAuth.getInstance().getUid();
        final DocumentReference userRef = fs.collection(getString(R.string.collection_users)).document(uid);
        final User user = ((UserClient)mActivity.getApplicationContext()).getUser();
        if (accepted){
            android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle("Enter a display name");

            final EditText input = new EditText(mActivity);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!input.getText().toString().equals("")) {
                        m_Text = input.getText().toString();
                        userRef.collection(getString(R.string.collection_contacts)).document(contact.getCid()).set(new Contact(m_Text,
                                contact.getUsername(), contact.getAvatar(), contact.getCid())).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                userRef.collection(getString(R.string.collection_requests)).document(contact.getCid()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        //todo notify recyclerview
                                    }
                                });
                                final DocumentReference contactRef =
                                        fs.collection(getString(R.string.collection_users)).document(contact.getCid());
                                contactRef.collection(getString(R.string.collection_pending)).document(user.getUser_id()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (!task.isSuccessful()){return;}
                                        final Contact ucontact = task.getResult().toObject(Contact.class);
                                        contactRef.collection(getString(R.string.collection_contacts)).document(ucontact.getCid()).set(ucontact).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (!task.isSuccessful()) {return;}
                                                contactRef.collection(getString(R.string.collection_pending)).document(ucontact.getCid()).delete();
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                    else {
                        Toast.makeText(mActivity, "Enter a chatroom name", Toast.LENGTH_SHORT).show();
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

        else {
            userRef.collection(getString(R.string.collection_requests)).document(contact.getCid()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    //todo notify recyclerview
                }
            });
            fs.collection(getString(R.string.collection_users)).document(contact.getCid()).collection(getString(R.string.collection_pending)).document(uid).delete();

        }
    }



//    //TODO
//    // should be probably in the requests fragment
//    private void acceptRequest(final Contact contact){
//        final FirebaseFirestore fs = FirebaseFirestore.getInstance();
//        final String uid = FirebaseAuth.getInstance().getUid();
//        final DocumentReference userRef = fs.collection(getString(R.string.collection_users)).document(uid);
//        final User user = ((UserClient)mActivity.getApplicationContext()).getUser();
//        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
//        builder.setTitle("Enter a display name");
//
//        final EditText input = new EditText(mActivity);
//        input.setInputType(InputType.TYPE_CLASS_TEXT);
//        builder.setView(input);
//
//        builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                if (!input.getText().toString().equals("")) {
//                    m_Text = input.getText().toString();
//                    userRef.collection(getString(R.string.collection_contacts)).document(contact.getCid()).set(new Contact(m_Text,
//                            contact.getUsername(), contact.getAvatar(), contact.getCid())).addOnCompleteListener(new OnCompleteListener<Void>() {
//                        @Override
//                        public void onComplete(@NonNull Task<Void> task) {
//                            userRef.collection(getString(R.string.collection_requests)).document(contact.getCid()).delete();
//                            final DocumentReference contactRef =
//                                    fs.collection(getString(R.string.collection_users)).document(contact.getCid());
//                            contactRef.collection(getString(R.string.collection_pending)).document(user.getUser_id()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                                @Override
//                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                                    if (!task.isSuccessful()){return;}
//                                    final Contact ucontact = task.getResult().toObject(Contact.class);
//                                    contactRef.collection(getString(R.string.collection_contacts)).document(ucontact.getCid()).set(ucontact).addOnCompleteListener(new OnCompleteListener<Void>() {
//                                        @Override
//                                        public void onComplete(@NonNull Task<Void> task) {
//                                            if (!task.isSuccessful()) {return;}
//                                            contactRef.collection(getString(R.string.collection_pending)).document(ucontact.getCid()).delete();
//                                        }
//                                    });
//                                }
//                            });
//                        }
//                    });
//                }
//                else {
//                    Toast.makeText(mActivity, "Enter a chatroom name", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//        builder.show();
//    }

    public interface ContactsCallback {
        ArrayList<Contact> getContacts();
        Set<String> getContactIds();
        HashMap<String, Contact> getId2Contact();
    }
}
