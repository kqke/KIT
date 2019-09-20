package com.example.kit.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.kit.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity
{

    //TODO
    // actionbar functionality (fix the strings shown) + additional menu options if necessary
    //TODO
    // try to move the location check to here
    //TODO
    // should we make this the launcher activity that redirects to login if necessary?
    //TODO
    // consider adding map to bottom navi bar
    //TODO
    // settings fragment
    //TODO
    // profile fragment
    //TODO
    // search fragment?
    //TODO
    // add meeting invites as a new message type
    //TODO
    // make a superclass for MapFragment & UserListFragment
    //TODO
    // Login activity isn't perfect yet
    //TODO
    // fix ImageListFragment (as a part of ProfileFragment)
    //TODO
    // does the AddContactsActivity only contain an AlertDialog?
    // if so, why is it an Activity? should be more than a dialog, fragment probably
    //TODO
    // what is ContactMessageActivity?
    // TODO
    //  merge activity_login.xml & activity_username.xml
    //TODO
    //  transitions between activities

    //Tag
    private static final String TAG = "MainActivity";

    //Fragments
//    private static final String SEARCH_FRAG = "SEARCH_FRAG";
    private static final String CHATS_MAP_FRAG = "CHATS_MAP_FRAG";
    private static final String CONATCTS_FRAG = "CONTACTS_FRAG";
    private static final String PROFLE_FRAG = "PROFILE_FRAG";
    private static final String SETTINGS_FRAG = "SETTINGS_FRAG";

    /*
    ----------------------------- Lifecycle ---------------------------------
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initMessageService();
    }

    /*
    ----------------------------- init ---------------------------------
    */

    private void initView(){
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar)findViewById(R.id.upper_toolbar));
        setTitle(R.string.fui_default_toolbar_title);
        initNavigationBar();
    }

    private void initNavigationBar() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navi_bar);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, ChatsMapFragment.newInstance(), CHATS_MAP_FRAG)
                .commit();
        bottomNav.setOnNavigationItemSelectedListener
                (new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            //TODO
                            // maybe add map to navi_bar
//                            case R.id.action_search:{
//                                replaceFragment(SearchFragment.newInstance(), SEARCH_FRAG);
//                                return true;
//                            }
                            case R.id.action_chats:{
                                //TODO
                                // highlight chats action on navibar
                                // basically do nothing
                                replaceFragment(ChatsMapFragment.newInstance(), CHATS_MAP_FRAG);
                                setTitle(R.string.fui_default_toolbar_title);
                                return true;
                            }
                            case R.id.action_contacts:{
                                replaceFragment(ContactsRequestsPendingFragment.newInstance(), CONATCTS_FRAG);
                                setTitle(R.string.title_activity_contacts);
                                return true;
                            }
                            case R.id.action_profile:{
                                replaceFragment(ProfileFragment.newInstance(), PROFLE_FRAG);
                                setTitle(R.string.action_profile);
                                return true;
                            }
                            case R.id.action_settings:{
                                replaceFragment(SettingsFragment.newInstance(), SETTINGS_FRAG);
                                setTitle(R.string.common_google_play_services_update_title);
                                return true;
                            }
                        }
                        return false;
                    }
                });
    }

    private void replaceFragment(Fragment newFragment, String tag) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, newFragment, tag).commit();
    }

    private void initMessageService(){
        Intent intent = new Intent("com.example.kit.services.MyFirebaseMessagingService");
        intent.setPackage("com.example.kit");
        this.startService(intent);
    }
}