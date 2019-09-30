package com.example.kit.models;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class UserLocation implements Parcelable{

    private User user;
    private GeoPoint geo_point;
    private double latitude, longitude;
    private @ServerTimestamp Date timestamp;
    private boolean incognito;

    public UserLocation(User user, GeoPoint geo_point, Date timestamp) {
        this.user = user;
        this.geo_point = geo_point;
        this.timestamp = timestamp;
        incognito = false;
    }

    public UserLocation() {
    }

    protected UserLocation(Parcel in) {
        user = in.readParcelable(User.class.getClassLoader());
        latitude = in.readDouble();
        longitude = in.readDouble();
        geo_point = new GeoPoint(latitude, longitude);
        incognito = in.readInt() == 1;
    }

    public static final Creator<UserLocation> CREATOR = new Creator<UserLocation>() {
        @Override
        public UserLocation createFromParcel(Parcel in) {
            return new UserLocation(in);
        }

        @Override
        public UserLocation[] newArray(int size) {
            return new UserLocation[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(user, flags);
        latitude = geo_point.getLatitude();
        longitude = geo_point.getLongitude();
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeInt(incognito ? 1:0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public GeoPoint getGeo_point() {
        return geo_point;
    }

    public void setGeo_point(GeoPoint geo_point) {
        this.geo_point = geo_point;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setIncognito(boolean incognito) {
        this.incognito = incognito;
    }

    public boolean isIncognito() {
        return incognito;
    }

    @Override
    public String toString() {
        return "UserLocation{" +
                "user=" + user +
                ", geo_point=" + geo_point +
                ", timestamp=" + timestamp +
                ", incognito= " + incognito +
                '}';
    }

}