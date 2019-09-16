package com.example.kit.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.User;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;

import static android.text.TextUtils.isEmpty;

public class LoginActivity extends AppCompatActivity
//        implements
//        View.OnClickListener
{

    private static final String TAG = "LoginActivity";

//    //Firebase
//    private FirebaseAuth.AuthStateListener mAuthListener;
//
//    // widgets
//    private EditText mEmail, mPassword;
//    private ProgressBar mProgressBar;
//
//    Button signInButton;
    private FirebaseAuth mAuth;
    public static final int RC_SIGN_IN = 900;
    protected ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null)
        {
            startAuthenticationActivity();
        }

        startMainActivity();
//        signInButton = (Button) findViewById(R.id.button);
//        mEmail = findViewById(R.id.email);
//        mPassword = findViewById(R.id.password);
//        mProgressBar = findViewById(R.id.progressBar);
//
//        setupFirebaseAuth();
//        findViewById(R.id.email_sign_in_button).setOnClickListener(this);
//        findViewById(R.id.link_register).setOnClickListener(this);
//
//        hideSoftKeyboard();
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
                startMainActivity();
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

//    public void login_click (View v) {
//        startAuthenticationActivity();
//    }


//    @Override
//    protected void onResume() {
//        super.onResume();
//        authenticateWithCachedToken();
//    }
//
//    protected void authenticateWithCachedToken () {
//        signInButton.setEnabled(false);
//
//        ChatSDK.auth().authenticate()
//                .observeOn(AndroidSchedulers.mainThread())
//                .doFinally(() -> {
//                    signInButton.setEnabled(true);
//                })
//                .subscribe(() -> {
//                    ChatSDK.ui().startMainActivity(MainActivity.this);
//                }, throwable -> {
//                    // Setup failed
//                });
//    }


//    private void showDialog(){
//        mProgressBar.setVisibility(View.VISIBLE);
//
//    }
//
//    private void hideDialog(){
//        if(mProgressBar.getVisibility() == View.VISIBLE){
//            mProgressBar.setVisibility(View.INVISIBLE);
//        }
//    }
//
//    private void hideSoftKeyboard(){
//        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//    }

    /*
        ----------------------------- Firebase setup ---------------------------------
     */
//    private void setupFirebaseAuth(){
//        Log.d(TAG, "setupFirebaseAuth: started.");
//
//        mAuthListener = new FirebaseAuth.AuthStateListener() {
//            @Override
//            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
//                FirebaseUser user = firebaseAuth.getCurrentUser();
//                if (user != null) {
//                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
//                    Toast.makeText(LoginActivity.this, "Authenticated with: " + user.getEmail(), Toast.LENGTH_SHORT).show();
//
//                    FirebaseFirestore db = FirebaseFirestore.getInstance();
//                    FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
//                            .setTimestampsInSnapshotsEnabled(true)
//                            .build();
//                    db.setFirestoreSettings(settings);
//
//                    DocumentReference userRef = db.collection(getString(R.string.collection_users))
//                            .document(user.getUid());
//
//                    userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                        @Override
//                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                            if(task.isSuccessful()){
//                                Log.d(TAG, "onComplete: successfully set the user client.");
//                                User user = task.getResult().toObject(User.class);
//                                System.out.println("yadadada");
//                                System.out.println(user);
//                                ((UserClient)(getApplicationContext())).setUser(user);
//                            }
//                        }
//                    });
//
//                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                    startActivity(intent);
//                    finish();
//
//                } else {
//                    // User is signed out
//                    Log.d(TAG, "onAuthStateChanged:signed_out");
//                }
//                // ...
//            }
//        };
//    }
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
//    }
//
//    @Override
//    public void onStop() {
//        super.onStop();
//        if (mAuthListener != null) {
//            FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
//        }
//    }
//
//    private void signIn(){
//        //check if the fields are filled out
//        if(!isEmpty(mEmail.getText().toString())
//                && !isEmpty(mPassword.getText().toString())){
//            Log.d(TAG, "onClick: attempting to authenticate.");
//
//            showDialog();
//
//            FirebaseAuth.getInstance().signInWithEmailAndPassword(mEmail.getText().toString(),
//                    mPassword.getText().toString())
//                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
//                        @Override
//                        public void onComplete(@NonNull Task<AuthResult> task) {
//
//                            hideDialog();
//
//                        }
//                    }).addOnFailureListener(new OnFailureListener() {
//                @Override
//                public void onFailure(@NonNull Exception e) {
//                    Toast.makeText(LoginActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
//                    hideDialog();
//                }
//            });
//        }else{
//            Toast.makeText(LoginActivity.this, "You didn't fill in all the fields.", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    @Override
//    public void onClick(View view) {
//        switch (view.getId()){
//            case R.id.link_register:{
//                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
//                startActivity(intent);
//                break;
//            }
//
//            case R.id.email_sign_in_button:{
//               signIn();
//               break;
//            }
//        }
//    }
}