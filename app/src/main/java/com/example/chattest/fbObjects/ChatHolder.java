package com.example.chattest.fbObjects;

import java.util.Date;

public class ChatHolder {
    public String type, dialogId, chatName, text, userIdAuthor, userIdMessage, avaUrl;
    public String cryptionType;
    public long time;


    public ChatHolder() {

    }

    public ChatHolder(String type, String dialogId, String chatName, String text, String userIdAuthor, String userIdMessage, String avaUrl, String cryptionType) {
        this.type = type;
        this.dialogId = dialogId;
        this.chatName = chatName;
        this.text = text;
        this.userIdAuthor = userIdAuthor;
        this.userIdMessage = userIdMessage;
        this.time = new Date().getTime();
        this.avaUrl = avaUrl;
        this.cryptionType = cryptionType;
    }

    public ChatHolder(String type, String dialogId, String chatName, String text, String userIdAuthor, String userIdMessage, long time, String avaUrl, String cryptionType) {
        this.type = type;
        this.dialogId = dialogId;
        this.chatName = chatName;
        this.text = text;
        this.userIdAuthor = userIdAuthor;
        this.userIdMessage = userIdMessage;
        this.time = time;
        this.avaUrl = avaUrl;
        this.cryptionType = cryptionType;
    }
}
