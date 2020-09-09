package com.example.chattest.fbObjects;

public class AddUser {
    public String userId, userName, avaUrl, publicKey, privateKey;
    public long lastOnline;

    public AddUser() {

    }

    public AddUser(String userId, String userName, String avaUrl, String publicKey, String privateKey, long lastOnline) {
        this.userId = userId;
        this.userName = userName;
        this.avaUrl = avaUrl;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.lastOnline = lastOnline;
    }

}
