package com.example.kit.ui;

import android.os.Bundle;

import androidx.annotation.Nullable;
import de.hdodenhof.circleimageview.CircleImageView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.Contact;
import com.example.kit.util.UsernameValidator;
import com.google.firebase.auth.FirebaseAuth;

import static android.view.View.VISIBLE;
import static com.example.kit.Constants.CONTACT;
import static com.example.kit.Constants.CONTACT_STATE;
import static com.example.kit.Constants.FRIENDS;
import static com.example.kit.Constants.MY_REQUEST_PENDING;
import static com.example.kit.Constants.NOT_FRIENDS;
import static com.example.kit.Constants.THEIR_REQUEST_PENDING;

public class ContactFragment extends DBGeoFragment implements
        View.OnClickListener, ContactDialogFragment.OnInputSelected{

    //Tag
    private static final String TAG = "ConatactFragment";

    //widgets
    private CircleImageView mAvatarImage;
    private TextView mProfileName, mUserName, mProfileStatus;
    private Button mSendReqBtn, mAcceptBtn,
            mDeclineBtn, mDeleteReqBtn, mDeleteBtn;
    private ImageView mEditBtn, mEditDoneBtn;
    private EditText mEditDisplayName;

    //Contact
    private Contact mContact;

    //vars
    private String mCurrent_state;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_contact, container, false);
        initView(v);
        return v;
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(View v){
        mAvatarImage = v.findViewById(R.id.image_choose_avatar);
        mProfileName = v.findViewById(R.id.profile_displayName);
        mProfileName.setText(mContact.getName());
        mUserName = v.findViewById(R.id.profile_userName);
        if(mCurrent_state.equals(FRIENDS)){
            mUserName.setText(mContact.getUsername());
        }
        else{
            mUserName.setVisibility(View.GONE);
        }
        mProfileStatus = v.findViewById(R.id.profile_status);
        mSendReqBtn = v.findViewById(R.id.profile_send_req_btn);
        mAcceptBtn = v.findViewById(R.id.profile_accept_req_btn);
        mDeclineBtn = v.findViewById(R.id.profile_decline_btn);
        mDeleteReqBtn = v.findViewById(R.id.profile_delete_request_btn);
        mDeleteBtn = v.findViewById(R.id.profile_delete_btn);
        initEditText(v);
        initState();
        retrieveProfileImage();
    }

    private void initEditText(View v){
        if(mCurrent_state.equals(FRIENDS)){
            mEditDisplayName = v.findViewById(R.id.profile_editDisplayName);
            mEditDisplayName.setVisibility(View.GONE);
            mEditDisplayName.setHint(mContact.getName());
            mEditBtn = v.findViewById(R.id.edit_btn);
            mEditBtn.setVisibility(VISIBLE);
            mEditBtn.setOnClickListener(this);
            mEditDoneBtn = v.findViewById(R.id.edit_done_btn);
        }
    }

    private void initState(){
        switch(mCurrent_state){
            case NOT_FRIENDS:{
                mSendReqBtn.setVisibility(VISIBLE);
                mSendReqBtn.setOnClickListener(this);
                break;
            }
            case FRIENDS:{
                mDeleteBtn.setVisibility(VISIBLE);
                mDeleteBtn.setOnClickListener(this);
                break;
            }
            case MY_REQUEST_PENDING:{
                mDeleteReqBtn.setVisibility(VISIBLE);
                mDeleteReqBtn.setOnClickListener(this);
                break;
            }
            case THEIR_REQUEST_PENDING:{
                mAcceptBtn.setVisibility(VISIBLE);
                mDeclineBtn.setVisibility(VISIBLE);
                mAcceptBtn.setOnClickListener(this);
                mDeclineBtn.setOnClickListener(this);
                break;
            }
        }
    }

    private void retrieveProfileImage(){
        RequestOptions requestOptions = new RequestOptions()
                .error(R.drawable.cartman_cop)
                .placeholder(R.drawable.cartman_cop);
        int avatar = 0;
        try{
            avatar = Integer.parseInt(((UserClient)mActivity.getApplicationContext()).getUser().getAvatar());
        }catch (NumberFormatException e){
            Log.e(TAG, "retrieveProfileImage: no avatar image. Setting default. " + e.getMessage() );
        }

        Glide.with(mActivity)
                .setDefaultRequestOptions(requestOptions)
                .load(avatar)
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
            mEditDisplayName.setText("");
            mEditBtn.setVisibility(VISIBLE);
            mEditDoneBtn.setVisibility(View.GONE);
        }
        else{
            Log.d(TAG, "edit done: invalid display name");
            Toast.makeText(mActivity,
                    "Enter a valid display name",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onClick(View v) {
        switch(v.getId()){
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
        }
    }

    /*
    ----------------------------- DB ---------------------------------
    */

    private void sendRequest(){

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

    @Override
    public void removeContact(Contact contact) {
        String uid = FirebaseAuth.getInstance().getUid();
        mDb.collection(getString(R.string.collection_users)).document(contact.getCid()).collection(getString(R.string.collection_contacts)).document(uid).delete();
        mDb.collection(getString(R.string.collection_users)).document(uid).collection(getString(R.string.collection_contacts)).document(contact.getCid()).delete();
        mDeleteBtn.setClickable(false);
        mDeleteBtn.setVisibility(View.INVISIBLE);
    }

    @Override
    public void removeRequest(Contact contact) {
        String uid = FirebaseAuth.getInstance().getUid();
        mDb.collection(getString(R.string.collection_users)).document(contact.getCid()).collection(getString(R.string.collection_pending)).document(uid).delete();
        mDb.collection(getString(R.string.collection_users)).document(uid).collection(getString(R.string.collection_requests)).document(contact.getCid()).delete();
        mDeleteReqBtn.setClickable(false);
        mDeleteReqBtn.setVisibility(View.INVISIBLE);
    }
}
