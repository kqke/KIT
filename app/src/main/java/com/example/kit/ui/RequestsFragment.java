package com.example.kit.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class RequestsFragment extends Fragment implements ContactRecyclerAdapter.ContactsRecyclerClickListener {
    private String m_Text;
    private RequestsCallback getData;
    private ArrayList<Contact> mContacts;


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
        Button buttonInFragment1 = rootView.findViewById(R.id.button);
        buttonInFragment1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "button in fragment 1", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        getData = (RequestsCallback)context;
        mContacts = getData.getContacts();
    }

    private void handleRequest(final Contact contact, boolean accepted){
        final FirebaseFirestore fs = FirebaseFirestore.getInstance();
        final String uid = FirebaseAuth.getInstance().getUid();
        final DocumentReference userRef = fs.collection(getString(R.string.collection_users)).document(uid);
        final User user = ((UserClient)getActivity().getApplicationContext()).getUser();
        if (accepted){
            android.app.AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Enter a display name");

            final EditText input = new EditText(getActivity());
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
                        Toast.makeText(getActivity(), "Enter a chatroom name", Toast.LENGTH_SHORT).show();
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

    public String getName(){
        return "Requests";
    }

    @Override
    public void onContactSelected(final int position) {
        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Accept Friend Request?");
        builder.setPositiveButton("ACCEPT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handleRequest(mContacts.get(position), true);
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    @Override
    public void onContactLongClick(final int position) {
        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Delete Friend Request?");
        builder.setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handleRequest(mContacts.get(position), true);
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public interface RequestsCallback {
        ArrayList<Contact> getContacts();
    }
}

