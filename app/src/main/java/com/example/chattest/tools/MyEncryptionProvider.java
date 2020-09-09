package com.example.chattest.tools;

import android.util.Base64;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class MyEncryptionProvider {

    public MyEncryptionProvider() {
    }

    public static String getAesByPassword(@NotNull String pass) {
        if (pass.toCharArray().length < 32) {
            int lenght = pass.toCharArray().length;
            int neededIterations = (int) Math.floor((double) 32 / (double) lenght);
            int additionalCount = 32 % lenght;
            StringBuilder res = new StringBuilder();
            for (int i = 0; i < neededIterations; i++) {
                res.append(pass);
            }
            res.append(pass.toCharArray(), 0, additionalCount);
            return res.toString();
        } else if (pass.toCharArray().length > 32) {
            char[] res = new char[32];
            pass.getChars(0, 32, res, 0);
            return new String(res);
        } else {
            return pass;
        }
    }

    public static SecretKey getAesKeyByPassword(@NotNull String pass) {
        if (pass.toCharArray().length < 32) {
            int lenght = pass.toCharArray().length;
            int neededIterations = (int) Math.floor((double) 32 / (double) lenght);
            int additionalCount = 32 % lenght;
            StringBuilder res = new StringBuilder();
            for (int i = 0; i < neededIterations; i++) {
                res.append(pass);
            }
            res.append(pass.toCharArray(), 0, additionalCount);
            return new SecretKeySpec(res.toString().getBytes(StandardCharsets.UTF_8), "AES");
        } else if (pass.toCharArray().length > 32) {
            char[] res = new char[32];
            pass.getChars(0, 32, res, 0);
            return new SecretKeySpec(new String(res).getBytes(StandardCharsets.UTF_8), "AES");
        } else {
            return new SecretKeySpec(pass.getBytes(StandardCharsets.UTF_8), "AES");
        }
    }

    public static PrivateKey getPrivateKey(@NotNull String base64key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(base64key, Base64.DEFAULT));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static PublicKey getPublicKey(@NotNull String base64key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decode(base64key, Base64.DEFAULT));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public static SecretKey getSecretKey(String base64key) {
        return new SecretKeySpec(Base64.decode(base64key, Base64.DEFAULT), "AES");
    }

    public static SecretKey getSecretKey(byte[] key) {
        return new SecretKeySpec(key, "AES");
    }
}
