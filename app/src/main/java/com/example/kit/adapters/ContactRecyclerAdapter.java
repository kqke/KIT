package com.example.kit.adapters;

import androidx.annotation.NonNull;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.recyclerview.widget.RecyclerView;

import com.example.kit.R;
import com.example.kit.models.Contact;

import java.util.ArrayList;

public class ContactRecyclerAdapter extends RecyclerView.Adapter<ContactRecyclerAdapter.ViewHolder>{

    private ArrayList<Contact> mContacts = new ArrayList<>();
    private ContactsRecyclerClickListener mContactRecyclerClickListener;

    public ContactRecyclerAdapter(ArrayList<Contact> contacts, ContactsRecyclerClickListener contactRecyclerClickListener) {
        this.mContacts = contacts;
        mContactRecyclerClickListener = contactRecyclerClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_contact_list_item, parent, false);
        final ViewHolder holder = new ViewHolder(view, mContactRecyclerClickListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ((ViewHolder)holder).contactName.setText(mContacts.get(position).getName());
        ((ViewHolder)holder).contactUsername.setText(mContacts.get(position).getUsername());
//        ((ViewHolder)holder).contactAvatar.setImage;
    }

    @Override
    public int getItemCount() {
        return mContacts.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener
    {
        TextView contactName;
        TextView contactUsername;
        ImageView contactAvatar;
        ContactsRecyclerClickListener clickListener;

        public ViewHolder(View itemView, ContactsRecyclerClickListener clickListener) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name);
            contactUsername = itemView.findViewById(R.id.contact_username);
            contactAvatar = itemView.findViewById(R.id.contact_avatar);
            this.clickListener = clickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickListener.onContactSelected(getAdapterPosition());
        }
    }

    public interface ContactsRecyclerClickListener {
        public void onContactSelected(int position);
    }
}
