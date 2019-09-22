package com.example.kit.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import com.example.kit.R;
import com.example.kit.models.User;
import com.example.kit.models.UserLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import static com.example.kit.Constants.ERROR_DIALOG_REQUEST;
import static com.example.kit.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.example.kit.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

public abstract class DBGeoFragment extends Fragment {

    //Tag
    private static final String TAG = "DBGeoFragment";

    //Activity
    protected FragmentActivity mActivity;

    //Firebase
    protected FirebaseFirestore mDb;

    //Location
    private static boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationClient;
    protected UserLocation mUserLocation;

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public DBGeoFragment(){
        mDb = FirebaseFirestore.getInstance();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            mActivity = (AppCompatActivity) context;
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mActivity);
        }
    }





    //    private void buildNewChatroom(String chatroomName){
//
//        final Chatroom chatroom = new Chatroom();
////        chatroom.setTitle(chatroomName);
//
////        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
////                .setTimestampsInSnapshotsEnabled(true)
////                .build();
////        mDb.setFirestoreSettings(settings);
//
//        DocumentReference newChatroomRef = mDb
//                .collection(getString(R.string.collection_chatrooms))
//                .document();
//
//        chatroom.setChatroom_id(newChatroomRef.getId());
//
//        newChatroomRef.set(chatroom).addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                hideDialog();
//
//                if(task.isSuccessful()){
//                    navChatroomActivity(chatroom);
//                }else{
//                    View parentLayout = findViewById(android.R.id.content);
//                    Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }

//    private void newChatroomDialog(){
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Enter a chatroom name");
//
//        final EditText input = new EditText(this);
//        input.setInputType(InputType.TYPE_CLASS_TEXT);
//        builder.setView(input);
//
//        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                if(!input.getText().toString().equals("")){
//                    buildNewChatroom(input.getText().toString());
//                }
//                else {
//                    Toast.makeText(MainActivity.this, "Enter a chatroom name", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
//
//        builder.show();
//    }



}
