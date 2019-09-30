package com.example.kit.ui;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.adapters.ChatMessageRecyclerAdapter;
import com.example.kit.models.ChatMessage;
import com.example.kit.models.Contact;
import com.example.kit.models.UChatroom;
import com.example.kit.models.User;
import com.example.kit.models.UserLocation;
import com.example.kit.util.FCM;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import static android.widget.LinearLayout.HORIZONTAL;


public class ChatFragment extends DBGeoFragment implements
        View.OnClickListener,
        ChatMessageRecyclerAdapter.MessageRecyclerClickListener
{

    //TODO
    // add actionbar to the map fragment that opens up with chat participants
    //TODO
    // make a nicer menu for actionbar actions
    //TODO
    // make a better layout for the keyboard, which includes a schedule a meeting button
    //TODO
    // onLongClickListener for message items
    //TODO
    // chatroom images
    //TODO
    // swipe functionality for messages? (like in whatsapp)

    //Tag
    private static final String TAG = "ChatFragment";

    //RecyclerView
    private RecyclerView mChatMessageRecyclerView;
    private ChatMessageRecyclerAdapter mChatMessageRecyclerAdapter;

    //Firebase
    private FirebaseFirestore mDb;

    //widgets
    private EditText mMessage;
    private View v;

    //vars
    private UChatroom mChatroom;
    private ListenerRegistration mChatMessageEventListener, mUserListEventListener;
    private ArrayList<ChatMessage> mMessages = new ArrayList<>();
    private Set<String> mMessageIds = new HashSet<>();
    private ArrayList<User> mUserList = new ArrayList<>();
    private ArrayList<UserLocation> mUserLocations = new ArrayList<>();
    private ArrayList<String> mUserTokens = new ArrayList<>();

    private ChatroomCallback getData;

     /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public ChatFragment() {
        // Required empty public constructor
    }

    public static ChatFragment newInstance() {
        return new ChatFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    public void onResume() {
        super.onResume();
        getChatMessages();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_chat, container, false);
        initView();
        return v;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        getData =  (ChatroomCallback)context;
        mChatroom = getData.getChatroom();
        mUserList = getData.getUserList();
        mUserLocations = getData.getUserLocations();
        mUserTokens = getData.getUserTokens();
    }

    @Override
    public void onPause() {
        super.onPause();
        hideSoftKeyboard();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mChatMessageEventListener != null){
            mChatMessageEventListener.remove();
        }
        if(mUserListEventListener != null){
            mUserListEventListener.remove();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mChatMessageEventListener != null) {
            mChatMessageEventListener.remove();
        }
        if (mUserListEventListener != null){
            mUserListEventListener.remove();
        }
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void init(){
        mDb = FirebaseFirestore.getInstance();
        mChatroom = ((ChatroomActivity)getActivity()).getChatroom();
//        getChatroomUsers();
    }

    private void initView(){
        setupUI(v.findViewById(R.id.parent));
        mMessage = v.findViewById(R.id.input_message);
        v.findViewById(R.id.checkmark).setOnClickListener(this);
        v.findViewById(R.id.lets_meet).setOnClickListener(this);
        mChatMessageRecyclerView = v.findViewById(R.id.chatmessage_recycler_view);
        initChatroomRecyclerView();
    }

    public void setupUI(View view) {

        // Set up touch listener for non-text box views to hide keyboard.
        if (!(view instanceof EditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard();
                    return false;
                }
            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                View innerView = ((ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }


    private void initChatroomRecyclerView(){
        User user = ((UserClient)(getActivity().getApplicationContext())).getUser();
        String userID = user.getUser_id();
        mChatMessageRecyclerAdapter = new ChatMessageRecyclerAdapter(mMessages, new ArrayList<User>(), getActivity(), userID, this);
        mChatMessageRecyclerView.setAdapter(mChatMessageRecyclerAdapter);
        mChatMessageRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mChatMessageRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v,
                                       int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    mChatMessageRecyclerView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(mMessages.size() > 0){
                                mChatMessageRecyclerView.smoothScrollToPosition(
                                        mChatMessageRecyclerView.getAdapter().getItemCount() - 1);
                            }

                        }
                    }, 100);
                }
            }
        });

    }

    /*
    ----------------------------- onClick ---------------------------------
    */

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.checkmark:{
                insertNewMessage();
                break;
            }
            case R.id.lets_meet:{
                getDate(null);
                break;
            }
        }
    }

    @Override
    public void onMessageSelected(int position) {
        final ChatMessage message = mMessages.get(position);
        if(message.isExpired()){
            Toast.makeText(mActivity, "This invite has expired...", Toast.LENGTH_SHORT).show();

        }
        else if(message.getUser().getUser_id()
                .equals(((UserClient)mActivity.getApplicationContext()).getUser().getUser_id())){
            Toast.makeText(mActivity, "Waiting for approval", Toast.LENGTH_SHORT).show();
        }
        else {
            final View dialogView = View.inflate(mActivity, R.layout.dialog_schedule, null);
            final AlertDialog alertDialog = new AlertDialog.Builder(mActivity).create();
            Contact contact = getData.getContact(message.getUser().getUser_id());
            ((TextView)dialogView.findViewById(R.id.profile_displayName)).setText(contact.getUsername());
            SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
            ((TextView)dialogView.findViewById(R.id.date)).setText(formatter.format(message.getMeeting_time()));
            RequestOptions requestOptions = new RequestOptions()
                    .error(R.drawable.cartman_cop)
                    .placeholder(R.drawable.cartman_cop);
            Glide.with(this)
                    .setDefaultRequestOptions(requestOptions)
                    .load(contact.getAvatar())
                    .into((CircleImageView)dialogView.findViewById(R.id.avatar));
            dialogView.findViewById(R.id.set).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    message.setExpired(true);
                    DocumentReference messageRef = mDb
                            .collection(getString(R.string.collection_chatrooms))
                            .document(mChatroom.getChatroom_id())
                            .collection(getString(R.string.collection_chat_messages))
                            .document(message.getMessage_id());
                    messageRef.update("expired", true);
                    alertDialog.dismiss();
                    Intent intent = new Intent(Intent.ACTION_INSERT)
                            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                                    message.getMeeting_time())
                            .setData(CalendarContract.Events.CONTENT_URI)
                            .putExtra(CalendarContract.Events.TITLE, "KIT: ")
                            .putExtra(CalendarContract.Events.DESCRIPTION, "meeting")
                            .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);
                    startActivity(intent);
                }
            });

            dialogView.findViewById(R.id.reschedule).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                    getDate(message);
                }
            });
            alertDialog.setView(dialogView);
            alertDialog.show();
        }
    }

    @Override
    public void onMessageLongClicked(int position) {
        // TODO
        // some deletion dialog
    }

    public void getDate(final ChatMessage message){
        final View dialogView = View.inflate(mActivity, R.layout.date_time_picker, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(mActivity).create();

        dialogView.findViewById(R.id.date_time_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DatePicker datePicker = (DatePicker) dialogView.findViewById(R.id.date_picker);
                TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.time_picker);

                Calendar calendar = new GregorianCalendar(datePicker.getYear(),
                        datePicker.getMonth(),
                        datePicker.getDayOfMonth(),
                        timePicker.getCurrentHour(),
                        timePicker.getCurrentMinute());
                insertMeetInvite(new Date(calendar.getTimeInMillis()));
                if(message != null){
                    message.setExpired(true);
                    DocumentReference messageRef = mDb
                            .collection(getString(R.string.collection_chatrooms))
                            .document(mChatroom.getChatroom_id())
                            .collection(getString(R.string.collection_chat_messages))
                            .document(message.getMessage_id());
                    messageRef.update("expired", true);
                }
                alertDialog.dismiss();
            }});
        alertDialog.setView(dialogView);
        alertDialog.show();
    }

    /*
    ----------------------------- DB ---------------------------------
    */

    private void getChatMessages(){

        CollectionReference messagesRef = mDb
                .collection(getString(R.string.collection_chatrooms))
                .document(mChatroom.getChatroom_id())
                .collection(getString(R.string.collection_chat_messages));

        mChatMessageEventListener = messagesRef
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "onEvent: Listen failed.", e);
                            return;
                        }

                        if(queryDocumentSnapshots != null){
                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                                ChatMessage message = doc.toObject(ChatMessage.class);
                                if(!mMessageIds.contains(message.getMessage_id())){
                                    mMessageIds.add(message.getMessage_id());
                                    mMessages.add(message);
                                    mChatMessageRecyclerView.smoothScrollToPosition(mMessages.size() - 1);
                                    mDb.collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).collection(getString(R.string.collection_user_chatrooms)).document(mChatroom.getChatroom_id()).update("read_last_message", true);
                                }

                            }
                            mChatMessageRecyclerAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void insertNewMessage(){
        String message = mMessage.getText().toString();

        if(!message.equals("")){
            message = message.replaceAll(System.getProperty("line.separator"), "");

            DocumentReference newMessageDoc = mDb
                    .collection(getString(R.string.collection_chatrooms))
                    .document(mChatroom.getChatroom_id())
                    .collection(getString(R.string.collection_chat_messages))
                    .document();

            final ChatMessage newChatMessage = new ChatMessage();
            newChatMessage.setMessage(message);
            newChatMessage.setMessage_id(newMessageDoc.getId());

            final User user = ((UserClient)(getActivity().getApplicationContext())).getUser();
            Log.d(TAG, "insertNewMessage: retrieved user client: " + user.toString());
            newChatMessage.setUser(user);
            newChatMessage.setTimestamp(new Date());
            newMessageDoc.set(newChatMessage).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        String uid = user.getUser_id();
                        for (final User u: mUserList){
                            mDb.collection(getString(R.string.collection_users))
                                    .document(u.getUser_id()).collection(getString(R.string.collection_user_chatrooms))
                                    .document(mChatroom.getChatroom_id()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()){
                                        UChatroom uChatroom = task.getResult().toObject(UChatroom.class);
                                        uChatroom.setLast_message(newChatMessage.getMessage());
                                        uChatroom.setTime_last_sent(new Date());
                                        if (!u.getUser_id().equals(user.getUser_id())){
                                            uChatroom.setRead_last_message(false);
                                        }
                                        DocumentReference chatRef =
                                                mDb.collection(getString(R.string.collection_users)).document(u.getUser_id()).collection(getString(R.string.collection_user_chatrooms)).document(mChatroom.getChatroom_id());
                                        mDb.collection(getString(R.string.collection_users)).document(u.getUser_id()).collection(getString(R.string.collection_user_chatrooms)).document(mChatroom.getChatroom_id()).set(uChatroom).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (u.getUser_id().equals(user.getUser_id())) {return;}
                                                FCM.send_FCM_Notification(u.getToken(), "message", newChatMessage.getMessage());
                                            }
                                        });

                                    }
                                }
                            });

                        }
                        clearMessage();
                    }else{
                        Snackbar.make(getView(), "Something went wrong.", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });

        }
    }

    private void insertMeetInvite(Date date){

        DocumentReference newMessageDoc = mDb
                .collection(getString(R.string.collection_chatrooms))
                .document(mChatroom.getChatroom_id())
                .collection(getString(R.string.collection_chat_messages))
                .document();

        final ChatMessage newChatMessage = new ChatMessage();
        newChatMessage.setMessage_id(newMessageDoc.getId());
        newChatMessage.setInvite(true);
        newChatMessage.setMeeting_time(date);
        newChatMessage.setExpired(false);

        final User user = ((UserClient)(getActivity().getApplicationContext())).getUser();
        Log.d(TAG, "insertNewMessage: retrieved user client: " + user.toString());
        newChatMessage.setUser(user);
        newChatMessage.setTimestamp(new Date());
        newMessageDoc.set(newChatMessage).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    String uid = user.getUser_id();
                    for (final User u: mUserList){
                        mDb.collection(getString(R.string.collection_users))
                                .document(u.getUser_id()).collection(getString(R.string.collection_user_chatrooms))
                                .document(mChatroom.getChatroom_id()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()){
                                    UChatroom uChatroom = task.getResult().toObject(UChatroom.class);
                                    uChatroom.setLast_message("lets meet!");
                                    uChatroom.setTime_last_sent(new Date());
                                    if (!u.getUser_id().equals(user.getUser_id())){
                                        uChatroom.setRead_last_message(false);
                                    }
                                    DocumentReference chatRef =
                                            mDb.collection(getString(R.string.collection_users)).document(u.getUser_id()).collection(getString(R.string.collection_user_chatrooms)).document(mChatroom.getChatroom_id());
                                    mDb.collection(getString(R.string.collection_users)).document(u.getUser_id()).collection(getString(R.string.collection_user_chatrooms)).document(mChatroom.getChatroom_id()).set(uChatroom).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (u.getUser_id().equals(user.getUser_id())) {return;}
                                            FCM.send_FCM_Notification(u.getToken(), "message", newChatMessage.getMessage());
                                        }
                                    });

                                }
                            }
                        });

                        if (u.getUser_id().equals(uid)) {continue;}
                        FCM.send_FCM_Notification(u.getToken(), "message", "MEET INVITE");
                    }
                }else{
                    Snackbar.make(getView(), "Something went wrong.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

    }

    /*
    ----------------------------- utils ---------------------------------
    */

    private void clearMessage(){
        mMessage.setText("");
    }

    private void hideSoftKeyboard(){
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    /*
    ----------------------------- Chatroom Callback ---------------------------------
    */

    public interface ChatroomCallback {
        UChatroom getChatroom();
        ArrayList<User> getUserList();
        ArrayList<UserLocation>getUserLocations();
        ArrayList<String>getUserTokens();
        UserLocation getUserLocation();
        Contact getContact(String id);
    }

}
