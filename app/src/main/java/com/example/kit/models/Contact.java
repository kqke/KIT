package com.example.kit.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Parcelable

    {

    private String name;
    private String username;
    private String avatar;
    private String cid;


    public Contact(String name, String username, String avatar, String cid) {
        this.name = name;
        this.username = username;
        this.avatar = avatar;
        this.cid = cid;
    }

    public Contact() {

    }

    public Contact(Parcel in) {
        name = in.readString();
        username = in.readString();
        avatar = in.readString();
        cid = in.readString();
    }

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

        @Override
    public String toString() {
        return "Contact{" +
                "name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", avatar='" + avatar + '\'' +
                ", cid='" + cid + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(username);
        dest.writeString(avatar);
        dest.writeString(cid);
    }
}
