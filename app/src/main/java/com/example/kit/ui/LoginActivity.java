package com.example.kit.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.User;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.example.kit.Constants.GET_USERNAME_REQUEST;

public class LoginActivity extends AppCompatActivity
{

    private static final String TAG = "LoginActivity";

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;

    private ProgressBar mProgressBar;

    private FirebaseAuth mAuth;
    public static final int RC_SIGN_IN = 900;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        setupFirebaseAuth();
        mProgressBar = findViewById(R.id.progressBar);
        showDialog();
    }

    public void startAuthenticationActivity () {

        ArrayList<AuthUI.IdpConfig> idps = new ArrayList<>();

        idps.add(new AuthUI.IdpConfig.EmailBuilder().build());
        idps.add(new AuthUI.IdpConfig.GoogleBuilder().build());
        idps.add(new AuthUI.IdpConfig.PhoneBuilder().build());

        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(idps)
                        .build(),
                RC_SIGN_IN);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // RC_SIGN_IN is the request code you passed into  startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                // Success
//                startMainActivity();
            }
            else {
                // Handle Error
            }
        }
        else if (requestCode == GET_USERNAME_REQUEST)
        {
            if (resultCode == RESULT_OK) {
                String userName = data.getData().toString();
                registerNewUser(userName);
            }
            else {
                // Handle Error
            }
        }
    }

    protected void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void showDialog(){
        mProgressBar.setVisibility(View.VISIBLE);

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
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    Toast.makeText(LoginActivity.this, "Authenticated with: " + user.getEmail(), Toast.LENGTH_SHORT).show();

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                            .setTimestampsInSnapshotsEnabled(true)
                            .build();
                    db.setFirestoreSettings(settings);

                    DocumentReference userRef = db.collection(getString(R.string.collection_users))
                            .document(user.getUid());

                    userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if(task.isSuccessful()){
                                Log.d(TAG, "onComplete: successfully set the user client.");
                                User user_ = task.getResult().toObject(User.class);
                                System.out.println("yadadada");
                                System.out.println(user_);
                                if(user_ == null)
                                {
                                    getUserName();
                                }
                                else {
                                    ((UserClient) (getApplicationContext())).setUser(user_);
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
                // ...
            }
        };
    }

    private void getUserName(){
        Intent intent = new Intent(LoginActivity.this, UsernameActivity.class);
        startActivityForResult(intent, GET_USERNAME_REQUEST);
    }


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

}