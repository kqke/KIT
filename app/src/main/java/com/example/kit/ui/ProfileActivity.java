package com.example.kit.ui;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.User;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class ProfileActivity extends AppCompatActivity implements
        View.OnClickListener,
        IProfile
{

    private static final String TAG = "ProfileActivity";


    //widgets
    private CircleImageView mAvatarImage;
    private TextView mProfileName, mProfileStatus;
    private Button mProfileSendReqBtn, mDeclineBtn;
    private ProgressDialog mProgressDialog;

    //vars
    private ImageListFragment mImageListFragment;
    private String mCurrent_state;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mAvatarImage = findViewById(R.id.image_choose_avatar);
        mProfileName = (TextView) findViewById(R.id.profile_displayName);
        mProfileStatus = (TextView) findViewById(R.id.profile_status);
        mProfileSendReqBtn = (Button) findViewById(R.id.profile_send_req_btn);
        mDeclineBtn = (Button) findViewById(R.id.profile_decline_btn);

        mCurrent_state = "not_friends";

        findViewById(R.id.image_choose_avatar).setOnClickListener(this);
        findViewById(R.id.text_choose_avatar).setOnClickListener(this);

        retrieveProfileImage();

        mDeclineBtn.setVisibility(View.INVISIBLE);
        mDeclineBtn.setEnabled(false);

//        progressDialog();

//        listen();
    }

    private void retrieveProfileImage(){
        RequestOptions requestOptions = new RequestOptions()
                .error(R.drawable.cartman_cop)
                .placeholder(R.drawable.cartman_cop);

        int avatar = 0;
        try{
            avatar = Integer.parseInt(((UserClient)getApplicationContext()).getUser().getAvatar());
        }catch (NumberFormatException e){
            Log.e(TAG, "retrieveProfileImage: no avatar image. Setting default. " + e.getMessage() );
        }

        Glide.with(ProfileActivity.this)
                .setDefaultRequestOptions(requestOptions)
                .load(avatar)
                .into(mAvatarImage);
    }

    private void progressDialog(){
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading User Data");
        mProgressDialog.setMessage("Please wait while we load the user data.");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    @Override
    public void onClick(View v) {
        mImageListFragment = new ImageListFragment();
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_in_down, R.anim.slide_out_down, R.anim.slide_out_up)
                .replace(R.id.fragment_container, mImageListFragment, getString(R.string.fragment_image_list))
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case android.R.id.home:{
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onImageSelected(int resource) {

        // remove the image selector fragment
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.slide_in_up, R.anim.slide_in_down, R.anim.slide_out_down, R.anim.slide_out_up)
                .remove(mImageListFragment)
                .commit();

        // display the image
        RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.cartman_cop)
                .error(R.drawable.cartman_cop);

        Glide.with(this)
                .setDefaultRequestOptions(requestOptions)
                .load(resource)
                .into(mAvatarImage);

        // update the client and database
        User user = ((UserClient)getApplicationContext()).getUser();
        user.setAvatar(String.valueOf(resource));

        FirebaseFirestore.getInstance()
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .set(user);
    }

//    private void listen(){
//        mUsersDatabase.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//
//                String display_name = dataSnapshot.child("name").getValue().toString();
//                String status = dataSnapshot.child("status").getValue().toString();
//                String image = dataSnapshot.child("image").getValue().toString();
//
//                mProfileName.setText(display_name);
//                mProfileStatus.setText(status);
//
//                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.default_avatar).into(mProfileImage);
//
//                if(mCurrent_user.getUid().equals(user_id)){
//
//                    mDeclineBtn.setEnabled(false);
//                    mDeclineBtn.setVisibility(View.INVISIBLE);
//
//                    mProfileSendReqBtn.setEnabled(false);
//                    mProfileSendReqBtn.setVisibility(View.INVISIBLE);
//
//                }
//
//
//                //--------------- FRIENDS LIST / REQUEST FEATURE -----
//
//                mFriendReqDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//
//                        if(dataSnapshot.hasChild(user_id)){
//
//                            String req_type = dataSnapshot.child(user_id).child("request_type").getValue().toString();
//
//                            if(req_type.equals("received")){
//
//                                mCurrent_state = "req_received";
//                                mProfileSendReqBtn.setText("Accept Friend Request");
//
//                                mDeclineBtn.setVisibility(View.VISIBLE);
//                                mDeclineBtn.setEnabled(true);
//
//
//                            } else if(req_type.equals("sent")) {
//
//                                mCurrent_state = "req_sent";
//                                mProfileSendReqBtn.setText("Cancel Friend Request");
//
//                                mDeclineBtn.setVisibility(View.INVISIBLE);
//                                mDeclineBtn.setEnabled(false);
//
//                            }
//
//                            mProgressDialog.dismiss();
//
//
//                        } else {
//
//
//                            mFriendDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
//                                @Override
//                                public void onDataChange(DataSnapshot dataSnapshot) {
//
//                                    if(dataSnapshot.hasChild(user_id)){
//
//                                        mCurrent_state = "friends";
//                                        mProfileSendReqBtn.setText("Unfriend this Person");
//
//                                        mDeclineBtn.setVisibility(View.INVISIBLE);
//                                        mDeclineBtn.setEnabled(false);
//
//                                    }
//
//                                    mProgressDialog.dismiss();
//
//                                }
//
//                                @Override
//                                public void onCancelled(DatabaseError databaseError) {
//
//                                    mProgressDialog.dismiss();
//
//                                }
//                            });
//
//                        }
//
//
//
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//
//                    }
//                });
//
//
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//
//            }
//        });
//
//
//        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                mProfileSendReqBtn.setEnabled(false);
//
//                // --------------- NOT FRIENDS STATE ------------
//
//                if(mCurrent_state.equals("not_friends")){
//
//
//                    DatabaseReference newNotificationref = mRootRef.child("notifications").child(user_id).push();
//                    String newNotificationId = newNotificationref.getKey();
//
//                    HashMap<String, String> notificationData = new HashMap<>();
//                    notificationData.put("from", mCurrent_user.getUid());
//                    notificationData.put("type", "request");
//
//                    Map requestMap = new HashMap();
//                    requestMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id + "/request_type", "sent");
//                    requestMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid() + "/request_type", "received");
//                    requestMap.put("notifications/" + user_id + "/" + newNotificationId, notificationData);
//
//                    mRootRef.updateChildren(requestMap, new DatabaseReference.CompletionListener() {
//                        @Override
//                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//                            if(databaseError != null){
//
//                                Toast.makeText(ProfileActivity.this, "There was some error in sending request", Toast.LENGTH_SHORT).show();
//
//                            } else {
//
//                                mCurrent_state = "req_sent";
//                                mProfileSendReqBtn.setText("Cancel Friend Request");
//
//                            }
//
//                            mProfileSendReqBtn.setEnabled(true);
//
//
//                        }
//                    });
//
//                }
//
//
//                // - -------------- CANCEL REQUEST STATE ------------
//
//                if(mCurrent_state.equals("req_sent")){
//
//                    mFriendReqDatabase.child(mCurrent_user.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void aVoid) {
//
//                            mFriendReqDatabase.child(user_id).child(mCurrent_user.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
//                                @Override
//                                public void onSuccess(Void aVoid) {
//
//
//                                    mProfileSendReqBtn.setEnabled(true);
//                                    mCurrent_state = "not_friends";
//                                    mProfileSendReqBtn.setText("Send Friend Request");
//
//                                    mDeclineBtn.setVisibility(View.INVISIBLE);
//                                    mDeclineBtn.setEnabled(false);
//
//
//                                }
//                            });
//
//                        }
//                    });
//
//                }
//
//
//                // ------------ REQ RECEIVED STATE ----------
//
//                if(mCurrent_state.equals("req_received")){
//
//                    final String currentDate = DateFormat.getDateTimeInstance().format(new Date());
//
//                    Map friendsMap = new HashMap();
//                    friendsMap.put("Friends/" + mCurrent_user.getUid() + "/" + user_id + "/date", currentDate);
//                    friendsMap.put("Friends/" + user_id + "/"  + mCurrent_user.getUid() + "/date", currentDate);
//
//
//                    friendsMap.put("Friend_req/" + mCurrent_user.getUid() + "/" + user_id, null);
//                    friendsMap.put("Friend_req/" + user_id + "/" + mCurrent_user.getUid(), null);
//
//
//                    mRootRef.updateChildren(friendsMap, new DatabaseReference.CompletionListener() {
//                        @Override
//                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//
//                            if(databaseError == null){
//
//                                mProfileSendReqBtn.setEnabled(true);
//                                mCurrent_state = "friends";
//                                mProfileSendReqBtn.setText("Unfriend this Person");
//
//                                mDeclineBtn.setVisibility(View.INVISIBLE);
//                                mDeclineBtn.setEnabled(false);
//
//                            } else {
//
//                                String error = databaseError.getMessage();
//
//                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
//
//
//                            }
//
//                        }
//                    });
//
//                }
//
//
//                // ------------ UNFRIENDS ---------
//
//                if(mCurrent_state.equals("friends")){
//
//                    Map unfriendMap = new HashMap();
//                    unfriendMap.put("Friends/" + mCurrent_user.getUid() + "/" + user_id, null);
//                    unfriendMap.put("Friends/" + user_id + "/" + mCurrent_user.getUid(), null);
//
//                    mRootRef.updateChildren(unfriendMap, new DatabaseReference.CompletionListener() {
//                        @Override
//                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//
//
//                            if(databaseError == null){
//
//                                mCurrent_state = "not_friends";
//                                mProfileSendReqBtn.setText("Send Friend Request");
//
//                                mDeclineBtn.setVisibility(View.INVISIBLE);
//                                mDeclineBtn.setEnabled(false);
//
//                            } else {
//
//                                String error = databaseError.getMessage();
//
//                                Toast.makeText(ProfileActivity.this, error, Toast.LENGTH_SHORT).show();
//
//
//                            }
//
//                            mProfileSendReqBtn.setEnabled(true);
//
//                        }
//                    });
//
//                }
//
//
//            }
//        });
//
//
//    }
//    }

}
