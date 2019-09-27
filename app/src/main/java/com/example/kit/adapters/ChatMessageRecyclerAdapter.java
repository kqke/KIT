package com.example.kit.adapters;

import android.content.Context;
import androidx.annotation.NonNull;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.kit.R;
import com.example.kit.models.ChatMessage;
import com.example.kit.models.User;
import com.example.kit.ui.ChatFragment;

import java.text.DateFormat;
import java.util.ArrayList;

import static com.example.kit.Constants.VIEW_TYPE_INVITE;
import static com.example.kit.Constants.VIEW_TYPE_MESSAGE_SENT;
import static com.example.kit.Constants.VIEW_TYPE_MESSAGE_RECEIVED;


public class ChatMessageRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<ChatMessage> mMessages = new ArrayList<>();
    private ArrayList<User> mUsers = new ArrayList<>();
    private Context mContext;
    private String mUserID;
    private MessageRecyclerClickListener listener;

    public ChatMessageRecyclerAdapter(ArrayList<ChatMessage> messages,
                                      ArrayList<User> users,
                                      Context context, String userID,
                                      MessageRecyclerClickListener listener) {
        this.mMessages = messages;
        this.mUsers = users;
        this.mContext = context;
        this.mUserID = userID;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            final RecyclerView.ViewHolder holder = new SentMessageHolder(view, listener);
            return holder;
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            final RecyclerView.ViewHolder holder = new ReceivedMessageHolder(view, listener);
            return holder;
        }
        else if(viewType == VIEW_TYPE_INVITE){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_invite, parent, false);
            final RecyclerView.ViewHolder holder = new InviteHolder(view, listener);
            return holder;
        }

        return null;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = (ChatMessage) mMessages.get(position);
        if(message.isInvite()){
            return VIEW_TYPE_INVITE;
        }
        else if (message.getUser().getUser_id().equals(mUserID)) {
            // If the current user is the sender of the message
            return VIEW_TYPE_MESSAGE_SENT;
        }
        else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {

        ChatMessage message = (ChatMessage) mMessages.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_INVITE:
                ((InviteHolder) holder).bind(message);
        }
    }



    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder
            implements View.OnLongClickListener{
        TextView messageText, timeText, nameText;
        MessageRecyclerClickListener longClickListener;

        ReceivedMessageHolder(View itemView, MessageRecyclerClickListener listener) {
            super(itemView);
            messageText = (TextView) itemView.findViewById(R.id.chat_message_message);
            nameText = (TextView) itemView.findViewById(R.id.chat_message_username);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
            longClickListener = listener;
            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            longClickListener.onMessageLongClicked(getAdapterPosition());
            return true;
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessage());

            timeText.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(message.getTimestamp()));
            nameText.setText(message.getUser().getUsername());
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder
            implements View.OnLongClickListener{
        TextView messageText, timeText;
        MessageRecyclerClickListener longClickListener;

        SentMessageHolder(View itemView, MessageRecyclerClickListener listener) {
            super(itemView);
            messageText = (TextView) itemView.findViewById(R.id.chat_message_message);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
            longClickListener = listener;
            itemView.setOnLongClickListener(this);
            Log.d("BC", "SentMessageHolder: " + timeText.toString());
        }

        @Override
        public boolean onLongClick(View v) {
            longClickListener.onMessageLongClicked(getAdapterPosition());
            return true;
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessage());
            // Format the stored timestamp into a readable String using method.
            timeText.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(message.getTimestamp()));
        }
    }

    private class InviteHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener{

        TextView timeText;
        MessageRecyclerClickListener clickListener;

        InviteHolder(View itemview, MessageRecyclerClickListener listener) {
            super(itemview);
            Button meetButton = itemview.findViewById(R.id.lets_meet);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
            clickListener = listener;
            meetButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickListener.onMessageSelected(getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            return true;
        }

        void bind(ChatMessage message) {
            timeText.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(message.getTimestamp()));
        }
    }

    public interface MessageRecyclerClickListener {
        void onMessageSelected(int position);
        void onMessageLongClicked(int position);
    }
}
















