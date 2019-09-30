package com.example.kit.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.kit.R;
import com.example.kit.models.ClusterMarker;
import com.example.kit.models.Contact;
import com.example.kit.models.PolylineData;
import com.example.kit.models.UserLocation;
import com.example.kit.util.MyClusterManagerRenderer;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.util.ArrayList;
import java.util.List;

import static com.example.kit.Constants.MAPVIEW_BUNDLE_KEY;

public class MapFragment extends DBGeoFragment implements
            OnMapReadyCallback,
            GoogleMap.OnInfoWindowClickListener,
            GoogleMap.OnPolylineClickListener, View.OnClickListener {

    //TODO
    // navigate to Profile bubble when user is clicked

    //Tag
    private static final String TAG = "MapFragment";

    //Map
    GoogleMap mGoogleMap;
    LatLngBounds mMapBoundary;
    ClusterManager<ClusterMarker> mClusterManager;
    MyClusterManagerRenderer mClusterManagerRenderer;
    ArrayList<ClusterMarker> mClusterMarkers = new ArrayList<>();
    GeoApiContext mGeoApiContext = null;
    private ArrayList<PolylineData> mPolyLinesData = new ArrayList<>();
    private Marker mSelectedMarker = null;
    private Marker mTripMarker = null;

    //Runnable
    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;

    //Contacts
    protected ArrayList<Contact> mContactList = new ArrayList<>();
    protected ArrayList<UserLocation> mContactLocations = new ArrayList<>();
    protected ListenerRegistration mContactListEventListener;

    //Widgets
    protected MapView mMapView;
    ImageButton mapRefreshButton;


    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public MapFragment() {
        super();
    }

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        MapCallBack getData = (MapCallBack)context;
        mContactLocations = getData.getUserLocations();
        mUserLocation = getData.getUserLocation();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        Log.d(TAG, "before: ");
        Log.d(TAG, "after: ");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mapRefreshButton = view.findViewById(R.id.btn_reset_map);
        mapRefreshButton.setOnClickListener(this);
        init(view);
        initGoogleMap(savedInstanceState);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        startUserLocationsRunnable();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }
        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if(mContactListEventListener != null){
            mContactListEventListener.remove();
        }
        stopLocationUpdates();
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void init(View v){
        mMapView = v.findViewById(R.id.frag_map);
    }

    /*
    ----------------------------- DB ---------------------------------
    */

    private void startUserLocationsRunnable() {
        Log.d(TAG, "startUserLocationsRunnable: starting runnable for retrieving updated locations.");
        mHandler.postDelayed(mRunnable = new Runnable() {
            @Override
            public void run() {
                retrieveUserLocations();
                mHandler.postDelayed(mRunnable, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    private void stopLocationUpdates() {
        mHandler.removeCallbacks(mRunnable);
    }

    private void retrieveUserLocations() {
        Log.d(TAG, "retrieveUserLocations: retrieving location of all users in the chatroom");
        try {
            for (final ClusterMarker clusterMarker : mClusterMarkers) {
                DocumentReference userLocationRef = FirebaseFirestore.getInstance()
                        .collection(getString(R.string.collection_user_locations))
                        .document(clusterMarker.getUser().getUser_id());
                userLocationRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            final UserLocation updatedUserLocation = task.getResult().toObject(UserLocation.class);
                            // update the location
                            for (int i = 0; i < mClusterMarkers.size(); i++) {
                                try {
                                    if (mClusterMarkers.get(i).getUser().getUser_id().equals(updatedUserLocation.getUser().getUser_id())) {
                                        LatLng updatedLatLng = new LatLng(
                                                updatedUserLocation.getGeo_point().getLatitude(),
                                                updatedUserLocation.getGeo_point().getLongitude()
                                        );
                                        mClusterMarkers.get(i).setPosition(updatedLatLng);
                                        mClusterManagerRenderer.setUpdateMarker(mClusterMarkers.get(i));
                                    }
                                } catch (NullPointerException e) {
                                    Log.e(TAG, "retrieveUserLocations: NullPointerException: " + e.getMessage());
                                }
                            }
                        }
                    }
                });
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "retrieveUserLocations: Fragment was destroyed during Firestore query. Ending query." + e.getMessage());
        }
    }

    /*
    ----------------------------- Map ---------------------------------
    */

    protected void initGoogleMap(Bundle savedInstanceState) {
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);
        if (mGeoApiContext == null){
            mGeoApiContext = new GeoApiContext.Builder().apiKey(getString(R.string.google_maps_api_key)).build();
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "onMapReady: ");
        if (ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
        mGoogleMap = map;
        addMapMarkers();
        mGoogleMap.setOnInfoWindowClickListener(this);
        mGoogleMap.setOnPolylineClickListener(this);
    }

    void addMapMarkers() {
        if (mGoogleMap != null) {
            resetMap();

            if (mClusterManager == null) {
                mClusterManager = new ClusterManager<ClusterMarker>(mActivity.getApplicationContext(), mGoogleMap);
            }
            if (mClusterManagerRenderer == null) {
                mClusterManagerRenderer = new MyClusterManagerRenderer(
                        mActivity,
                        mGoogleMap,
                        mClusterManager
                );
                mClusterManager.setRenderer(mClusterManagerRenderer);
            }
            System.out.println(mContactLocations.toString());
            for (UserLocation userLocation : mContactLocations) {
                Log.d(TAG, "addMapMarkers: location: " + userLocation.getGeo_point().toString());
                try {
                    String snippet = "";
                    if (userLocation.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())) {
                        snippet = "This is you";
                    } else {
                        snippet = "Determine route to " + userLocation.getUser().getUsername() + "?";
                    }

                    int avatar = R.drawable.cartman_cop; // set the default avatar
                    try {
                        avatar = Integer.parseInt(userLocation.getUser().getAvatar());
                    } catch (NumberFormatException e) {
                        Log.d(TAG, "addMapMarkers: no avatar for " + userLocation.getUser().getUsername() + ", setting default.");
                    }
                    ClusterMarker newClusterMarker = new ClusterMarker(
                            new LatLng(userLocation.getGeo_point().getLatitude(), userLocation.getGeo_point().getLongitude()),
                            userLocation.getUser().getUsername(),
                            snippet,
                            avatar,
                            userLocation.getUser()
                    );
                    mClusterManager.addItem(newClusterMarker);
                    mClusterMarkers.add(newClusterMarker);

                } catch (NullPointerException e) {
                    Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage());
                }
            }
            mClusterManager.cluster();
            setCameraView();
        }
    }

    private void setCameraView() {
        if(mUserLocation != null) {
            double lat = mUserLocation.getGeo_point().getLatitude();
            double lon = mUserLocation.getGeo_point().getLongitude();
            double bb = lat - .1;
            double lb = lon - .1;
            double tb = lat + .1;
            double rb = lon + .1;
            double cur_lat, cur_long;
            for (UserLocation loc: mContactLocations){
                cur_lat = loc.getGeo_point().getLatitude();
                cur_long = loc.getGeo_point().getLongitude();
                if (cur_lat < bb) {
                    bb = cur_lat - 0.1;
                    tb = lat + Math.abs(lat - bb);
                }
                else if (cur_lat > tb){
                    tb = cur_lat + 0.1;
                    bb = lat - Math.abs(lat - tb);

                }

                if (cur_long < lb) {
                    lb = cur_long - 0.1;
                    rb = lon + Math.abs(lon - lb);
                }
                else if (cur_long > rb){
                    rb = cur_long + 0.1;
                    lb = lon - Math.abs(lon - rb);
                }
            }
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = 4;
            mMapBoundary = new LatLngBounds(new LatLng(bb, lb), new LatLng(tb, rb));
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mMapBoundary, width, height, padding));
        }
    }

    public void zoomRoute(List<LatLng> lstLatLngRoute) {
        if (mGoogleMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);
        int routePadding = 120;
        LatLngBounds latLngBounds = boundsBuilder.build();
        mGoogleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                600,
                null
        );
    }

    private void removeTripMarker(){
        if (mTripMarker != null){
            mTripMarker.remove();
            removeTripMarker();
        }
    }

    private void resetSelectedMarker(){
        if (mSelectedMarker != null) {
            mSelectedMarker.setVisible(true);
        }
    }

    private void resetMap(){
        if(mGoogleMap != null) {
            mGoogleMap.clear();

            if(mClusterManager != null){
                mClusterManager.clearItems();
            }

            if (mClusterMarkers.size() > 0) {
                mClusterMarkers.clear();
                mClusterMarkers = new ArrayList<>();
            }

            if(mPolyLinesData.size() > 0){
                mPolyLinesData.clear();
                mPolyLinesData = new ArrayList<>();
            }
        }
    }

    @Override
    public void onInfoWindowClick(final Marker marker) {
        if(marker.getTitle().contains("Trip #")){
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Open Google Maps?")
                    .setCancelable(true)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            String latitude = String.valueOf(marker.getPosition().latitude);
                            String longitude = String.valueOf(marker.getPosition().longitude);
                            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + latitude + "," + longitude);
                            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                            mapIntent.setPackage("com.google.android.apps.maps");

                            try{
                                if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                    startActivity(mapIntent);
                                }
                            }catch (NullPointerException e){
                                Log.e(TAG, "onClick: NullPointerException: Couldn't open map." + e.getMessage() );
                                Toast.makeText(getActivity(), "Couldn't open map", Toast.LENGTH_SHORT).show();
                            }

                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        } else {


            if (marker.getSnippet().equals("This is you")) {
                marker.hideInfoWindow();
            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                builder.setMessage("Navigate or Go to Profile?")
                        .setCancelable(true)
                        .setPositiveButton("Navigate", new DialogInterface.OnClickListener() {
                            public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                resetSelectedMarker();
                                mSelectedMarker = marker;
                                calculateDirections(marker);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Profile", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                dialog.cancel();
                            }
                        });
                final AlertDialog alert = builder.create();
                alert.show();
            }
        }
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        int index = 0;
        for(PolylineData polylineData: mPolyLinesData){
            index++;
            Log.d(TAG, "onPolylineClick: toString: " + polylineData.toString());
            if(polyline.getId().equals(polylineData.getPolyline().getId())){
                polylineData.getPolyline().setColor(ContextCompat.getColor(mActivity, R.color.blue1));
                polylineData.getPolyline().setZIndex(1);
                LatLng endLocation = new LatLng(polylineData.getLeg().endLocation.lat, polylineData.getLeg().endLocation.lng);
                Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(endLocation).title("Trip #: " + index).snippet(
                        "Duration: " + polylineData.getLeg().duration + "\nDistance: " + polylineData.getLeg().distance));
                mTripMarker = marker;
                zoomRoute(polyline.getPoints());
            }
            else{
                polylineData.getPolyline().setColor(ContextCompat.getColor(mActivity, R.color.darkGrey));
                polylineData.getPolyline().setZIndex(0);
            }
        }

    }

    private void calculateDirections(Marker marker){
        Log.d(TAG, "calculateDirections: calculating directions.");
        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude
        );
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);
        directions.alternatives(true);
        directions.origin(
                new com.google.maps.model.LatLng(
                        mUserLocation.getGeo_point().getLatitude(),
                        mUserLocation.getGeo_point().getLongitude()
                )
        );
        Log.d(TAG, "calculateDirections: destination: " + destination.toString());
        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());
                addPolylinesToMap(result);
                Log.d(TAG, "onResult: SUCCESSSSSS");
            }
            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage() );
            }
        });
    }

    private void addPolylinesToMap(final DirectionsResult result){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);
                if(mPolyLinesData.size() > 0){
                    for(PolylineData polylineData: mPolyLinesData){
                        polylineData.getPolyline().remove();
                    }
                    mPolyLinesData.clear();
                    mPolyLinesData = new ArrayList<>();
                }
                double duration = 999999999;
                double distance = 999999999;
                double tempDuration, tempDistance;
                for(DirectionsRoute route: result.routes){
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());
                    List<LatLng> newDecodedPath = new ArrayList<>();

                    // This loops through all the LatLng coordinates of ONE polyline.
                    for(com.google.maps.model.LatLng latLng: decodedPath){
                        Log.d(TAG, "run: latlng: " + latLng.toString());
                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = mGoogleMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(ContextCompat.getColor(mActivity, R.color.darkGrey));
                    polyline.setClickable(true);
                    mPolyLinesData.add(new PolylineData(polyline, route.legs[0]));
                    tempDuration = route.legs[0].duration.inSeconds;
                    tempDistance = route.legs[0].distance.inMeters;
                    if (tempDuration < duration || (tempDuration == duration && tempDistance < distance)) {
                        duration = tempDuration;
                        distance = tempDistance;
                        onPolylineClick(polyline);
                    }
                    mSelectedMarker.setVisible(false);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_reset_map: {
                addMapMarkers();
                break;
            }
        }
    }

    /*
    ----------------------------- Map Callback ---------------------------------
    */

    public interface MapCallBack {
        ArrayList<UserLocation> getUserLocations();
        UserLocation getUserLocation();
    }
}

