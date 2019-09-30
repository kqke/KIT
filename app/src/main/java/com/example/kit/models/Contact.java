package com.example.kit.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;


public class Contact implements Parcelable

    {

    private String name;
    private String username;
    private String avatar;
    private String cid;
    private String status = "Hey, I'm using KIT!";
    private Date last_sent;
    private boolean inArea;


    public Contact(String name, String username, String avatar, String cid, String status) {
        this.name = name;
        this.username = username;
        this.avatar = avatar;
        this.cid = cid;
        this.status = status;
        last_sent = new Date(System.currentTimeMillis() - 3600000);
        inArea = false;
    }

    public Contact() {

    }

    public Contact(Parcel in) {
        name = in.readString();
        username = in.readString();
        avatar = in.readString();
        cid = in.readString();
        status = in.readString();
        last_sent = new Date(in.readLong());
        inArea = in.readInt() == 1;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLast_sent() {
    return last_sent;
}

    public void setLast_sent(Date last_sent) {
        this.last_sent = last_sent;
    }

    public boolean isInArea() {
        return inArea;
    }

    public void setInArea(boolean notification_sent) {
        this.inArea = notification_sent;
    }

        @Override
    public String toString() {
        return "Contact{" +
                "name='" + name + '\'' +
                ", username='" + username + '\'' +
                ", avatar='" + avatar + '\'' +
                ", cid='" + cid + '\'' +
                ", status='" + status + '\'' +
                ", last_sent='" + last_sent + '\'' +
                ", inArea='" + inArea + '\'' +
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
        dest.writeString(status);
        dest.writeLong(last_sent.getTime());
        dest.writeInt(inArea ? 1:0);
    }
}
