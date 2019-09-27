package com.example.kit.adapters;

import androidx.annotation.NonNull;

import android.util.Log;
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

    private static final String TAG = "ContactRecyclerAdapter";

    private ArrayList<Contact> mContacts = new ArrayList<>();
    private ArrayList<Contact> mFilteredContacts = new ArrayList<>();
    private ArrayList<Contact> mCheckedContacts = new ArrayList<>();
    private ContactsRecyclerClickListener mContactRecyclerClickListener;
    private int itemLayout;
    private boolean withCheckBoxes = false;

    public ContactRecyclerAdapter(ArrayList<Contact> contacts,
                                  ContactsRecyclerClickListener contactRecyclerClickListener,
                                  int type)
    {
        mContacts = contacts;
        mFilteredContacts = contacts;
        itemLayout = type;
        mContactRecyclerClickListener = contactRecyclerClickListener;
    }

    public ArrayList<Contact> getCheckedContacts() {
        return mCheckedContacts;
    }

    public boolean isWithCheckBoxes() {
        return withCheckBoxes;
    }

    public void setWithCheckBoxes(boolean withCheckBoxes) {
        this.withCheckBoxes = withCheckBoxes;
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
        holder.contactName.setText(mFilteredContacts.get(position).getName());
        if (itemLayout == R.layout.layout_contact_list_item) {
            if (withCheckBoxes) {
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox
                        .setChecked(mCheckedContacts.contains(mFilteredContacts.get(position)));
            } else {
                holder.checkBox.setVisibility(View.GONE);
                holder.checkBox.setChecked(false);
            }
        }
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
            this.clickListener = clickListener;
            switch (itemLayout){
                case R.layout.layout_contact_list_item:{
                    checkBox = itemView.findViewById(R.id.check);
                    if(withCheckBoxes){
                        checkBox.setVisibility(View.VISIBLE);
                        checkBox.setChecked(mCheckedContacts.contains(mFilteredContacts.get(getAdapterPosition())));
                    }
                    else{
                        checkBox.setVisibility(View.GONE);
                        checkBox.setChecked(false);
                    }
                    itemView.setOnClickListener(this);
                    itemView.setOnLongClickListener(this);
                    break;
                }
                case R.layout.layout_requests_list_item:{
                    itemView.findViewById(R.id.accept).setOnClickListener(this);
                    itemView.findViewById(R.id.decline).setOnClickListener(this);
                    break;
                }
                case R.layout.layout_pending_list_item:{
                    itemView.findViewById(R.id.delete_request).setOnClickListener(this);
                    break;
                }
            }
        }

        @Override
        public void onClick(View v) {
            Log.d(TAG, "onClick: Entered");
            int adapterPosition = getAdapterPosition();
            switch (itemLayout){
                case R.layout.layout_contact_list_item:{
                    if(withCheckBoxes){
                        checkBox.setChecked(!checkBox.isChecked());
                        if(checkBox.isChecked()){
                            mCheckedContacts.add(mContacts.get(adapterPosition));
                        }
                        else{
                            mCheckedContacts.remove(mContacts.get(adapterPosition));
                        }
                        if(mCheckedContacts.isEmpty())
                        {
                            changeView();
                        }
                    }
                    else {
                        clickListener.onContactSelected(getAdapterPosition());
                    }
                    break;
                }
                case R.layout.layout_requests_list_item:{
                    if(v.getId() == R.id.accept){
                        clickListener.onAcceptSelected(adapterPosition);
                    }
                    else if(v.getId() == R.id.decline){
                        clickListener.onRejectSelected(adapterPosition);
                    }
                    break;
                }
                case R.layout.layout_pending_list_item:{
                    if(v.getId() == R.id.delete_request){
                        clickListener.onDeleteSelected(adapterPosition);
                    }
                }
            }


        }

        @Override
        public boolean onLongClick(View v) {
            Log.d(TAG, "onLongClick: Entered");
            changeView();
            return true;
        }

        public void changeView(){
            if(withCheckBoxes){
                withCheckBoxes = false;
                mCheckedContacts = new ArrayList<>();
            }
            else{
                withCheckBoxes = true;
                checkBox.setChecked(true);
                mCheckedContacts.add(mContacts.get(getAdapterPosition()));
            }
            clickListener.onContactLongClick(getAdapterPosition());
        }

        public CheckBox getCheckBox() {
            return checkBox;
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
            }
        };
    }

    public interface ContactsRecyclerClickListener {
        void onContactSelected(int position);
        void onContactLongClick(int position);
        void onAcceptSelected(int position);
        void onRejectSelected(int position);
        void onDeleteSelected(int position);
    }
}
