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

import static com.example.kit.Constants.EMPTY;
import static com.example.kit.Constants.WRONG_FORMAT;
import static com.example.kit.util.Check.isEmpty;

public class LoginActivity extends AppCompatActivity
        implements View.OnClickListener
{
    //Tag
    private static final String TAG = "LoginActivity";

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseFirestore mDb;
    private FirebaseAuth mAuth;

    //widgets
    private EditText mUsername;
    private ProgressBar mProgressBar;

    // TODO
    //  check which is better - onActivityResult or onAuthStateChanged;
    //  does the usage of the latter create a problem of concurrency,
    //  or does the AuthenticationActivity have to be initiated with startActivityForResult

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
        setupFirebaseAuth();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
        }
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void init(){
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();

    }

    private void initView(){
        setContentView(R.layout.activity_login);
        mProgressBar = findViewById(R.id.progressBar);
        showLoading();
    }

    private void showLoading(){

        // TODO
        //  We can put here the logo of the app with loading animation or smth.

        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideLoading(){
        if(mProgressBar.getVisibility() == View.VISIBLE){
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    /*
    ----------------------------- nav ---------------------------------
    */

    protected void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void startAuthenticationActivity () {

        ArrayList<AuthUI.IdpConfig> idps = new ArrayList<>();

        idps.add(new AuthUI.IdpConfig.EmailBuilder().build());
        idps.add(new AuthUI.IdpConfig.GoogleBuilder().build());
        idps.add(new AuthUI.IdpConfig.PhoneBuilder().build());

        startActivity(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(idps)
                        .build());
    }

    //    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        // RC_SIGN_IN is the request code you passed into  startActivityForResult(...) when starting the sign in flow.
//        if (requestCode == RC_SIGN_IN) {
//            IdpResponse response = IdpResponse.fromResultIntent(data);
//
//            // Successfully signed in
//            if (resultCode == RESULT_OK) {
//                // Success
////                startMainActivity();
//            }
//            else {
//                // Handle Error
//            }
//        }
//    }

    /*
    ----------------------------- DB ---------------------------------
    */

    private User registerNewUser(String userName){
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
                .document(FirebaseAuth.getInstance().getUid());

        newUserRef.set(user);
        return user;
    }

    private void updateToken(){
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String newToken = instanceIdResult.getToken();
                Log.e("newToken", newToken);
                sendRegistrationToServer(newToken);
            }
        });
    }

    private void sendRegistrationToServer(String token){
        try{
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            DocumentReference usersRef = FirebaseFirestore.getInstance()
                    .collection(getString(R.string.collection_users))
                    .document(FirebaseAuth.getInstance().getUid());
            usersRef.set(data, SetOptions.merge()).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "TOKEN successfully written!");
                }
            });
        }catch (NullPointerException e){
            Log.e(TAG, "User instance is null, stopping notification service.");
            Log.e(TAG, "saveToken: NullPointerException: "  + e.getMessage() );
        }
    }

    private void setCurrentUser(User user){
        Toast.makeText(LoginActivity.this,
                "Authenticated with: " + user.getEmail(),
                Toast.LENGTH_SHORT).show();
        ((UserClient) (getApplicationContext())).setUser(user);
        Log.d(TAG, "setCurrentUser: successfully set the user client.");
    }

/*
----------------------------- Firebase setup ---------------------------------
*/
    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: started.");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                            .build();
                    mDb.setFirestoreSettings(settings);

                    DocumentReference userRef = mDb.collection(getString(R.string.collection_users))
                            .document(user.getUid());

                    userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.isSuccessful()){
                                User user = task.getResult().toObject(User.class);
                                System.out.println(user);
                                if(user == null) // TODO maybe add another check for unique username
                                {
                                    getUserName();
                                }
                                else {
                                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUser_id());
                                    setCurrentUser(user);
                                    updateToken();
                                    startMainActivity();
                                }
                            }
                        }
                    });
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    startAuthenticationActivity();
                }
            }
        };
    }

/*
----------------------------- Username setup ---------------------------------
*/
    private void getUserName(){
        setContentView(R.layout.activity_username);
        mUsername = findViewById(R.id.input_username);
        findViewById(R.id.btn_go).setOnClickListener(this);
        hideSoftKeyboard();
    }

    @Override
    public void onClick(View v) {
        showLoading();
        if(v.getId() == R.id.btn_go) {
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
                    checkExisting(userName);
            }
            hideLoading();
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
                            User user = registerNewUser(userName);
                            setCurrentUser(user);
                            updateToken();
                            startMainActivity();
                        }
                    }
                }
            }
        });
    }
}