package com.example.chattest.RealmObjects;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class DialogUserRealm extends RealmObject {
    String userId;
    String dialogId;

    @PrimaryKey
    String personalKey;

    public DialogUserRealm() {
    }

    public DialogUserRealm(String dialogId, String userId) {
        this.personalKey = userId + dialogId;
        this.dialogId = dialogId;
        this.userId = userId;
    }

    public String getPersonalKey() {
        return personalKey;
    }

    public void setPersonalKey(String personalKey) {
        this.personalKey = personalKey;
    }

    public String getDialogId() {
        return dialogId;
    }

    public void setDialogId(String dialogId) {
        this.dialogId = dialogId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
