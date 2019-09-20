package com.example.kit.ui;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.kit.R;

public class RequestsFragment extends Fragment {


    public RequestsFragment() {
        // Required empty public constructor
    }

    public static RequestsFragment newInstance() {
        RequestsFragment fragment = new RequestsFragment();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_requests, container, false);
        Button buttonInFragment1 = rootView.findViewById(R.id.button);
        buttonInFragment1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), "button in fragment 1", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    public String getName(){
        return "Requests";
    }
}

