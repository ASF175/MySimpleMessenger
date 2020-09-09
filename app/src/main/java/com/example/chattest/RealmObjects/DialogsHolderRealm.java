package com.example.chattest.RealmObjects;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DialogsHolderRealm extends RealmObject {
    public String type, chatName, userName, text, userIdOpponent, userIdAuthor, userIdMessage;
    public String avaUri, lastMessageUri;
    public String cryptionType;
    public String decryptedAES;
    public String sign;
    public String canWrite;
    public long time, unreadedMessagesCount;
    @PrimaryKey
    public String dialogId;

    public DialogsHolderRealm(String type, String dialogId, String chatName, String userName, String text, String userIdOpponent, String userIdAuthor, long time, String userIdMessage, String avaUri, String cryptionType, String decryptedAES, String sign, long unreadedMessagesCount, String lastMessageUri) {
        this.type = type;
        this.dialogId = dialogId;
        this.chatName = chatName;
        this.userName = userName;
        this.text = text;
        this.userIdOpponent = userIdOpponent;
        this.userIdAuthor = userIdAuthor;
        this.time = time;
        this.userIdMessage = userIdMessage;
        this.avaUri = avaUri;
        this.cryptionType = cryptionType;
        this.decryptedAES = decryptedAES;
        this.sign = sign;
        this.canWrite = "true";
        this.lastMessageUri = lastMessageUri;
        //this.unreadedMessagesCount = unreadedMessagesCount;
    }

    public DialogsHolderRealm() {
    }

    public String getLastMessageUri() {
        return lastMessageUri;
    }

    public void setLastMessageUri(String lastMessageUri) {
        this.lastMessageUri = lastMessageUri;
    }

    public long getUnreadedMessagesCount() {
        return unreadedMessagesCount;
    }

    public void setUnreadedMessagesCount(long unreadedMessagesCount) {
        this.unreadedMessagesCount = unreadedMessagesCount;
    }

    public String getUserIdAuthor() {
        return userIdAuthor;
    }

    public void setUserIdAuthor(String userIdAuthor) {
        this.userIdAuthor = userIdAuthor;
    }

    public String getCanWrite() {
        return canWrite;
    }

    public void setCanWrite(String canWrite) {
        this.canWrite = canWrite;
    }

    public String getCryptionType() {
        return cryptionType;
    }

    public void setCryptionType(String cryptionType) {
        this.cryptionType = cryptionType;
    }

    public String getDecryptedAES() {
        return decryptedAES;
    }

    public void setDecryptedAES(String decryptedAES) {
        this.decryptedAES = decryptedAES;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getUserIdMessage() {
        return userIdMessage;
    }

    public void setUserIdMessage(String userIdMessage) {
        this.userIdMessage = userIdMessage;
    }

    public String getAvaUri() {
        return avaUri;
    }

    public void setAvaUri(String avaUri) {
        this.avaUri = avaUri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDialogId() {
        return dialogId;
    }

    public void setDialogId(String dialogId) {
        this.dialogId = dialogId;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUserIdOpponent() {
        return userIdOpponent;
    }

    public void setUserIdOpponent(String userIdOpponent) {
        this.userIdOpponent = userIdOpponent;
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
