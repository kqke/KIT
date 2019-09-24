package com.example.kit.adapters;

import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;


import androidx.recyclerview.widget.RecyclerView;

import com.example.kit.R;
import com.example.kit.models.UChatroom;

import java.util.ArrayList;

public class ChatroomRecyclerAdapter extends RecyclerView.Adapter<ChatroomRecyclerAdapter.ViewHolder>
        implements Filterable
{

    private ArrayList<UChatroom> mChatrooms = new ArrayList<>();
    private ArrayList<UChatroom> mFilteredChatrooms = new ArrayList<>();
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

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charSequenceString = constraint.toString();
                if (charSequenceString.isEmpty()) {
                    mFilteredChatrooms = mChatrooms;
                } else {
                    ArrayList<UChatroom> mFiltered = new ArrayList<>();
                    for (UChatroom chatroom : mChatrooms) {
                        if (chatroom.getDisplay_name().toLowerCase().contains(charSequenceString.toLowerCase())) {
                            mFiltered.add(chatroom);
                        }
                    }
                    mFilteredChatrooms = mFiltered;
                }
                FilterResults results = new FilterResults();
                results.values = mFilteredChatrooms;
                return results;
            }
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFilteredChatrooms = (ArrayList<UChatroom>)results.values;
                notifyDataSetChanged();
            }
        };
    }

    public interface ChatroomRecyclerClickListener {
        public void onChatroomSelected(int position);
    }
}
















