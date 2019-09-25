package com.example.kit.ui;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.kit.R;

public class ContactsRequestsPendingFragment extends Fragment {

    //TODO
    // requests fragment
    //TODO
    // pending fragment
    //TODO
    // update recycler item views to have functionality to accept/send/revoke pairing requests

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public ContactsRequestsPendingFragment() {
        // Required empty public constructor
    }

    public static ContactsRequestsPendingFragment newInstance() {
        return new ContactsRequestsPendingFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_viewpager, container, false);
        initView(view);
        return view;
    }

    /*
    ----------------------------- iit ---------------------------------
    */

    private void initView(View v){
        ContactsViewPagerAdapter mAdapter = new ContactsViewPagerAdapter(getChildFragmentManager());
        ViewPager mPager = v.findViewById(R.id.view_pager);
        mPager.setAdapter(mAdapter);
    }

    /*
    ----------------------------- ViewPagerAdapter ---------------------------------
    */
    public static class ContactsViewPagerAdapter extends FragmentPagerAdapter {

        private String[] mTitles = new String[] {
                "Contacts",
                "Requests",
                "Pending"
        };

        private ContactsViewPagerAdapter(FragmentManager manager) {
            super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            switch(position){
                case 0:
                    return ContactsFragment.newInstance();
                case 1:
                    return RequestsFragment.newInstance();
                case 2:
                    return PendingFragment.newInstance();
            }
            return null; // not reachable
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }
    }
}
