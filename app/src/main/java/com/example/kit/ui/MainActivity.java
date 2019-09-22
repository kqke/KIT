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
    // chat crashes on orientation change and locations are not updated properly
    //TODO
    // launch different fragments for private chats (no user list)
    // and for group chats (with user list)
    //TODO
    // actionbar functionality - additional menu options if necessary
    //TODO
    // try to move the location check to here
    //TODO
    // should we make this the launcher activity that redirects to login if necessary?
    //TODO
    // settings fragment
    //TODO
    // profile fragment
    //TODO
    // search fragment?
    //TODO
    // add meeting invites as a new message type
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
    //TODO
    // top right button on map not working

    //Tag
    private static final String TAG = "MainActivity";

    //Fragments
//    private static final String SEARCH_FRAG = "SEARCH_FRAG";
    private static final String CHATS_FRAG = "CHATS_FRAG";
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
        setTitle(R.string.fragment_chats);
        initNavigationBar();
    }

    private void initNavigationBar() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navi_bar);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_container, ChatsFragment.newInstance(), CHATS_FRAG)
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
                                replaceFragment(ChatsFragment.newInstance(), CHATS_FRAG);
                                setTitle(R.string.fragment_chats);
                                return true;
                            }
                            case R.id.action_contacts:{
                                replaceFragment(ContactsRequestsPendingFragment.newInstance(), CONATCTS_FRAG);
                                setTitle(R.string.fragment_contacts);
                                return true;
                            }
                            case R.id.action_profile:{
                                replaceFragment(ProfileFragment.newInstance(), PROFLE_FRAG);
                                setTitle(R.string.fragment_profile);
                                return true;
                            }
                            case R.id.action_settings:{
                                replaceFragment(SettingsFragment.newInstance(), SETTINGS_FRAG);
                                setTitle(R.string.fragment_settings);
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