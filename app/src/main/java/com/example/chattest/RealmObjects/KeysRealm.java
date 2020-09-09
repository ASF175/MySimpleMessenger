package com.example.chattest.RealmObjects;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class KeysRealm extends RealmObject {
    String key;
    @PrimaryKey
    String keyName;

    public KeysRealm() {
    }

    public KeysRealm(String key, String keyName) {
        this.key = key;
        this.keyName = keyName;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }
}
