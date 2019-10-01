package com.example.kit.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.kit.R;
import com.example.kit.Constants;
import com.example.kit.models.Contact;

public class RequestsDialogFragment extends DialogFragment {
    private static final String TAG = "RequestsDialogFragment";

    public interface OnInputSelected{
        void requestAccepted(String display_name, Contact contact);
        void requestRemoved(Contact contact);
    }
    public OnInputSelected mOnInputSelected;

    //widgets
    private EditText mInput;
    private TextView mActionOk, mActionCancel;
    private int type;
    private Contact contact;
    private Activity mActivity;
    private DBGeoFragment requestsFragment;

    public RequestsDialogFragment(int type, Contact contact, Activity activity, DBGeoFragment requestsFragment){
        super();
        this.mActivity = activity;
        this.requestsFragment = requestsFragment;
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
        switch (this.type){
            case Constants.GET_ACCEPT_REQUEST:
                title.setText("Choose The Name to Display For This Contact");
                mInput.setHint("Enter name here...");
                mInput.setVisibility(View.VISIBLE);
                mActionOk.setText("ACCEPT");
                break;

            case Constants.GET_REMOVE_REQUEST:
                title.setText("Are You Sure You Want to Remove This Request?");
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

                switch (type) {
                    case Constants.GET_ACCEPT_REQUEST:
                        String input = mInput.getText().toString();
                        if (!input.equals("")) {
                            mOnInputSelected.requestAccepted(input, contact);
                        } else {
                            Toast.makeText(getActivity(), "Enter a name", Toast.LENGTH_SHORT).show();
                        }
//                            mOnInputSelected.requestAccepted(input, contact);
                        break;



                    case Constants.GET_REMOVE_REQUEST:
                        mOnInputSelected.requestRemoved(contact);


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
            mOnInputSelected = (OnInputSelected) requestsFragment;
        }catch (ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException : " + e.getMessage() );
        }
    }
}