package com.example.kit.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class UChatroom implements Parcelable {
    private String display_name;
    private String chatroom_id;
    private String last_message;
    private Date time_last_sent;
    private boolean read_last_message;
    private boolean group;
    private int numUsers;


    public UChatroom(String display_name, String chatroom_id, boolean isGroup) {
        this.display_name = display_name;
        this.chatroom_id = chatroom_id;
        last_message = "";
        time_last_sent = new Date();
        read_last_message = false;
        this.group = isGroup;
        this.numUsers = 2;
    }

    public UChatroom(String display_name, String chatroom_id, boolean isGroup, int numUsers) {
        this.display_name = display_name;
        this.chatroom_id = chatroom_id;
        last_message = "";
        time_last_sent = new Date();
        read_last_message = false;
        this.group = isGroup;
        this.numUsers = numUsers;
        }



    public UChatroom() {}

    public UChatroom(Parcel in) {
        display_name = in.readString();
        chatroom_id = in.readString();
        last_message = in.readString();
        time_last_sent = new Date(in.readLong());
        read_last_message = in.readInt() == 1;
        group = in.readInt() == 1;
        numUsers = in.readInt();
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

    public String getLast_message() {
        return last_message;
    }

    public void setLast_message(String last_message) {
        this.last_message = last_message;
    }

    public Date getTime_last_sent() {
        return time_last_sent;
    }

    public void setTime_last_sent(Date time_last_sent) {
        this.time_last_sent = time_last_sent;
    }

    public boolean isRead_last_message() {
        return read_last_message;
    }

    public void setRead_last_message(boolean read_last_message) {
        this.read_last_message = read_last_message;
    }

    public boolean isGroup() {
        return group;
    }

    public int getNumUsers() {
        return numUsers;
    }

    public void setNumUsers(int numUsers) {
        this.numUsers = numUsers;
    }

    @Override
    public String toString() {
        return "UChatroom{" +
                "display_name='" + display_name + '\'' +
                ", chatroom_id='" + chatroom_id + '\'' +
                ", last_message='" + last_message + '\'' +
                ", time_last_sent='" + time_last_sent.getTime() + '\'' +
                ", read_last_message='" + read_last_message + '\'' +
                ", isGroup='" + group + '\'' +
                ", numUsers='" + numUsers + '\'' +
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
        dest.writeString(last_message);
        dest.writeLong(time_last_sent.getTime());
        dest.writeInt(read_last_message ? 1:0);
        dest.writeInt(group ? 1:0);
        dest.writeInt(numUsers);
    }

}


