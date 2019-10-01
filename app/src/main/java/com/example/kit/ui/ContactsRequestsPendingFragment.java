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

import java.util.ArrayList;
import java.util.List;

public class ContactsRequestsPendingFragment extends Fragment {

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
    ----------------------------- init ---------------------------------
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

        private List<Fragment> fragments;

        private ContactsViewPagerAdapter(FragmentManager manager) {
            super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.fragments = new ArrayList<>();
            this.fragments.add(0, ContactsFragment.newInstance());
            this.fragments.add(1, RequestsFragment.newInstance());
            this.fragments.add(2, PendingFragment.newInstance());
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            return fragments.get(position);
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
