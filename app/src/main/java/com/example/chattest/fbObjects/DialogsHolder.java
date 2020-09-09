package com.example.chattest.fbObjects;

import java.util.Date;

public class DialogsHolder {
    public String type, dialogId, chatName, text, userIdOpponent, userIdMessage, userIdAuthor;
    public String cryptionType, encryptedAES, sign;
    public String canWrite;
    public long time;

    public DialogsHolder(String type, String dialogId, String chatName, String text, String userIdOpponent, String userIdAuthor, String userIdMessage, String cryptionType, String encryptedAES, String sign) {
        this.type = type;
        this.dialogId = dialogId;
        this.chatName = chatName;
        this.text = text;
        this.userIdOpponent = userIdOpponent;
        this.userIdAuthor = userIdAuthor;
        this.time = new Date().getTime();
        this.userIdMessage = userIdMessage;
        this.cryptionType = cryptionType;
        this.encryptedAES = encryptedAES;
        this.sign = sign;
        this.canWrite = "true";
    }

    public DialogsHolder(String type, String dialogId, String chatName, String text, String userIdOpponent, String userIdAuthor, long time, String userIdMessage, String cryptionType, String encryptedAES, String sign) {
        this.type = type;
        this.dialogId = dialogId;
        this.chatName = chatName;
        this.text = text;
        this.userIdOpponent = userIdOpponent;
        this.userIdAuthor = userIdAuthor;
        this.time = time;
        this.userIdMessage = userIdMessage;
        this.cryptionType = cryptionType;
        this.encryptedAES = encryptedAES;
        this.sign = sign;
        this.canWrite = "true";
    }

    public DialogsHolder() {
    }

    public String getCryptionType() {
        return cryptionType;
    }

    public void setCryptionType(String cryptionType) {
        this.cryptionType = cryptionType;
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

    public String getCanWrite() {
        return canWrite;
    }

    public void setCanWrite(String canWrite) {
        this.canWrite = canWrite;
    }

    public String getUserIdMessage() {
        return userIdMessage;
    }

    public void setUserIdMessage(String userIdMessage) {
        this.userIdMessage = userIdMessage;
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
