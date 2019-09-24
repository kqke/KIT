package com.example.kit.adapters;

import androidx.annotation.NonNull;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
    private ArrayList<Contact> mCheckedContacts = new ArrayList<>();
    private ContactsRecyclerClickListener mContactRecyclerClickListener;
    private int itemLayout;
    private boolean withCheckBoxes = false;

    public ContactRecyclerAdapter(ArrayList<Contact> contacts,
                                  ContactsRecyclerClickListener contactRecyclerClickListener)
    {
        mContacts = contacts;
        mFilteredContacts = contacts;
        itemLayout = R.layout.layout_contact_list_item;
        mContactRecyclerClickListener = contactRecyclerClickListener;
    }

    public ArrayList<Contact> getCheckedContacts() {
        return mCheckedContacts;
    }

    public boolean isWithCheckBoxes() {
        return withCheckBoxes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        final ViewHolder holder = new ViewHolder(view, mContactRecyclerClickListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ((ViewHolder)holder).contactName.setText(mFilteredContacts.get(position).getName());
        if(withCheckBoxes){
            ((ViewHolder)holder).checkBox
                    .setChecked(mCheckedContacts.contains(mFilteredContacts.get(position)));
        }
//        ((ViewHolder)holder).contactUsername.setText(mFilteredContacts.get(position).getUsername());
//        ((ViewHolder)holder).contactAvatar.setImage();
    }

    @Override
    public int getItemCount() {
        return mFilteredContacts.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener,
            View.OnLongClickListener
    {
        TextView contactName;
        ImageView contactAvatar;
        CheckBox checkBox;
        ContactsRecyclerClickListener clickListener;

        public ViewHolder(View itemView, ContactsRecyclerClickListener clickListener) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name);
            contactAvatar = itemView.findViewById(R.id.contact_avatar);
            checkBox = itemView.findViewById(R.id.check);
            if(withCheckBoxes){
                checkBox.setVisibility(View.VISIBLE);
            }
            else{
                checkBox.setVisibility(View.GONE);
            }
            this.clickListener = clickListener;
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(withCheckBoxes){
                CheckBox checkBox = v.findViewById(R.id.check);
                int adapterPosition = getAdapterPosition();
                checkBox.setChecked(!checkBox.isChecked());
                if(checkBox.isChecked()){
                    mCheckedContacts.add(mContacts.get(adapterPosition));
                }
                else{
                    mCheckedContacts.remove(mContacts.get(adapterPosition));
                }
            }
            clickListener.onContactSelected(getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            if (withCheckBoxes){
                checkBox.setVisibility(View.GONE);
                withCheckBoxes = false;
            }
            else {
                checkBox.setVisibility(View.VISIBLE);
                withCheckBoxes = true;
            }
            clickListener.onContactLongClick(getAdapterPosition());
            return false;
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
        void onContactLongClick(int position);
    }
}
