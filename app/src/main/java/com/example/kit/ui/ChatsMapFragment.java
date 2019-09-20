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

import org.jetbrains.annotations.NotNull;

public class ChatsMapFragment extends Fragment {

    //TODO
    // modify title bar
    //TODO
    // maybe add map to navi_bar

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    public ChatsMapFragment() {
        // Required empty public constructor
    }

    public static ChatsMapFragment newInstance() {
        return new ChatsMapFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_viewpager, container, false);
        initView(view);
        return view;
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(@NotNull View v){
        ChatMapViewPagerAdapter mAdapter = new ChatMapViewPagerAdapter(getChildFragmentManager());
        ViewPager mPager = v.findViewById(R.id.view_pager);
        mPager.setAdapter(mAdapter);
    }

    /*
    ----------------------------- ViewPagerAdapter ---------------------------------
    */
    public static class ChatMapViewPagerAdapter extends FragmentPagerAdapter {

        private String[] mTitles = new String[] {
                "Chats",
                "Map"
        };

        public ChatMapViewPagerAdapter(FragmentManager manager) {
            super(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        @NonNull
        public Fragment getItem(int position) {
            switch(position){
                case 0:
                    return ChatsFragment.newInstance();
                case 1:
                    return MapFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }
    }
}
