package com.example.chattest.RealmObjects;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class AdvancedMessageRealm extends RealmObject {
    public String type;
    public String name;
    public String mess;
    public String userId;
    public String dialogId;
    public String avaUrl;
    public long time;
    public boolean readed;

    @PrimaryKey
    public String messageId;


    public AdvancedMessageRealm() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
    }

    public AdvancedMessageRealm(String type, String name, String mess, String messageId, String userId, String dialogId, String avaUrl, boolean readed) {
        this.type = type;
        this.name = name;
        this.mess = mess;
        this.messageId = messageId;
        this.userId = userId;
        this.dialogId = dialogId;
        this.time = new Date().getTime();
        this.avaUrl = avaUrl;
        this.readed = readed;
    }

    public AdvancedMessageRealm(String type, String name, String mess, String messageId, String userId, String dialogId, long time, String avaUrl, boolean readed) {
        this.type = type;
        this.name = name;
        this.mess = mess;
        this.messageId = messageId;
        this.userId = userId;
        this.dialogId = dialogId;
        this.time = time;
        this.avaUrl = avaUrl;
        this.readed = readed;
    }

    public boolean isReaded() {
        return readed;
    }

    public void setReaded(boolean readed) {
        this.readed = readed;
    }

    public String getAvaUrl() {
        return avaUrl;
    }

    public void setAvaUrl(String avaUrl) {
        this.avaUrl = avaUrl;
    }

    public String getDialogId() {
        return dialogId;
    }

    public void setDialogId(String dialogId) {
        this.dialogId = dialogId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMess() {
        return mess;
    }

    public void setMess(String mess) {
        this.mess = mess;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
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
}
