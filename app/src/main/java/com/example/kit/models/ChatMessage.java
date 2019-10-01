package com.example.kit.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;

public class ChatMessage implements Serializable {

    private User user;
    private String message;
    private String message_id;
    private boolean invite;
    private @ServerTimestamp Date timestamp;
    private Date meeting_time;
    private boolean expired;


    public ChatMessage(User user,
                       String message,
                       String message_id,
                       Date timestamp,
                       Date meeting_time,
                       boolean expired) {
        this.user = user;
        this.message = message;
        this.message_id = message_id;
        this.timestamp = timestamp;
        this.invite = false;
        this.meeting_time = meeting_time;
        this.expired = expired;
    }

    public ChatMessage() {

    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isInvite() {
        return invite;
    }

    public void setInvite(boolean invite) {
        this.invite = invite;
    }

    public Date getMeeting_time() {
        return meeting_time;
    }

    public void setMeeting_time(Date meeting_time) {
        this.meeting_time = meeting_time;
    }

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "user=" + user +
                ", message='" + message + '\'' +
                ", message_id='" + message_id + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
