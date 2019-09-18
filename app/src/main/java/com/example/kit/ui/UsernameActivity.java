package com.example.kit.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.kit.R;
import com.example.kit.util.UsernameValidator;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import static com.example.kit.Constants.EMPTY;
import static com.example.kit.Constants.WRONG_FORMAT;
import static com.example.kit.util.Check.isEmpty;

public class UsernameActivity extends AppCompatActivity
        implements View.OnClickListener
{
    private static final String TAG = "UsernameActivity";

    //widgets
    private EditText mUsername;
    private ProgressBar mProgressBar;

    //vars
    private FirebaseFirestore mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_username);
        mUsername = (EditText) findViewById(R.id.input_username);
        mProgressBar = findViewById(R.id.progressBar);
        findViewById(R.id.btn_go).setOnClickListener(this);
        mDb = FirebaseFirestore.getInstance();

        hideSoftKeyboard();
    }

    @Override
    public void onClick(View v) {
        showDialog();
        switch (v.getId()){
            case R.id.btn_go: {
                Log.d(TAG, "onClick: attempting to verify username.");
                String userName = mUsername.getText().toString();
                    switch (checkUsername(userName)) {
                        case EMPTY:{
                            Toast.makeText(UsernameActivity.this,
                                    "Error message informing of empty username",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        }

                        case WRONG_FORMAT: {
                            // TODO
                            // maybe add another func for more specific error codes
                            Toast.makeText(UsernameActivity.this,
                                    "Error message informing of username containing wrong chars",
                                    Toast.LENGTH_SHORT).show();
                            break;
                        }
                        default:
                            checkExisting(userName);
                    }
                    hideDialog();
                    break;
            }
        }
    }

    private int checkUsername(String userName)
    {
        if(isEmpty(userName))
        {
            return EMPTY;
        }
        if (!new UsernameValidator().validate(userName))
        {
            return WRONG_FORMAT;
        }
        return 0;
    }

    private void checkExisting(final String userName)
    {
        CollectionReference usersRef = mDb.collection("Users");
        Query query = usersRef.whereEqualTo("username", userName);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(DocumentSnapshot documentSnapshot : task.getResult()){
                        String user = documentSnapshot.getString("username");

                        if(user.equals(userName)){
                            Log.d(TAG, "User Exists");
                            Toast.makeText(UsernameActivity.this,
                                    "Username exists",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                if(task.getResult().size() == 0 ){
                    Log.d(TAG, "User not Exists");
                    // SUCCESS
                    Intent data = new Intent();
                    data.setData(Uri.parse(userName));
                    setResult(RESULT_OK, data);
                    finish();

                }
            }
        });
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void showDialog(){
        mProgressBar.setVisibility(View.VISIBLE);

    }

    private void hideDialog(){
        if(mProgressBar.getVisibility() == View.VISIBLE){
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }
}
