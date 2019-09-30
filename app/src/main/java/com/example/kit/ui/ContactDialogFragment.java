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

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.kit.Constants;
import com.example.kit.R;
import com.example.kit.models.Contact;

public class ContactDialogFragment extends DialogFragment {
    private static final String TAG = "ContactDialogFragment";

    public interface OnInputSelected{
        void removeContact(Contact contact);
        void removeRequest(Contact contact);
    }
    public ContactDialogFragment.OnInputSelected mOnInputSelected;

    //widgets
    private EditText mInput;
    private TextView mActionOk, mActionCancel;
    private int type;
    private Contact contact;
    private Activity mActivity;
    private DBGeoFragment contactFragment;



    public ContactDialogFragment(int type, Contact contact, Activity activity, DBGeoFragment contactFragment){
        super();
        this.mActivity = activity;
        this.contactFragment = contactFragment;
        this.type = type;
        this.contact = contact;
    }


    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_requests, container, false);
        TextView title = view.findViewById(R.id.req_dialog_heading);
        mActionOk = view.findViewById(R.id.action_ok);
        title.setText("Are You Sure You Want to Remove "  + contact.getName() + "?");
        mActionOk.setText("REMOVE");
        mInput = view.findViewById(R.id.req_dialog_input);
        mInput.setVisibility(View.INVISIBLE);




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
                switch (type) {
                    case Constants.GET_REMOVE_CONTACT:
                    Log.d(TAG, "onClick: capturing input.");
                    mOnInputSelected.removeContact(contact);
                    getDialog().dismiss();
                    break;

                    case Constants.GET_REMOVE_REQUEST:
                        Log.d(TAG, "onClick: capturing input.");
                        mOnInputSelected.removeRequest(contact);
                        getDialog().dismiss();
                }
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            mOnInputSelected = (ContactDialogFragment.OnInputSelected) contactFragment;
        }catch (ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException : " + e.getMessage() );
        }
    }

}
