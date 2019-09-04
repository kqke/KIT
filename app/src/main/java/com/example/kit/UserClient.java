package com.example.kit;

import android.app.Application;

import com.example.kit.models.User;


public class UserClient extends Application {

    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
