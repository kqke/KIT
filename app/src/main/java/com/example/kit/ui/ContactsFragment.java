package com.example.kit.ui;

import android.Manifest;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.kit.R;
import com.example.kit.adapters.ContactRecyclerAdapter;
import com.example.kit.models.Contact;
import com.example.kit.models.UserLocation;
import com.example.kit.util.ViewWeightAnimationWrapper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import static android.widget.LinearLayout.HORIZONTAL;


public class ContactsFragment extends DBGeoFragment implements
        ContactRecyclerAdapter.ContactsRecyclerClickListener,
        View.OnClickListener
{

    //Tag
    private static final String TAG = "ContactsFragment";

    //RecyclerView
    private ContactRecyclerAdapter mContactRecyclerAdapter;
    private RecyclerView mContactRecyclerView;

    //Contacts
    private HashMap<String, Contact> mContacts = new HashMap<>();
    private ArrayList<Contact> mRecyclerList = new ArrayList<>();

    //Contacts Callback
    ContactsCallback getData;

    //Listeners
    private ListenerRegistration mContactEventListener;

    //Vars
    private ContactsFragment mContactFragment;

    //New Contact Dialog
    private AlertDialog alertDialog;
    EditText inputUsername;
    TextureView cameraPreview;
    private final AtomicBoolean shouldThrottle = new AtomicBoolean(false);
    private boolean cameraOpen;
    private Button startCameraBtn;
    private ImageAnalysis imageAnalysis;
    private Preview preview;
    private LinearLayout linearLayout;
    private int numClassified = 0;


    private static View view;

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public ContactsFragment() {
        super();
    }

    public static ContactsFragment newInstance() {
        return new ContactsFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContactFragment = this;
        getData = (ContactsCallback)context;
        ArrayList<UserLocation> locs = getData.getUserLocations();
        mContacts = getData.getId2Contact();

        for (UserLocation userLocation: locs){

            if (mContacts.containsKey(userLocation.getUser().getUser_id())) {
                mContacts.get(userLocation.getUser().getUser_id()).setAvatar(userLocation.getUser().getAvatar());
            }
        }
        mRecyclerList = new ArrayList<>(mContacts.values());
        if (mContactRecyclerAdapter != null){
            mContactRecyclerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mContactEventListener != null) {
            mContactEventListener.remove();
        }
    }

    @Override
    public void onResume() {
        initListener();
        super.onResume();
        getContacts();
    }

    @Override
    public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        initView(view);
        initListener();
        return view;
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void getContacts(){
        mDb.collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).collection(getString(R.string.collection_contacts)).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (!task.isSuccessful()){ return; }
                mContacts.clear();
                for (QueryDocumentSnapshot doc: task.getResult()){
                    Contact contact = doc.toObject(Contact.class);
                    mContacts.put(contact.getCid(), contact);
                }
                mRecyclerList.clear();
                mRecyclerList = new ArrayList<>(mContacts.values());
                notifyRecyclerView();
            }
        });
    }

    private void notifyRecyclerView(){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                mContactRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
                        mContactFragment, R.layout.layout_contact_list_item, getContext());
                mContactRecyclerView.setAdapter(mContactRecyclerAdapter);
                mContactRecyclerAdapter.notifyDataSetChanged();
                mContactRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
                DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
                mContactRecyclerView.addItemDecoration(itemDecor);
                mContactRecyclerAdapter.notifyDataSetChanged();
            }
        });
    }

    private void initView(View v){
        linearLayout = v.findViewById(R.id.linear);
        if(mContacts.size()==0){
            linearLayout.setVisibility(View.VISIBLE);
        }
        v.findViewById(R.id.fab).setOnClickListener(this);
        mContactRecyclerView = v.findViewById(R.id.contact_recycler_view);
        mContactRecyclerAdapter = new ContactRecyclerAdapter(mRecyclerList,
                this, R.layout.layout_contact_list_item, getContext());
        mContactRecyclerView.setAdapter(mContactRecyclerAdapter);
        mContactRecyclerAdapter.notifyDataSetChanged();
        mContactRecyclerView.setLayoutManager(new LinearLayoutManager(mActivity));
        DividerItemDecoration itemDecor = new DividerItemDecoration(mActivity, HORIZONTAL);
        mContactRecyclerView.addItemDecoration(itemDecor);
        v.findViewById(R.id.fab).setOnClickListener(this);
        initSearchView(v);
    }

    private void initSearchView(View v){
        SearchView searchView = v.findViewById(R.id.search_view);
        ImageView icon = searchView.findViewById(R.id.search_button);
        icon.setColorFilter(Color.BLACK);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String queryString) {
                mContactRecyclerAdapter.getFilter().filter(queryString);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String queryString) {
                mContactRecyclerAdapter.getFilter().filter(queryString);
                return false;
            }
        });
    }

    private void initListener(){
        CollectionReference contactsCollection = mDb
                .collection(getString(R.string.collection_users))
                .document(FirebaseAuth.getInstance().getUid())
                .collection(getString(R.string.collection_contacts));
        mContactEventListener = contactsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");
                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }
                if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                    Contact contact;
                    //Instead of simply using the entire query snapshot
                    //See the actual changes to query results between query snapshots (added, removed, and modified)
                    for (DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()) {
                        switch (doc.getType()) {

                            case ADDED:

                                //Call the model to populate it with document
                                contact = doc.getDocument().toObject(Contact.class);
                                mContacts.put(contact.getCid(), contact);
                                mRecyclerList = new ArrayList<>(mContacts.values());
                                getData.updateContacts(mRecyclerList, mContacts);
                                notifyRecyclerView();
                                linearLayout.setVisibility(View.INVISIBLE);

                                Log.d(TAG,"THIS SHOULD BE CALLED");

                                   /* //Just call this method once
                                    if (noContent.isShown()){
                                        //This will be called only if user added some new post
                                        announcementList.add(annonPost);
                                        announcementRecyclerAdapter.notifyDataSetChanged();
                                        noContent.setVisibility(View.GONE);
                                        label.setVisibility(View.VISIBLE);
                                    }*/

                                break;

                            case MODIFIED:
                                contact = doc.getDocument().toObject(Contact.class);
                                mContacts.put(contact.getCid(), contact);
                                mRecyclerList = new ArrayList<>(mContacts.values());
                                getData.updateContacts(mRecyclerList, mContacts);
                                notifyRecyclerView();
                                linearLayout.setVisibility(View.INVISIBLE);
                                break;

                            case REMOVED:
                                contact = doc.getDocument().toObject(Contact.class);
                                mContacts.remove(contact.getCid());
                                mRecyclerList = new ArrayList<>(mContacts.values());
                                getData.updateContacts(mRecyclerList, mContacts);
                                notifyRecyclerView();
                                if (mContacts.size() == 0) {
                                    linearLayout.setVisibility(View.VISIBLE);
                                }
                        }
                    }

//                    if (queryDocumentSnapshots != null) {
//                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
//
//                        Contact contact = doc.toObject(Contact.class);
//                        mContacts.put(contact.getCid(), contact);
//                        mRecyclerList = new ArrayList<>(mContacts.values());
//                        getData.updateContacts(mRecyclerList, mContacts);
//                        notifyRecyclerView();
//
//                        Log.d(TAG, "onEvent: number of contacts: " + mContacts.size());
//
//                    }
                }
            }
        });
    }


    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onContactSelected(int position) {
        getData.initContactFragment(mRecyclerList.get(position).getCid(), null);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.fab:
                newContactDialog();
                break;
        }
    }

    @Override
    public void onContactLongClick(int pos) {

    }

    @Override
    public void onAcceptSelected(int position) {

    }

    @Override
    public void onRejectSelected(int position) {

    }

    @Override
    public void onDeleteSelected(int position) {

    }

    /*
    ----------------------------- nav ---------------------------------
    */

    private void newContactDialog(){
        final View dialogView = View.inflate(mActivity, R.layout.dialog_new_contact, null);
        ((ViewGroup) dialogView.findViewById(R.id.a_d_ll)).getLayoutTransition()
                .setDuration(800);
        ((ViewGroup) dialogView.findViewById(R.id.a_d_ll)).getLayoutTransition()
                .enableTransitionType(LayoutTransition.CHANGING);
        alertDialog = new AlertDialog.Builder(mActivity).create();
        inputUsername = dialogView.findViewById(R.id.dialog_input);
        cameraPreview = dialogView.findViewById(R.id.camera_preview);
        startCameraBtn = dialogView.findViewById(R.id.start_camera);
        dialogView.findViewById(R.id.go_profile_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkExistingUsername(inputUsername.getText().toString());
            }
        });
        startCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!cameraOpen){
                    if(initCamera()) {
                        inputUsername.setText("");
                        startCameraBtn.setText("CLOSE CAMERA");
                        cameraPreview.setVisibility(View.VISIBLE);
                        cameraOpen = true;
                    }
                }
                else{
                    startCameraBtn.setText("SCAN TEXT");
                    cameraPreview.setVisibility(View.GONE);
                    imageAnalysis.removeAnalyzer();
                    preview.removePreviewOutputListener();
                    numClassified = 0;
                    cameraOpen = false;
                }
            }
        });
        alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                inputUsername.setText("");
                if(cameraOpen) {
                    cameraPreview.setVisibility(View.GONE);
                    startCameraBtn.setText("SCAN TEXT");
                    if (imageAnalysis != null) {
                        imageAnalysis.removeAnalyzer();
                    }
                    if (preview != null) {
                        preview.removePreviewOutputListener();
                    }
                    numClassified = 0;
                    cameraOpen = false;
                }
            }
        });
        alertDialog.setView(dialogView);
        alertDialog.show();
    }




    /*
    ----------------------------- Text Recognition ---------------------------------
    */

    private boolean initCamera() {
        boolean hasPermission = ContextCompat.checkSelfPermission(mActivity.getApplicationContext(),
                Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasCamera = mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        if (hasPermission && hasCamera){

            CameraX.unbindAll();

            PreviewConfig previewConfig = new PreviewConfig.Builder()
                    .setTargetAspectRatio(new Rational(cameraPreview.getWidth(), cameraPreview.getHeight()))
                    .setTargetResolution(new  Size(cameraPreview.getWidth()/2, cameraPreview.getHeight()))
                    .build();
            preview = new Preview(previewConfig);

            preview.setOnPreviewOutputUpdateListener(
                    new Preview.OnPreviewOutputUpdateListener() {
                        @Override
                        public void onUpdated(Preview.PreviewOutput previewOutput) {
                            ViewGroup parent = (ViewGroup) cameraPreview.getParent();
                            parent.removeView(cameraPreview);
                            parent.addView(cameraPreview, 0);
                            SurfaceTexture preview = previewOutput.getSurfaceTexture();
                            cameraPreview.setSurfaceTexture(preview);
                            updateTransform();
                        }
                    });

            ImageAnalysisConfig analysisConfig = new ImageAnalysisConfig.Builder()
                            .setTargetAspectRatio(new Rational(cameraPreview.getWidth(), cameraPreview.getHeight()))
                            .setTargetResolution(new Size(cameraPreview.getWidth() / 2, cameraPreview.getHeight() / 2))
                            .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                            .build();
            imageAnalysis = new ImageAnalysis(analysisConfig);
            imageAnalysis.setAnalyzer(
                    new ImageAnalysis.Analyzer() {
                        @Override
                        public void analyze(ImageProxy imageProxy, int degrees) {
                            if (imageProxy == null || imageProxy.getImage() == null) {
                                return;
                            }
                            Image mediaImage = imageProxy.getImage();
                            int rotation = degreesToFirebaseRotation(degrees);
                            FirebaseVisionImage image =
                                    FirebaseVisionImage.fromMediaImage(mediaImage, rotation);
                            FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                                    .getOnDeviceTextRecognizer();
                            detector.processImage(image)
                                    .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                        @Override
                                        public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                            // Task completed successfull - got text
                                            putCameraText(firebaseVisionText);
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    shouldThrottle.set(false);
                                                }
                                            },1000);
                                        }
                                    })
                                    .addOnFailureListener(
                                            new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    // Task failed with an exception
                                                    new Handler().postDelayed(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            shouldThrottle.set(false);
                                                        }
                                                    },1000);
                                                }
                                            });
                            shouldThrottle.set(true);
                        }
                    });
            CameraX.bindToLifecycle(this, imageAnalysis, preview);
            return true;
        }
        if(!hasPermission){
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA},
                    999);
        }
        if(!hasCamera){
            Toast.makeText(mActivity,
                    "You don't seem to have a camera",
                    Toast.LENGTH_SHORT).show();
        }
        return false;
    }


    private void putCameraText(FirebaseVisionText result){
        String resultText = result.getText();
        numClassified++;
        if(cameraOpen){
            inputUsername.setText(resultText);
            if(numClassified >= 4) {
                cameraPreview.setVisibility(View.GONE);
                startCameraBtn.setText("SCAN AGAIN");
                if (imageAnalysis != null) {
                    imageAnalysis.removeAnalyzer();
                }
                if (preview != null) {
                    preview.removePreviewOutputListener();
                }
                numClassified = 0;
                cameraOpen = false;
            }
        }
    }

    private void updateTransform(){
        Matrix mx = new Matrix();
        float w = cameraPreview.getMeasuredWidth();
        float h = cameraPreview.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)cameraPreview.getRotation();

        switch(rotation){
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float)rotationDgr, cX, cY);
        cameraPreview.setTransform(mx);
    }

    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException(
                        "Rotation must be 0, 90, 180, or 270.");
        }
    }


    /*
    ----------------------------- DB ---------------------------------
    */

    private void checkExistingUsername(final String userName)
    {
        CollectionReference usersRef = mDb.collection("Users");
        Query query = usersRef.whereEqualTo("username", userName);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    if (task.getResult() != null) {
                        for (DocumentSnapshot documentSnapshot : task.getResult()) {
                            String user = documentSnapshot.getString("username");
                            if (user.equals(userName)) {
                                Log.d(TAG, "chcekExisting: User Exists");
                                String userId = documentSnapshot.getString("user_id");
                                getData.initContactFragment(userId, null);
                                alertDialog.dismiss();
                                return;
                            }
                        }
                        if(task.getResult().size() == 0 ){
                            Log.d(TAG, "checkExisting: User chose a unique username");
                            Toast.makeText(mActivity,
                                    "This username does not exist",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
    }

    /*
    ----------------------------- Contacts Callback ---------------------------------
    */

    public interface ContactsCallback {
        ArrayList<Contact> getContacts();
        Set<String> getContactIds();
        HashMap<String, Contact> getId2Contact();
        ArrayList<UserLocation> getUserLocations();
        void initContactFragment(String contactID, String state);
        void updateContacts(ArrayList<Contact> contacts, HashMap<String, Contact> id2contact);
    }
}
