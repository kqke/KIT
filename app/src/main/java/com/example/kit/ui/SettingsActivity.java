package com.example.kit.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;
import de.hdodenhof.circleimageview.CircleImageView;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.example.kit.Constants.MY_PREFERENCES;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    //Tag
    private static final String TAG = "SettingsActivity";

    //User
    User mUser;

    //widgets
    private CircleImageView mAvatarImage;
    private TextView mUserName, mProfileStatus, mEditAvatar;
    private Button mSingOut;
    private ImageView mEditBtn, mEditDoneBtn;
    private EditText mEditStatus;

    //Avatar
    Intent CamIntent;
    File file;
    Uri uri;
    StorageReference ProfileImagesRef = FirebaseStorage.getInstance().getReference().child("profile images");

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = ((UserClient)getApplicationContext()).getUser();
        init();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener{

        SwitchPreferenceCompat incognito;
        SwitchPreferenceCompat proximity;
        SwitchPreferenceCompat notification;
        SeekBarPreference proximityRange;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            SharedPreferences sp = this.getActivity().getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
            incognito = findPreference(Constants.INCOGNITO);
            incognito.setOnPreferenceChangeListener(this);
            incognito.setChecked(sp.getBoolean(Constants.INCOGNITO, sp.getBoolean(Constants.INCOGNITO, false)));
            proximity = findPreference(Constants.PROXIMITY);
            proximity.setOnPreferenceChangeListener(this);
            proximity.setChecked(sp.getBoolean(Constants.PROXIMITY, sp.getBoolean(Constants.PROXIMITY, true)));
            notification = findPreference(Constants.NOTIFICATIONS);
            notification.setOnPreferenceChangeListener(this);
            notification.setChecked(sp.getBoolean(Constants.NOTIFICATIONS, sp.getBoolean(Constants.NOTIFICATIONS, true)));
            proximityRange = findPreference(Constants.PROXIMITY_RANGE);
            proximityRange.setOnPreferenceChangeListener(this);
            proximityRange.setValue(sp.getInt(Constants.PROXIMITY_RANGE, 10000));
            final int progress = Integer.valueOf(String.valueOf(proximityRange.getValue()));
            proximityRange.setSummary(String.format("set to: %dm", progress));
            proximityRange.setShowSeekBarValue(true);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            SharedPreferences sp = this.getActivity().getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
            switch (preference.getKey()){
                case Constants.INCOGNITO:{
                    boolean switched = ((SwitchPreferenceCompat) preference).isChecked();
                    if(switched){
                        sp.edit().putBoolean(Constants.INCOGNITO, false).apply();
                        incognito.setChecked(false);
                    }
                    else{
                        sp.edit().putBoolean(Constants.INCOGNITO, true).apply();
                        incognito.setChecked(true);
                    }
                    return true;
                }

                case Constants.PROXIMITY:{
                    boolean switched = ((SwitchPreferenceCompat) preference).isChecked();
                    if(switched){
                        sp.edit().putBoolean(Constants.PROXIMITY, false).apply();
                        proximity.setChecked(true);
                    }
                    else{
                        sp.edit().putBoolean(Constants.PROXIMITY, true).apply();
                        proximity.setChecked(false);
                    }
                    return true;
                }

                case Constants.NOTIFICATIONS:{
                    boolean switched = ((SwitchPreferenceCompat) preference).isChecked();
                    final User user = ((UserClient) (getActivity().getApplicationContext())).getUser();
                    User nUser = new User(user.getEmail(), user.getUser_id(), user.getUsername(), user.getAvatar(), user.getToken(),
                            user.getStatus());
                    if(switched){
                        sp.edit().putBoolean(Constants.NOTIFICATIONS, false).apply();
                        nUser.setNotifications(false);
                        notification.setChecked(true);
                    }
                    else{
                        sp.edit().putBoolean(Constants.NOTIFICATIONS, true).apply();
                        nUser.setNotifications(true);
                        notification.setChecked(false);
                    }
                    FirebaseFirestore.getInstance().collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).set(nUser);
                    return true;
                }

                case Constants.PROXIMITY_RANGE:{
                    final int progress = Integer.valueOf(String.valueOf(newValue));
                    proximityRange.setSummary(String.format("set to: %dm", progress));
                    sp.edit().putInt(Constants.PROXIMITY_RANGE, (int)newValue).apply();
                    proximityRange.setValue((int)newValue);
                    return true;
                }


            }
//            if(preference.getKey().equals("incognito")){
//                boolean switched = ((SwitchPreferenceCompat) preference).isChecked();
//                SharedPreferences sp = this.getActivity().getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
//                if(switched){
//                    sp.edit().putBoolean("incognito", false).apply();
//                    incognito.setChecked(false);
//                }
//                else{
//                    sp.edit().putBoolean("incognito", true).apply();
//                    incognito.setChecked(true);
//                }
//                return true;
//            }
            return false;
        }
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void init(){
        setContentView(R.layout.activity_settings);
        mAvatarImage = findViewById(R.id.avatar);
        mUserName = findViewById(R.id.profile_userName);
        mProfileStatus = findViewById(R.id.profile_status);
        mSingOut = findViewById(R.id.sign_out);
        mEditAvatar = findViewById(R.id.edit_image_btn);
        mEditBtn = findViewById(R.id.edit_btn);
        mEditDoneBtn = findViewById(R.id.edit_done_btn);
        initView();
    }

    private void initView(){
        mUser = ((UserClient)getApplicationContext()).getUser();
        mUserName.setText(mUser.getUsername());
        mProfileStatus.setText(mUser.getStatus());
        mEditAvatar.setOnClickListener(this);
        mAvatarImage.setOnClickListener(this);
        mProfileStatus.setOnClickListener(this);
        mSingOut.setOnClickListener(this);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        initEditText();
        retrieveProfileImage();
    }

    private void initEditText(){
        mEditStatus = findViewById(R.id.profile_editStatus);
        mEditStatus.setVisibility(View.GONE);
        mEditStatus.setText(mUser.getStatus());
        mEditBtn = findViewById(R.id.edit_btn);
        mEditBtn.setVisibility(VISIBLE);
        mEditBtn.setOnClickListener(this);
        mEditStatus.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    // Perform action on key press
                    doneEditStatus();
                    return true;
                }
                return false;
            }
        });
        mEditDoneBtn = findViewById(R.id.edit_done_btn);
        mEditDoneBtn.setOnClickListener(this);
    }


    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.edit_image_btn:
            case R.id.avatar:{
                showAlertDialogButtonClicked();
                break;
            }
            case R.id.edit_btn:
            case R.id.profile_status:{
                editStatus();
                break;
            }
            case R.id.edit_done_btn:{
                doneEditStatus();
                break;
            }
            case R.id.sign_out:{
                signOut();
                break;
            }
        }
    }

    private void editStatus(){
        mEditStatus.setVisibility(View.VISIBLE);
        mProfileStatus.setVisibility(GONE);
        mEditDoneBtn.setVisibility(View.VISIBLE);
        mEditBtn.setVisibility(GONE);
    }

    private void doneEditStatus(){
        mEditStatus.setVisibility(GONE);
        mProfileStatus.setVisibility(VISIBLE);
        mProfileStatus.setText(mEditStatus.getText().toString());
        mEditDoneBtn.setVisibility(GONE);
        mEditBtn.setVisibility(VISIBLE);
        setNewStatus(mEditStatus.getText().toString());
    }

    private void setNewStatus(String status){
        mUser.setStatus(status);
        DocumentReference userRef = FirebaseFirestore.getInstance()
                .collection(getString(R.string.collection_users))
                .document(mUser.getUser_id());
        userRef.update("status", status);
    }

    /*
    ----------------------------- nav ---------------------------------
    */

    private void signOut(){
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /*
    ----------------------------- Get Image ---------------------------------
    */

    public void showAlertDialogButtonClicked() {

        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Notice");
        builder.setMessage("CHOOSE ONE");

        // add the buttons

        builder.setPositiveButton("Gallery", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getImageFromGallery();
            }
        });
        builder.setNeutralButton("Camera", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                getImageFromCamera();
            }
        });

        builder.setNegativeButton("Cancel", null);

        // create and show the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void getImageFromGallery(){
        // option #1:
//        GalIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(Intent.createChooser(GalIntent, "Choose Image From Gallery"), 2);

        // option #2:
        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1).start(this);
        // option #2 from fragment:
        // CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1).start(getContext(), this);
    }

    private void getImageFromCamera(){
        CamIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = new File(Environment.getExternalStorageDirectory(), "file" + System.currentTimeMillis() + ".jpg");
        uri = Uri.fromFile(file);
        CamIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        CamIntent.putExtra("return-data", true);
        startActivityForResult(CamIntent, 0);

    }

    private void retrieveProfileImage(){
        RequestOptions requestOptions = new RequestOptions()
                .error(R.drawable.cartman_cop)
                .placeholder(R.drawable.cartman_cop);
        String avatar = "";
        try{
            avatar = (((UserClient)getApplicationContext()).getUser().getAvatar());
        }catch (NumberFormatException e){
            Log.e(TAG, "retrieveProfileImage: no avatar image. Setting default. " + e.getMessage() );
        }

        Glide.with(this)
                .setDefaultRequestOptions(requestOptions)
                .load(avatar)
                .into((CircleImageView)findViewById(R.id.avatar));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            imageCropFunction();
        } else if (requestCode == 2){
            if (data != null) {
                uri = data.getData();
                imageCropFunction();
            }
        } else if (requestCode == 1) {
            Bundle bundle = data.getExtras();
            Bitmap bitmap = bundle.getParcelable("data");

            // set image i.e: imageview.setImageBitmap(bitmap)

            ((CircleImageView)findViewById(R.id.avatar)).setImageBitmap(bitmap);

        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();

                // saves the image
                final StorageReference filePath = ProfileImagesRef.child(FirebaseAuth.getInstance().getUid() + ".jpg");
                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()){
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    // sets the avatar
                                    HashMap<String, String> avatar = new HashMap<>();
                                    avatar.put("avatar", uri.toString());
                                    FirebaseFirestore.getInstance().collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).update("avatar", uri.toString());
                                    retrieveProfileImage();
                                }
                            });
                        }
                    }
                });
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void imageCropFunction(){
        // option #1:
//        try {
//            CropIntent = new Intent("com.android.action.CROP");
//            CropIntent.setDataAndType(uri, "image/*");
//            CropIntent.putExtra("crop", true);
//            CropIntent.putExtra("outputX", 180);
//            CropIntent.putExtra("outputY", 180);
//            CropIntent.putExtra("aspectX", 4);
//            CropIntent.putExtra("aspectY", 4);
//            CropIntent.putExtra("scaleUpIfNeeded", true);
//            CropIntent.putExtra("return-data", true);
//            startActivityForResult(CropIntent, 1);
//
//        }
//        catch (ActivityNotFoundException e){
//
//        }

        // option #2:
        CropImage.activity(uri).setAspectRatio(1, 1).start(this);
        // option #2 from fragment:
        // CropImage.activity(uri).setAspectRatio(1, 1).start(this);

    }
}