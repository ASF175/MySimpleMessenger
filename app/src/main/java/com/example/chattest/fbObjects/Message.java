package com.example.chattest.fbObjects;

import java.util.Date;

public class Message {
    public String type;
    public String mess;
    public String userId;
    public String encryptedAES, sign;
    public long time;

    public Message() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public Message(String type, String mess, String userId, String encryptedAES, String sign) {
        this.type = type;
        this.mess = mess;
        this.userId = userId;
        this.time = new Date().getTime();
        this.encryptedAES = encryptedAES;
        this.sign = sign;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMess() {
        return mess;
    }

    public void setMess(String mess) {
        this.mess = mess;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setTime() {
        this.time = new Date().getTime();
    }

    public String getEncryptedAES() {
        return encryptedAES;
    }

    public void setEncryptedAES(String encryptedAES) {
        this.encryptedAES = encryptedAES;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}
