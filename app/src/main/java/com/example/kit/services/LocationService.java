package com.example.kit.services;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.Contact;
import com.example.kit.models.User;
import com.example.kit.models.UserLocation;
import com.example.kit.util.FCM;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private FusedLocationProviderClient mFusedLocationClient;
    private final static long UPDATE_INTERVAL = 4 * 1000;  /* 4 secs */
    private final static long FASTEST_INTERVAL = 2000; /* 2 sec */
//    private static HashMap<String, UserLocation> CONTACT_LOCATIONS = new HashMap<>();
    private static HashMap<String, Contact> CONTACTS = new HashMap<>();
    private static Location location = new Location("");
    private static ListenerRegistration mContactEventListener;
    static String CHANNEL_ID = "my_channel_01";
    static String PROX_CHANNEL_ID = "proximity_channel_01";
    private NotificationChannel mNotificationChannel;
    private NotificationChannel mProxyNotificationChannel;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: CARLLLLLLLLL");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (Build.VERSION.SDK_INT >= 26) {
            Log.d(TAG, "onCreate: BARAK SERVICE");
            mNotificationChannel = new NotificationChannel(CHANNEL_ID,
                    "My Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationChannel.setSound(null, null);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(mNotificationChannel);

            mProxyNotificationChannel = new NotificationChannel(PROX_CHANNEL_ID,
                    "My Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(mProxyNotificationChannel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").setSound(null).build();
            getContacts();
            startForeground(1, notification);
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: called.");
        getLocation();
        return START_NOT_STICKY;
    }
    private void getLocation() {
// ---------------------------------- LocationRequest ------------------------------------
// Create the location request to start receiving updates
        LocationRequest mLocationRequestHighAccuracy = new LocationRequest();
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestHighAccuracy.setInterval(UPDATE_INTERVAL);
        mLocationRequestHighAccuracy.setFastestInterval(FASTEST_INTERVAL);
// new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "getLocation: stopping the location service.");
            stopSelf();
            return;
        }
        Log.d(TAG, "getLocation: getting location information.");
        mFusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        Log.d(TAG, "onLocationResult: got location result.");
                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            User user = ((UserClient)(getApplicationContext())).getUser();
                            GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            LocationService.location = location;
                            UserLocation userLocation = new UserLocation(user, geoPoint, null);
                            saveUserLocation(userLocation);
                        }
                    }
                },
                Looper.myLooper()); // Looper.myLooper tells this to repeat forever until thread is destroyed
    }
    private void saveUserLocation(final UserLocation userLocation){
        try{
            DocumentReference locationRef = FirebaseFirestore.getInstance()
                    .collection(getString(R.string.collection_user_locations))
                    .document(FirebaseAuth.getInstance().getUid());
            locationRef.set(userLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Log.d(TAG, "onComplete: \ninserted user location into database." +
                                "\n latitude: " + userLocation.getGeo_point().getLatitude() +
                                "\n longitude: " + userLocation.getGeo_point().getLongitude());
                        getContactLocations();
                        if (CONTACTS != null && CONTACTS.isEmpty()){
                            getContacts();
                        }

                    }
                }
            });
        }catch (NullPointerException e){
            Log.e(TAG, "saveUserLocation: User instance is null, stopping location service.");
            Log.e(TAG, "saveUserLocation: NullPointerException: "  + e.getMessage() );
            stopSelf();
        }
    }

    private void fetchContacts() {
        CollectionReference contactsCollection = FirebaseFirestore.getInstance()
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_contacts));
        ListenerRegistration mContactEventListener = contactsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        Contact contact = doc.toObject(Contact.class);
                        CONTACTS.put(contact.getCid(), contact);
                    }
                }
            }
        });
    }

    private void getContacts(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference contactsRef =
                db.collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).collection(getString(R.string.collection_contacts));
        mContactEventListener = contactsRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if (queryDocumentSnapshots != null) {
                    Log.d(TAG, "onEvent: loc service got contacts");
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        Contact contact = doc.toObject(Contact.class);
                        CONTACTS.put(contact.getCid(), contact);
                    }
//                    if(queryDocumentSnapshots.size() == 0){
//                        mContactsFetched = true;
//                        checkReady();
//                    }
                }

            }
        });
//        contactsRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                if (task.isSuccessful()){
//                    CONTACTS.clear();
//                    Contact contact;
//                    for (QueryDocumentSnapshot snapshot: task.getResult()){
//                        contact = snapshot.toObject(Contact.class);
//                        CONTACTS.put(contact.getCid(), contact);
//                    }
//                }
//            }
//        });
    }

    private void getContactLocations(){
        for (Contact contact: CONTACTS.values()){
            getContactLocation(contact);
        }
    }

    private void getContactLocation(final Contact contact){
        DocumentReference locRef =
                FirebaseFirestore.getInstance().collection(getString(R.string.collection_user_locations)).document(contact.getCid());
        locRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()){
                    final UserLocation userLocation = task.getResult().toObject(UserLocation.class);
                    if (userLocation != null){
//                        CONTACT_LOCATIONS.put(userLocation.getUser().getUser_id(), userLocation);
                        Location loc1 = new Location("");
                        loc1.setLatitude(userLocation.getGeo_point().getLatitude());
                        loc1.setLongitude(userLocation.getGeo_point().getLongitude());
                        if (location.distanceTo(loc1) <= 10000){ //todo set preference

                            Long cur_time = new Date().getTime();
                            if (!contact.isInArea() && cur_time - contact.getLast_sent().getTime() >= 3600000) {
                                final User user = ((UserClient) (getApplicationContext())).getUser();
                                FirebaseFirestore fs = FirebaseFirestore.getInstance();
                                final DocumentReference uContactRef =
                                        fs.collection(getString(R.string.collection_users)).document(contact.getCid()).collection(getString(R.string.collection_contacts)).document(user.getUser_id());
                                uContactRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (!task.isSuccessful()) {
                                            return;
                                        }
                                        Contact uContact = task.getResult().toObject(Contact.class);
                                        if (uContact != null) {

                                            FCM.send_FCM_Notification(userLocation.getUser().getToken(), "Proximity Alert",
                                                    "you are close to: " + uContact.getName());

                                            uContact.setLast_sent(new Date());
                                            uContact.setInArea(true);
                                            uContactRef.set(uContact);
                                            uContactRef.update("inArea", true);
                                        }
                                    }
                                });
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "1234")
                                        .setContentTitle("Proximity Alert")
                                        .setContentText("you are near " + contact.getName())
                                        .setSmallIcon(R.drawable.chef)
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                if (Build.VERSION.SDK_INT >= 26) {
                                    builder.setChannelId(PROX_CHANNEL_ID);

                                }

                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

// notificationId is a unique int for each notification that you must define
                                notificationManager.notify(12345, builder.build());

                                Contact nContact = new Contact(contact.getName(), contact.getUsername(), contact.getAvatar(),
                                        contact.getCid());
                                nContact.setInArea(true);
                                nContact.setLast_sent(new Date());
                                CONTACTS.get(contact.getCid()).setLast_sent(new Date());
                                fs.collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).collection(getString(R.string.collection_contacts)).document(contact.getCid()).set(nContact);

                            }

                        } else {
                            FirebaseFirestore.getInstance().collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).collection(getString(R.string.collection_contacts)).document(contact.getCid()).update("inArea", false);
                        }

//                        if (mContactLocations.size() == numTotal){
//                            mContactsFetched = true;
//                            checkReady();
//                        }
                    }
                }
            }
        });
    }

//    private void createNotificationChannel() {
//        // Create the NotificationChannel, but only on API 26+ because
//        // the NotificationChannel class is new and not in the support library
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = "poximity";
//            int importance = NotificationManager.IMPORTANCE_DEFAULT;
//            NotificationChannel channel = new NotificationChannel("1234", name, importance);
//            // Register the channel with the system; you can't change the importance
//            // or other notification behaviors after this
//            NotificationManager notificationManager = getSystemService(NotificationManager.class);
//            notificationManager.createNotificationChannel(channel);
//
//        }
//    }
}