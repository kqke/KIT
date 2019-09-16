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
import com.example.kit.models.Username;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddContactsActivity extends AppCompatActivity {
    private String m_Text = "";
    private static final String TAG = "AddContact";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contacts);
        Intent intent = getIntent();
        addUser(intent.getStringExtra(getString(R.string.intent_contact)), intent.getStringExtra("un"));

    }


    protected void addUser(final String uid, final String username) {
        Log.d(TAG, "addUser: uid = " + uid);
        User user = ((UserClient) getApplicationContext()).getUser();
        FirebaseFirestore fs = FirebaseFirestore.getInstance();
        final DocumentReference dr = fs.collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid());
        dr.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "onComplete: start");
                    newContactDialog(uid, username);
                }
            }
        });
    }

    private void newContactDialog(final String uid, final String username){
        FirebaseFirestore fs = FirebaseFirestore.getInstance();
        final DocumentReference dr = fs.collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid());
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
                    Contact contact = new Contact(m_Text, username, null, uid);
                    dr.collection(getString(R.string.collection_contacts)).document(uid).set(contact).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "DocumentSnapshot successfully written!");
                            m_Text = "";
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
