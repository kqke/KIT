package com.example.kit.models;

import android.os.Parcel;
import android.os.Parcelable;

public class UChatroom implements Parcelable {
    private String display_name;
    private String chatroom_id;


    public UChatroom(String display_name, String chatroom_id) {
        this.display_name = display_name;
        this.chatroom_id = chatroom_id;
        }



    public UChatroom() {}

    protected UChatroom(Parcel in) {
        display_name = in.readString();
        chatroom_id = in.readString();
        }

    public static final Creator<UChatroom> CREATOR = new Creator<UChatroom>() {
        @Override
        public UChatroom createFromParcel(Parcel in) {
            return new UChatroom(in);
        }

        @Override
        public UChatroom[] newArray(int size) {
            return new UChatroom[size];
        }
    };


    public String getDisplay_name() {
        return display_name;
    }

    public void setDisplay_name(String display_name) {
        this.display_name = display_name;
    }

    public String getChatroom_id() {
        return chatroom_id;
    }

    public void setChatroom_id(String chatroom_id) {
        this.chatroom_id = chatroom_id;
    }


    @Override
    public String toString() {
        return "Chatroom{" +
                "display_name='" + display_name + '\'' +
                ", chatroom_id='" + chatroom_id + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(display_name);
        dest.writeString(chatroom_id);
    }

}


