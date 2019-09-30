package com.example.kit.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.models.Contact;
import com.example.kit.models.User;

public class ContactsDialogFragment extends DialogFragment {
    private static final String TAG = "ContactsDialogFragment";

    public interface OnInputSelected{
        void addContact(String display_name, User contactUser);
        void search(String username);
        void remove(Contact contact);
    }
    public ContactsDialogFragment.OnInputSelected mOnInputSelected;

    //widgets
    private EditText mInput;
    private TextView mActionOk, mActionCancel;
    private int type;
    private User contactUser;
    private Contact contact;
    private Activity mActivity;
    private DBGeoFragment contactsFragment;

    public ContactsDialogFragment(int type, User contactUser, Activity activity, DBGeoFragment contactsFragment){
        super();
        this.mActivity = activity;
        this.contactsFragment = contactsFragment;
        this.type = type;
        this.contactUser = contactUser;
    }

    public ContactsDialogFragment(int type, Contact contact, Activity activity, DBGeoFragment contactsFragment){
        super();
        this.mActivity = activity;
        this.contactsFragment = contactsFragment;
        this.type = type;
        this.contact = contact;
    }


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_requests, container, false);
        TextView title = view.findViewById(R.id.req_dialog_heading);
        mActionOk = view.findViewById(R.id.action_ok);
        mInput = view.findViewById(R.id.req_dialog_input);
        mInput.setVisibility(View.VISIBLE);

        switch (type){
            case Constants.GET_DISPLAY_NAME:
                title.setText("Choose The Name to Display For This Contact");
                mActionOk.setText("ACCEPT");
                break;

            case Constants.GET_SEARCH_REQUEST:
                title.setText("Enter The Username of The User You Would Like to Add");
                mActionOk.setText("ADD");
                break;

            case Constants.GET_REMOVE_REQUEST:
                mInput.setVisibility(View.INVISIBLE);
                title.setText("Are You Sure You Want to Remove "  + contact.getName() + "?");
                mActionOk.setText("REMOVE");
        }




        mActionCancel = view.findViewById(R.id.action_cancel);

        mActionCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: closing dialog");
                getDialog().dismiss();
            }
        });

        mActionOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: capturing input.");
                String input = mInput.getText().toString();
                if (input.equals("")){
                    Toast.makeText(getActivity(), "Enter a name", Toast.LENGTH_SHORT).show();
                } else {

                    switch (type) {
                        case Constants.GET_DISPLAY_NAME:

                            mOnInputSelected.addContact(input, contactUser);
                            break;


                        case Constants.GET_SEARCH_REQUEST:
                            mOnInputSelected.search(input);
                            break;

                        case Constants.GET_REMOVE_REQUEST:
                            mOnInputSelected.remove(contact);
                            break;

                    }
                }


                getDialog().dismiss();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            mOnInputSelected = (ContactsDialogFragment.OnInputSelected) contactsFragment;
        }catch (ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException : " + e.getMessage() );
        }
    }

}
