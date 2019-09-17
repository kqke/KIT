package com.example.kit.models;

import android.os.Parcel;
import android.os.Parcelable;



public class Chatroom implements Parcelable {
    private String user1;
    private String user2;
    private String chatroom_id;
    private String group_name;
    private boolean isGroup;


    public Chatroom(String user1, String user2, String chatroom_id) {
        this.user1 = user1;
        this.user2 = user2;
        this.chatroom_id = chatroom_id;
        this.group_name = "default";
        this.isGroup = false;

    }

    public Chatroom(String chatroom_id, String group_name) {
        this.user1 = "";
        this.user2 = "";
        this.chatroom_id = chatroom_id;
        this.group_name = group_name;
        this.isGroup = true;

    }

    public Chatroom() {

    }

    protected Chatroom(Parcel in) {
        chatroom_id = in.readString();
        group_name = in.readString();
    }

    public static final Creator<Chatroom> CREATOR = new Creator<Chatroom>() {
        @Override
        public Chatroom createFromParcel(Parcel in) {
            return new Chatroom(in);
        }

        @Override
        public Chatroom[] newArray(int size) {
            return new Chatroom[size];
        }
    };


    public String getUser1() {
        return user1;
    }

    public void setUser1(String user1) {
        this.user1 = user1;
    }

    public String getUser2() {
        return user2;
    }

    public void setUser2(String user2) {
        this.user2 = user2;
    }

    public String getChatroom_id() {
        return chatroom_id;
    }

    public void setChatroom_id(String chatroom_id) {
        this.chatroom_id = chatroom_id;
    }

    public String getGroup_name() {
        return group_name;
    }

    public void setGroup_name(String group_name) {
        this.group_name = group_name;
    }

    public boolean isGroup() {
        return isGroup;
    }

    @Override
    public String toString() {
        return "Chatroom{" +
                "user1='" + user1 + '\'' +
                ", user2='" + user2 + '\'' +
                ", chatroom_id='" + chatroom_id + '\'' +
                ", group_name='" + group_name + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(user1);
        dest.writeString(user2);
        dest.writeString(chatroom_id);
    }

}
