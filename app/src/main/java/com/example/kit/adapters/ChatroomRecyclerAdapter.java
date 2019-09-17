package com.example.kit.adapters;

import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import androidx.recyclerview.widget.RecyclerView;

import com.example.kit.R;
import com.example.kit.models.Chatroom;
import com.example.kit.models.Contact;
import com.example.kit.models.UChatroom;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ChatroomRecyclerAdapter extends RecyclerView.Adapter<ChatroomRecyclerAdapter.ViewHolder>{

    private ArrayList<UChatroom> mChatrooms = new ArrayList<>();
    private ChatroomRecyclerClickListener mChatroomRecyclerClickListener;

    public ChatroomRecyclerAdapter(ArrayList<UChatroom> chatrooms, ChatroomRecyclerClickListener chatroomRecyclerClickListener) {
        this.mChatrooms = chatrooms;
        mChatroomRecyclerClickListener = chatroomRecyclerClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_chatroom_list_item, parent, false);
        final ViewHolder holder = new ViewHolder(view, mChatroomRecyclerClickListener);


        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ((ViewHolder)holder).chatroomTitle.setText(mChatrooms.get(position).getDisplay_name());
    }

    @Override
    public int getItemCount() {
        return mChatrooms.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener
    {
        TextView chatroomTitle;
        ChatroomRecyclerClickListener clickListener;

        public ViewHolder(View itemView, ChatroomRecyclerClickListener clickListener) {
            super(itemView);
            chatroomTitle = itemView.findViewById(R.id.chatroom_title);
            this.clickListener = clickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickListener.onChatroomSelected(getAdapterPosition());
        }
    }

    public interface ChatroomRecyclerClickListener {
        public void onChatroomSelected(int position);
    }
}
















