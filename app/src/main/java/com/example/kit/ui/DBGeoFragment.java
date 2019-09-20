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
            mActivity = (FragmentActivity) context;
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mActivity);
        }
    }

    /*
    ----------------------------- DB ---------------------------------
    */

    protected void getUserDetails(){
        if(mUserLocation == null){
            mUserLocation = new UserLocation();
            DocumentReference userRef = mDb.collection(getString(R.string.collection_users))
                    .document(FirebaseAuth.getInstance().getUid());

            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        Log.d(TAG, "onComplete: successfully set the user client.");
                        User user = task.getResult().toObject(User.class);
                        mUserLocation.setUser(user);
                        getLastKnownLocation();
                    }
                }
            });
        }
        else{
            getLastKnownLocation();
        }
    }

    private void saveUserLocation(){
        if(mUserLocation != null){
            DocumentReference locationRef = mDb
                    .collection(getString(R.string.collection_user_locations))
                    .document(FirebaseAuth.getInstance().getUid());

            locationRef.set(mUserLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Log.d(TAG, "saveUserLocation: \ninserted user location into database." +
                                "\n latitude: " + mUserLocation.getGeo_point().getLatitude() +
                                "\n longitude: " + mUserLocation.getGeo_point().getLongitude());
                    }
                }
            });
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


    /*
    ----------------------------- Location ---------------------------------
    */

    protected boolean isLocationPermissionGranted(){
        return mLocationPermissionGranted;
    }

    protected void setLocationPermissionGranted(boolean permissionGranted){
        mLocationPermissionGranted = permissionGranted;
    }

    protected boolean getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(mActivity.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            getUserDetails();
            return true;
        } else {
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return false;
        }
    }

    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation: called.");

        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    Location location = task.getResult();
                    if (location != null) {
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        mUserLocation.setGeo_point(geoPoint);
                        mUserLocation.setTimestamp(null);
                        saveUserLocation();
                    }
                }
            }
        });

    }

    protected boolean checkMapServices(){
        return (isServicesOK() && isMapsEnabled());
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private boolean isMapsEnabled(){
        final LocationManager manager = (LocationManager)mActivity.getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mActivity);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }

        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(mActivity, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }

        else {
            Toast.makeText(mActivity, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
