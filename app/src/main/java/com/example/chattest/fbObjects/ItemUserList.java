package com.example.chattest.fbObjects;

public class ItemUserList {
    public String userName;
    public String userId;
    public String avaUrl;
    public boolean selected;

    public ItemUserList() {

    }

    public ItemUserList(String userName, String userId, String avaUrl, boolean selected) {
        this.userName = userName;
        this.userId = userId;
        this.avaUrl = avaUrl;
        this.selected = selected;
    }

    public ItemUserList(String userName, String userId, String avaUrl) {
        this.userName = userName;
        this.userId = userId;
        this.avaUrl = avaUrl;
        this.selected = false;
    }

}
