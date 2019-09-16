package com.example.kit.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.User;
import com.example.kit.models.Username;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddContactsActivity extends AppCompatActivity {
    private String m_Text = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        addUser(intent.getStringExtra(getString(R.string.intent_contact)));
        Intent result = new Intent();
        result.putExtra(getString(R.string.intent_contact), true);
        setResult(1);
        finish();

    }


    protected void addUser(final String uid){
        User user = ((UserClient) getApplicationContext()).getUser();
        FirebaseFirestore fs = FirebaseFirestore.getInstance();
        final DocumentReference dr = fs.collection(getString(R.string.collection_users)).document(user.getUser_id());
        dr.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    Map<String, Object> toAdd = new HashMap<>();
                    getDisplayName();
                    if (m_Text.equals("")){return;}
                    toAdd.put("name", m_Text);
                    toAdd.put("cid", uid);
                    m_Text = "";
                    dr.collection(getString(R.string.collection_contacts)).document(uid).set(toAdd);
                    Intent result = new Intent();
                    result.putExtra(getString(R.string.intent_contact), true);
                    setResult(1, result);
                    finish();
                }
                else{
                    Intent result = new Intent();
                    result.putExtra(getString(R.string.intent_contact), false);
                    setResult(1, result);
                    finish();
                }
            }
        });
    }

    private void getDisplayName(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                m_Text = input.getText().toString();
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
