package com.example.kit.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.User;
import com.example.kit.util.UsernameValidator;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.example.kit.Constants.AUTH;
import static com.example.kit.Constants.EMPTY;
import static com.example.kit.Constants.RC_SIGN_IN;
import static com.example.kit.Constants.WRONG_FORMAT;
import static com.example.kit.util.Check.isEmpty;

public class LoginActivity extends AppCompatActivity
        implements View.OnClickListener
{
    //Tag
    private static final String TAG = "LoginActivity";

    //Firebase
    private FirebaseFirestore mDb;
    private FirebaseAuth mAuth;

    //widgets
    private EditText mUsername;

    //TODO
    // add a sign in button at the beginning of the run
    // currently whenever the user signs out, he's automatically thrown into re-authentication


    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        initView();
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void init(){
        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();
        getIncomingIntent();
    }

    private void getIncomingIntent(){
        if(getIntent().getBooleanExtra(AUTH, false)){
            startAuthenticationActivity();
        }
    }

    private void initView(){
        //TODO
        // add logo to the registration button
        setContentView(R.layout.activity_login);
        findViewById(R.id.auth).setOnClickListener(this);
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /*
    ----------------------------- OnClick ---------------------------------
    */

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case(R.id.auth):
                startAuthenticationActivity();
                break;
            case(R.id.btn_go):{
                Log.d(TAG, "onClick: attempting to verify username.");
                String userName = mUsername.getText().toString();
                switch (checkUsername(userName)) {
                    case EMPTY:{
                        Toast.makeText(LoginActivity.this,
                                "Error message informing of empty username",
                                Toast.LENGTH_SHORT).show();
                        break;
                    }
                    case WRONG_FORMAT: {

                        // TODO
                        // maybe add another func for more specific error codes

                        Toast.makeText(LoginActivity.this,
                                "Error message informing of username containing wrong chars",
                                Toast.LENGTH_SHORT).show();
                        break;
                    }
                    default:
                        checkExistingUsername(userName);
                }
                break;
            }
        }
    }

    /*
    ----------------------------- nav ---------------------------------
    */

    protected void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(AUTH, true);
        startActivity(intent);
        finish();
    }

    public void startAuthenticationActivity () {
        if(mAuth.getCurrentUser() == null) {

            ArrayList<AuthUI.IdpConfig> idps = new ArrayList<>();

            idps.add(new AuthUI.IdpConfig.EmailBuilder().build());
            idps.add(new AuthUI.IdpConfig.GoogleBuilder().build());
            idps.add(new AuthUI.IdpConfig.PhoneBuilder().build());

            startActivityForResult(AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(idps)
                    .build(), RC_SIGN_IN);
        }
        else{
            getUserName();
        }
    }

        @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // RC_SIGN_IN is the request code you passed into  startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            // Successfully signed in
            if(resultCode == RESULT_OK)
            {
                checkExistingUser();
            }
            else {
                Log.d(TAG, "LOLZ");
            }
        }
    }

    /*
    ----------------------------- DB ---------------------------------
    */

    private void registerNewUser(String userName){
        FirebaseUser cur_user = mAuth.getCurrentUser();
        User user = new User();
        String email = cur_user.getEmail();
        user.setEmail(email);
        user.setUsername(userName);
        user.setUser_id(FirebaseAuth.getInstance().getUid());

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(settings);

        DocumentReference newUserRef = db
                .collection(getString(R.string.collection_users))
                .document(mAuth.getUid());
        newUserRef.set(user);
        Toast.makeText(LoginActivity.this,
                "Authenticated with: " + user.getEmail(),
                Toast.LENGTH_SHORT).show();
        ((UserClient) (getApplicationContext())).setUser(user);
        Log.d(TAG, "setCurrentUser: successfully set the user client.");
    }

/*
----------------------------- Username setup ---------------------------------
*/
    private void getUserName(){
        //TODO
        // changes to the xml
        setContentView(R.layout.activity_username);
        mUsername = findViewById(R.id.input_username);
        findViewById(R.id.btn_go).setOnClickListener(this);
        hideSoftKeyboard();
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

    private void checkExistingUser(){
        CollectionReference usersRef = mDb.collection("Users");
        Query query = usersRef.whereEqualTo("user_id", mAuth.getUid());
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    if (task.getResult() != null) {
                        for (DocumentSnapshot documentSnapshot : task.getResult()) {
                            String userID = documentSnapshot.getString("user_id");
                            if (userID.equals(mAuth.getUid())) {
                                Log.d(TAG, "chcekExistingUser: User Exists");
                                startMainActivity();
                            }
                        }
                        if(task.getResult().size() == 0){
                            Log.d(TAG, "checkExistingUser: user does not exist in DB");
                            getUserName();
                        }
                    }
                }
            }
        });
    }


    private void checkExistingUsername(final String userName)
    {
        CollectionReference usersRef = mDb.collection("Users");
        Query query = usersRef.whereEqualTo("username", userName);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    if (task.getResult() != null) {
                        for (DocumentSnapshot documentSnapshot : task.getResult()) {
                            String user = documentSnapshot.getString("username");

                            if (user.equals(userName)) {
                                Log.d(TAG, "chcekExisting: User Exists");
                                Toast.makeText(LoginActivity.this,
                                        "checkExisting: This username is already taken",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        if(task.getResult().size() == 0 ){
                            Log.d(TAG, "checkExisting: User chose a unique username");
                            // SUCCESS
                            registerNewUser(userName);
                            startMainActivity();
                        }
                    }
                }
            }
        });
    }
}