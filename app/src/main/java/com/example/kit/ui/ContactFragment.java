package com.example.kit.ui;

import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import de.hdodenhof.circleimageview.CircleImageView;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.Contact;
import com.example.kit.models.User;
import com.example.kit.models.UserLocation;
import com.example.kit.util.FCM;
import com.example.kit.util.RequestHandler;
import com.example.kit.util.UsernameValidator;
import com.example.kit.util.ViewWeightAnimationWrapper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import static android.view.View.VISIBLE;

import static com.example.kit.Constants.CONTACT;
import static com.example.kit.Constants.CONTACT_STATE;
import static com.example.kit.Constants.FRIENDS;
import static com.example.kit.Constants.MY_REQUEST_PENDING;
import static com.example.kit.Constants.NOT_FRIENDS;
import static com.example.kit.Constants.THEIR_REQUEST_PENDING;

public class ContactFragment extends MapFragment implements
        View.OnClickListener, ContactDialogFragment.OnInputSelected, RequestsDialogFragment.OnInputSelected{

    //Tag
    private static final String TAG = "ConatactFragment";

    //widgets
    private CircleImageView mAvatarImage;
    private TextView mProfileName, mUserName, mProfileStatus;
    private Button mSendReqBtn, mAcceptBtn,
            mDeclineBtn, mDeleteReqBtn, mDeleteBtn;
    private LinearLayout mTheirReqPending;
    private ImageView mEditBtn, mEditDoneBtn;
    private EditText mEditDisplayName;

    //Contact
    private Contact mContact;

    //vars
    private String mCurrent_state;

    private ContactCallback recreate;

    private DBGeoFragment contactFragment;

    RelativeLayout mContactInfo;

    //map
    RelativeLayout mMapContainer;
    ImageButton expandBut;
    ImageButton resetBut;
    private static final int MAP_LAYOUT_STATE_CONTRACTED = 0;
    private static final int MAP_LAYOUT_STATE_EXPANDED = 1;
    private int mMapLayoutState = 0;



    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public ContactFragment() {
        // Required empty public constructor
    }

    public static ContactFragment newInstance() {
        return new ContactFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mContact = args.getParcelable(CONTACT);
        mCurrent_state = args.getString(CONTACT_STATE);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        contactFragment = this;
        recreate = (ContactCallback)context;
        if (mContact == null && getArguments() != null) {
            mContact = getArguments().getParcelable(CONTACT);
        }
        id2ContactsLocations = recreate.getContactLocations(mContact.getCid());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_contact, container, false);
        mMapView = v.findViewById(R.id.contact_map);
        initView(v);
        initGoogleMap(savedInstanceState);
        return v;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {

    }


    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(View v){
        initEditText(v);
        v.findViewById(R.id.contact_lin_lay).setBackgroundColor(getResources().getColor(R.color.White));
        mContactInfo = v.findViewById(R.id.contact_info_container);
        mMapContainer = v.findViewById(R.id.contact_map_container);
        expandBut = v.findViewById(R.id.c_btn_full_screen_map);
        resetBut = v.findViewById(R.id.c_btn_reset_map);
        expandBut.setOnClickListener(this);
        resetBut.setOnClickListener(this);
        mAvatarImage = v.findViewById(R.id.image_choose_avatar);
        mProfileName = v.findViewById(R.id.profile_displayName);
        mProfileName.setText(mContact.getName());
        mProfileName.setOnClickListener(this);
        mUserName = v.findViewById(R.id.profile_userName);
        mProfileStatus = v.findViewById(R.id.profile_status);
        mSendReqBtn = v.findViewById(R.id.profile_send_req_btn);
        mTheirReqPending = v.findViewById(R.id.request_btns);
        mAcceptBtn = v.findViewById(R.id.profile_accept_req_btn);
        mDeclineBtn = v.findViewById(R.id.profile_decline_btn);
        mDeleteReqBtn = v.findViewById(R.id.profile_delete_request_btn);
        mDeleteBtn = v.findViewById(R.id.profile_delete_btn);
        if(!mCurrent_state.equals(FRIENDS)){
            setNonFriend();
        }
        ((ViewGroup) v.findViewById(R.id.contact_lin_lay)).getLayoutTransition()
                .setDuration(800);
        ((ViewGroup) v.findViewById(R.id.contact_lin_lay)).getLayoutTransition()
                .enableTransitionType(LayoutTransition.CHANGING);
        initState();
        retrieveProfileImage();
    }

    private void setFriend(){
        mUserName.setText(mContact.getUsername());
        if(mContact.getStatus() != null) {
            mProfileStatus.setText(mContact.getStatus());
        }
        else {
            mProfileStatus.setVisibility(View.INVISIBLE);
        }
        mDeleteBtn.setVisibility(VISIBLE);
        mDeleteBtn.setClickable(true);
        mDeleteBtn.setOnClickListener(this);
        mAcceptBtn.setVisibility(View.INVISIBLE);
        mAcceptBtn.setClickable(false);
        mDeclineBtn.setVisibility(View.INVISIBLE);
        mDeclineBtn.setClickable(false);
        mMapView.setVisibility(View.VISIBLE);
        mMapView.setClickable(true);
        expandBut.setVisibility(View.VISIBLE);
        expandBut.setClickable(true);
        resetBut.setVisibility(View.VISIBLE);
        resetBut.setClickable(true);


    }

    private void setNonFriend(){
        mProfileStatus.setVisibility(View.INVISIBLE);
        mUserName.setVisibility(View.GONE);
        mEditBtn.setVisibility(View.GONE);
        mDeleteBtn.setVisibility(View.INVISIBLE);
        mDeleteBtn.setClickable(false);
        mMapView.setVisibility(View.INVISIBLE);
        mMapView.setClickable(false);
        expandBut.setVisibility(View.INVISIBLE);
        expandBut.setClickable(false);
        resetBut.setVisibility(View.INVISIBLE);
        resetBut.setClickable(false);

    }

    private void initEditText(View v){
        if(mCurrent_state.equals(FRIENDS)){
            mEditDisplayName = v.findViewById(R.id.profile_editDisplayName);
            mEditDisplayName.setVisibility(View.GONE);
            mEditDisplayName.setText(mContact.getName());
            mEditBtn = v.findViewById(R.id.edit_btn);
            mEditBtn.setVisibility(VISIBLE);
            mEditBtn.setOnClickListener(this);
            mEditDisplayName.setOnKeyListener(new View.OnKeyListener() {
                  public boolean onKey(View v, int keyCode, KeyEvent event) {
                      // If the event is a key-down event on the "enter" button
                      if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                              (keyCode == KeyEvent.KEYCODE_ENTER)) {
                          // Perform action on key press
                          doneEditDisplayName();
                          return true;
                      }
                      return false;
                  }
              });
            mEditDoneBtn = v.findViewById(R.id.edit_done_btn);
        }
        else {
            mEditBtn = v.findViewById(R.id.edit_btn);
        }
    }

    private void setNotFirends(){
        mSendReqBtn.setClickable(true);
        mSendReqBtn.setVisibility(VISIBLE);
        mSendReqBtn.setOnClickListener(this);
        mDeleteReqBtn.setVisibility(View.INVISIBLE);
        mDeleteReqBtn.setClickable(false);

    }

    private void setMyRequestPending(){
        mDeleteReqBtn.setClickable(true);
        mDeleteReqBtn.setVisibility(VISIBLE);
        mDeleteReqBtn.setOnClickListener(this);
        mSendReqBtn.setVisibility(View.INVISIBLE);
        mSendReqBtn.setClickable(false);
    }

    private void setTheirReqPending(){
        mTheirReqPending.setVisibility(VISIBLE);
        mAcceptBtn.setVisibility(VISIBLE);
        mDeclineBtn.setVisibility(VISIBLE);
        mAcceptBtn.setOnClickListener(this);
        mDeclineBtn.setOnClickListener(this);
    }

    private void initState(){
        switch(mCurrent_state){
            case NOT_FRIENDS:{
                setNotFirends();
                break;
            }
            case FRIENDS:{
                setFriend();
                break;
            }
            case MY_REQUEST_PENDING:{
                setMyRequestPending();
                break;
            }
            case THEIR_REQUEST_PENDING:{
                setTheirReqPending();
                break;
            }
        }
    }

    private void retrieveProfileImage(){
        RequestOptions requestOptions = new RequestOptions()
                .error(R.drawable.cartman_cop)
                .placeholder(R.drawable.cartman_cop);
        Glide.with(this)
                .setDefaultRequestOptions(requestOptions)
                .load(mContact.getAvatar())
                .into(mAvatarImage);
    }

    private void editDisplayName(){
        mEditDisplayName.setVisibility(VISIBLE);
        mProfileName.setVisibility(View.GONE);
        mEditBtn.setVisibility(View.GONE);
        mEditDoneBtn.setVisibility(VISIBLE);
        mEditDoneBtn.setOnClickListener(this);
    }

    private void doneEditDisplayName(){
        UsernameValidator validator = new UsernameValidator();
        if(validator.validate(mEditDisplayName.getText().toString())){
            mEditDisplayName.setVisibility(View.GONE);
            mProfileName.setVisibility(VISIBLE);
            mProfileName.setText(mEditDisplayName.getText().toString());
            mEditBtn.setVisibility(VISIBLE);
            mEditDoneBtn.setVisibility(View.GONE);
            changeDisplayName(mEditDisplayName.getText().toString());
        }
        else{
            Log.d(TAG, "edit done: invalid display name");
            Toast.makeText(mActivity,
                    "Enter a valid display name",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void getDisplayName(){
        final View dialogView = View.inflate(mActivity, R.layout.dialog_display_name, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(mActivity).create();
        dialogView.findViewById(R.id.go_profile_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String displayName = ((EditText)dialogView.findViewById(R.id.dialog_input)).getText().toString();
                UsernameValidator validator = new UsernameValidator();
                if(validator.validate(displayName)){
                    addContact(displayName, mContact);
                    alertDialog.dismiss();
                }
                else{
                    Toast.makeText(mActivity,
                            "Enter a valid display name",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        alertDialog.setView(dialogView);
        alertDialog.show();
    }

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.profile_displayName:
            case R.id.edit_btn:{
                editDisplayName();
                break;
            }
            case R.id.edit_done_btn:{
                doneEditDisplayName();
                break;
            }
            case R.id.profile_send_req_btn:{
                sendRequest();
                break;
            }
            case R.id.profile_accept_req_btn:{
                acceptRequest();
                break;
            }
            case R.id.profile_decline_btn:{
                declineRequest();
                break;
            }
            case R.id.profile_delete_request_btn:{
                deleteRequest();
                break;
            }
            case R.id.profile_delete_btn:{
                deleteContact();
                break;
            }
            case R.id.c_btn_full_screen_map:{
                if(mMapLayoutState == MAP_LAYOUT_STATE_CONTRACTED){
                    mMapLayoutState = MAP_LAYOUT_STATE_EXPANDED;
                    mContactInfo.setVisibility(View.GONE);
                }
                else if(mMapLayoutState == MAP_LAYOUT_STATE_EXPANDED){
                    mMapLayoutState = MAP_LAYOUT_STATE_CONTRACTED;
                    mContactInfo.setVisibility(VISIBLE);
                }
                break;
            }

            case R.id.c_btn_reset_map:{
                addMapMarkers();
                setCameraView();
                break;
            }
        }
    }

    /*
    ----------------------------- DB ---------------------------------
    */

    private void sendRequest(){
        getDisplayName();
    }

    private void acceptRequest(){
        RequestsDialogFragment requestDialog = new RequestsDialogFragment(Constants.GET_ACCEPT_REQUEST, mContact, getActivity(), this);
        requestDialog.setTargetFragment(ContactFragment.this, 1);
        requestDialog.show(getFragmentManager(), "RequestsDialogFragment");
    }

    private void declineRequest(){
        RequestsDialogFragment requestDialog = new RequestsDialogFragment(Constants.GET_REMOVE_REQUEST, mContact, getActivity(), this);
        requestDialog.setTargetFragment(ContactFragment.this, 1);
        requestDialog.show(getFragmentManager(), "RequestsDialogFragment");
    }

    private void deleteRequest(){
        ContactDialogFragment contactDialogFragment = new ContactDialogFragment(Constants.GET_REMOVE_REQUEST, mContact, getActivity(), this);
        contactDialogFragment.setTargetFragment(ContactFragment.this, 1);
        contactDialogFragment.show(getFragmentManager(), "RequestsDialogFragment");
    }

    private void deleteContact(){
        ContactDialogFragment contactDialogFragment = new ContactDialogFragment(Constants.GET_REMOVE_CONTACT, mContact, getActivity(), this);
        contactDialogFragment.setTargetFragment(ContactFragment.this, 1);
        contactDialogFragment.show(getFragmentManager(), "RequestsDialogFragment");

    }

    private void addContact(String display_name, final Contact contact) {
        setMyRequestPending();
        final User user = ((UserClient) (getActivity().getApplicationContext())).getUser();
        Contact newContact = new Contact(display_name, contact.getName(), contact.getAvatar(), contact.getCid(), contact.getStatus());
        newContact.setToken(contact.getToken());
        mDb.collection(getString(R.string.collection_users)).document(user.getUser_id()).collection(getString(R.string.collection_pending)).document(contact.getCid()).set(newContact).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "DocumentSnapshot successfully written!");
                DocumentReference contactRef =
                        mDb.collection(getString(R.string.collection_users)).document(contact.getCid()).collection(getString(R.string.collection_requests)).document(user.getUser_id());
                Contact userContact = new Contact(user.getUsername(), user.getEmail(), user.getAvatar(), user.getUser_id(),
                        user.getStatus());
                userContact.setToken(user.getToken());
                contactRef.set(userContact).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                FCM.send_FCM_Notification(contact.getToken(), "Friend Request", user.getUsername() + " has sent " +
                        "you a friend request");
//                recreate.initContactFragment(mContact.getCid(), MY_REQUEST_PENDING);


                    }
                });
                recreate.addContact(contact, MY_REQUEST_PENDING);
            }
        });
    }


    @Override
    public void removeContact(Contact contact) {
        setNonFriend();
        setNotFirends();
        String uid = FirebaseAuth.getInstance().getUid();
        mDb.collection(getString(R.string.collection_users)).document(contact.getCid()).collection(getString(R.string.collection_contacts)).document(uid).delete();
        mDb.collection(getString(R.string.collection_users)).document(uid).collection(getString(R.string.collection_contacts)).document(contact.getCid()).delete();
        recreate.removeContact(contact);


//        recreate.initContactFragment(mContact.getCid(), NOT_FRIENDS);
//        mDeleteBtn.setClickable(false);
//        mDeleteBtn.setVisibility(View.INVISIBLE);
    }

    @Override
    public void removeRequest(Contact contact) {
        setNotFirends();
        String uid = FirebaseAuth.getInstance().getUid();
        mDb.collection(getString(R.string.collection_users)).document(contact.getCid()).collection(getString(R.string.collection_requests)).document(uid).delete();
        mDb.collection(getString(R.string.collection_users)).document(uid).collection(getString(R.string.collection_pending)).document(contact.getCid()).delete();
        recreate.removeContact(contact);
//        recreate.initContactFragment(mContact.getCid(), NOT_FRIENDS);
//        mDeleteReqBtn.setClickable(false);
//        mDeleteReqBtn.setVisibility(View.INVISIBLE);
    }

    public void changeDisplayName(final String name){
        DocumentReference contactRef = FirebaseFirestore.getInstance()
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_contacts))
                .document(mContact.getCid());
        contactRef.update("name", name);
        final String first, second, chatroom_id;
        if (stringCompare(mContact.getCid(), FirebaseAuth.getInstance().getUid()) > 0) {
            first = mContact.getCid();
            second = FirebaseAuth.getInstance().getUid();
        } else {
            first = FirebaseAuth.getInstance().getUid();
            second = mContact.getCid();
        }
        chatroom_id = first + second;
        CollectionReference chatroomRef = FirebaseFirestore.getInstance()
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection("user_chatrooms");
        Query query = chatroomRef.whereEqualTo("chatroom_id", chatroom_id);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    if (task.getResult() != null) {
                        DocumentReference chatroomRef = FirebaseFirestore.getInstance()
                                .collection(getString(R.string.collection_users))
                                .document(FirebaseAuth.getInstance().getUid())
                                .collection("user_chatrooms")
                                .document(chatroom_id);
                        chatroomRef.update("display_name", name);
                    }
                }
            }
        });
    }


     /*
    ----------------------------- map ---------------------------------
    */

    private void expandMapAnimation(){
        ViewWeightAnimationWrapper mapAnimationWrapper = new ViewWeightAnimationWrapper(mMapContainer);
        ObjectAnimator mapAnimation = ObjectAnimator.ofFloat(mapAnimationWrapper,
                "weight",
                45,
                100);
        mapAnimation.setDuration(800);

        ViewWeightAnimationWrapper recyclerAnimationWrapper = new ViewWeightAnimationWrapper(mContactInfo);
        ObjectAnimator recyclerAnimation = ObjectAnimator.ofFloat(recyclerAnimationWrapper,
                "weight",
                55,
                0);
        recyclerAnimation.setDuration(800);

        recyclerAnimation.start();
        mapAnimation.start();

        Log.d(TAG, "expandMapAnimation: map weight ");
    }

    private void contractMapAnimation(){
        ViewWeightAnimationWrapper mapAnimationWrapper = new ViewWeightAnimationWrapper(mMapContainer);
        ObjectAnimator mapAnimation = ObjectAnimator.ofFloat(mapAnimationWrapper,
                "weight",
                100,
                45);
        mapAnimation.setDuration(800);

        ViewWeightAnimationWrapper recyclerAnimationWrapper = new ViewWeightAnimationWrapper(mContactInfo);
        ObjectAnimator recyclerAnimation = ObjectAnimator.ofFloat(recyclerAnimationWrapper,
                "weight",
                0,
                55);
        recyclerAnimation.setDuration(800);

        recyclerAnimation.start();
        mapAnimation.start();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        super.onMapReady(map);
        if (mMapLayoutState == MAP_LAYOUT_STATE_EXPANDED){
            expandMapAnimation();
        }
    }


    /*
    ----------------------------- utils ---------------------------------
    */

    private void hideSoftKeyboard(){
        final InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    public static int stringCompare(String str1, String str2)
    {
        int l1 = str1.length();
        int l2 = str2.length();
        int lmin = Math.min(l1, l2);

        for (int i = 0; i < lmin; i++) {
            int str1_ch = (int)str1.charAt(i);
            int str2_ch = (int)str2.charAt(i);
            if (str1_ch != str2_ch) {
                return str1_ch - str2_ch;
            }
        }
        // Edge case for strings like
        // String 1="Geeks" and String 2="Geeksforgeeks"
        if (l1 != l2) {
            return l1 - l2;
        }
        // If none of the above conditions is true,
        // it implies both the strings are equal
        else {
            return 0;
        }
    }

    @Override
    public void requestAccepted(String display_name, Contact contact) {
        setFriend();
        RequestHandler.handleRequest(contact, display_name, true);
        recreate.addContact(contact, FRIENDS);
    }

    @Override
    public void requestRemoved(Contact contact) {
        setNotFirends();
        mAcceptBtn.setVisibility(View.INVISIBLE);
        mDeclineBtn.setVisibility(View.INVISIBLE);
        mAcceptBtn.setClickable(false);
        mDeclineBtn.setClickable(false);
        RequestHandler.handleRequest(contact, "",false);
        recreate.removeContact(contact);
    }

    public interface ContactCallback{
        void initContactFragment(String id, String state);
        void removeContact(Contact contact);
        void addContact(Contact contact, String type);
        HashMap<String, UserLocation> getContactLocations(String contact_id);
    }
}
