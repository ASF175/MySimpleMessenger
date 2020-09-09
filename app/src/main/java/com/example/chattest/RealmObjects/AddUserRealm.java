package com.example.chattest.RealmObjects;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class AddUserRealm extends RealmObject {
    public String userName, avaUrl, publicKey;
    public long lastOnline;
    @PrimaryKey
    public String userId;

    public AddUserRealm() {
    }

    public AddUserRealm(String userName, String avaUrl, String publicKey, String userId, long lastOnline) {
        this.userName = userName;
        this.avaUrl = avaUrl;
        this.publicKey = publicKey;
        this.userId = userId;
        this.lastOnline = lastOnline;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public long getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(long lastOnline) {
        this.lastOnline = lastOnline;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAvaUrl() {
        return avaUrl;
    }

    public void setAvaUrl(String avaUrl) {
        this.avaUrl = avaUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
