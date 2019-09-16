package com.example.kit.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Username implements Parcelable {
    private String user_id;

    public Username(String user_id){
        this.user_id = user_id;
    }

    public Username() {

    }

    protected Username(Parcel in) {
        user_id = in.readString();
    }

    public static final Creator<Username> CREATOR = new Creator<Username>() {
        @Override
        public Username createFromParcel(Parcel in) {
            return new Username(in);
        }

        @Override
        public Username[] newArray(int size) {
            return new Username[size];
        }
    };


    public static Creator<Username> getCREATOR() {
        return CREATOR;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }


    @Override
    public String toString() {
        return "Username{" +
                "user_id='" + user_id + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(user_id);
    }
}

