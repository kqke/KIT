package com.example.kit.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.Contact;
import com.example.kit.models.User;
import com.example.kit.util.FCM;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;


public class AddContactsActivity extends AppCompatActivity {
    private String m_Text = "";
    private static final String TAG = "AddContact";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contacts);
        Intent intent = getIntent();
        addUser((User)intent.getParcelableExtra(getString(R.string.intent_contact)));

    }


    protected void addUser(final User contact) {
        Log.d(TAG, "addUser: uid = " + contact.getUser_id());
        FirebaseFirestore fs = FirebaseFirestore.getInstance();
        final DocumentReference user = fs.collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid());
        user.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete: start");
                    newContactDialog(contact);
                }
            }
        });
    }

    private void newContactDialog(final User contactUser){
        final FirebaseFirestore fs = FirebaseFirestore.getInstance();
        final String uid = FirebaseAuth.getInstance().getUid();
        final DocumentReference userRef = fs.collection(getString(R.string.collection_users)).document(uid);
        final User user = ((UserClient)getApplicationContext()).getUser();
        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter a display name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!input.getText().toString().equals("")) {
                    m_Text = input.getText().toString();
                    Contact contact = new Contact(m_Text, contactUser.getUsername(), null, contactUser.getUser_id());
                    userRef.collection(getString(R.string.collection_pending)).document(contactUser.getUser_id()).set(contact).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully written!");
                            m_Text = "";
                            DocumentReference contactRef =
                                    fs.collection(getString(R.string.collection_users)).document(contactUser.getUser_id()).collection(getString(R.string.collection_requests)).document(uid);
                            contactRef.set(new Contact(user.getUsername(), user.getEmail(), user.getAvatar(), user.getUser_id())).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    FCM.send_FCM_Notification(contactUser.getToken(), "Friend Request", user.getUsername() +" has sent " +
                                            "you a friend request");
                                    Intent result = new Intent();
                                    result.putExtra(getString(R.string.intent_contact), true);
                                    setResult(1, result);
                                    Log.d(TAG, "onComplete: finish");
                                    finish();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "Error writing document", e);
                                    Intent result = new Intent();
                                    result.putExtra(getString(R.string.intent_contact), false);
                                    setResult(1, result);
                                    Log.d(TAG, "onComplete: finish");
                                    finish();
                                }
                            });

                        }
                    })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "Error writing document", e);
                                    Intent result = new Intent();
                                    result.putExtra(getString(R.string.intent_contact), false);
                                    setResult(1, result);
                                    Log.d(TAG, "onComplete: finish");
                                    finish();
                                }
                            });

                }



                else {
                    Toast.makeText(AddContactsActivity.this, "Enter a display name", Toast.LENGTH_SHORT).show();
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


}
