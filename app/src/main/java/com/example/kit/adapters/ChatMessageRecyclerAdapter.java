package com.example.kit.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import io.opencensus.internal.Utils;

import com.example.kit.R;
import com.example.kit.UserClient;
import com.example.kit.models.ChatMessage;
import com.example.kit.models.User;
import com.google.firebase.auth.FirebaseAuth;

import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;

import static com.example.kit.Constants.VIEW_TYPE_MESSAGE_SENT;
import static com.example.kit.Constants.VIEW_TYPE_MESSAGE_RECEIVED;


public class ChatMessageRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private ArrayList<ChatMessage> mMessages = new ArrayList<>();
    private ArrayList<User> mUsers = new ArrayList<>();
    private Context mContext;
    private String mUserID;

    public ChatMessageRecyclerAdapter(ArrayList<ChatMessage> messages,
                                      ArrayList<User> users,
                                      Context context, String userID) {
        this.mMessages = messages;
        this.mUsers = users;
        this.mContext = context;
        this.mUserID = userID;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_chat_message_list_item, parent, false);
//        final ViewHolder holder = new ViewHolder(view);
//        return holder;
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            final RecyclerView.ViewHolder holder = new SentMessageHolder(view);
            return holder;
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            final RecyclerView.ViewHolder holder = new ReceivedMessageHolder(view);
            return holder;
        }

        return null;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = (ChatMessage) mMessages.get(position);
        if (message.getUser().getUser_id().equals(mUserID)) {
            // If the current user is the sender of the message
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            // If some other user sent the message
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
        }
    }



    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView message, username;

        public ViewHolder(View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.chat_message_message);
            username = itemView.findViewById(R.id.chat_message_username);
        }
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, nameText;
//        ImageView profileImage;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            messageText = (TextView) itemView.findViewById(R.id.chat_message_message);
            nameText = (TextView) itemView.findViewById(R.id.chat_message_username);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time); //missing
//            profileImage = (ImageView) itemView.findViewById(R.id.image_message_profile); //missing
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessage());

            // Format the stored timestamp into a readable String using method.

            timeText.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(message.getTimestamp()));
            nameText.setText(message.getUser().getUsername());

//             Insert the profile image from the URL into the ImageView.
//            Utils.displayRoundImageFromUrl(mContext, message.getUser().getUsername(), profileImage);
        }
    }

    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText, nameText;

        SentMessageHolder(View itemView) {
            super(itemView);
            messageText = (TextView) itemView.findViewById(R.id.chat_message_message);
            timeText = (TextView) itemView.findViewById(R.id.text_message_time);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessage());

            // Format the stored timestamp into a readable String using method.
            timeText.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(message.getTimestamp()));
            nameText.setText(message.getUser().getUsername());
        }
    }


}
















