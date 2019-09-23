package com.example.kit.adapters;

import androidx.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.recyclerview.widget.RecyclerView;

import com.example.kit.R;
import com.example.kit.models.Contact;

import java.util.ArrayList;

public class ContactRecyclerAdapter extends RecyclerView.Adapter<ContactRecyclerAdapter.ViewHolder>
        implements Filterable {

    private ArrayList<Contact> mContacts = new ArrayList<>();
    private ArrayList<Contact> mFilteredContacts = new ArrayList<>();
    private ContactsRecyclerClickListener mContactRecyclerClickListener;

    public ContactRecyclerAdapter(ArrayList<Contact> contacts, ContactsRecyclerClickListener contactRecyclerClickListener) {
        this.mContacts = contacts;
        this.mFilteredContacts = contacts;
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
        ((ViewHolder)holder).contactName.setText(mFilteredContacts.get(position).getName());
        ((ViewHolder)holder).contactUsername.setText(mFilteredContacts.get(position).getUsername());
//        ((ViewHolder)holder).contactAvatar.setImage;
    }

    @Override
    public int getItemCount() {
        return mFilteredContacts.size();
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

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String charSequenceString = constraint.toString();
                if (charSequenceString.isEmpty()) {
                    mFilteredContacts = mContacts;
                } else {
                    ArrayList<Contact> mFiltered = new ArrayList<>();
                    for (Contact contact : mContacts) {
                        if (contact.getName().toLowerCase().contains(charSequenceString.toLowerCase())) {
                            mFiltered.add(contact);
                        }
                    }
                    mFilteredContacts = mFiltered;
                }
                FilterResults results = new FilterResults();
                results.values = mFilteredContacts;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFilteredContacts = (ArrayList<Contact>)results.values;
                notifyDataSetChanged();
            }
        };
    }

    public interface ContactsRecyclerClickListener {
        void onContactSelected(int position);
    }
}
