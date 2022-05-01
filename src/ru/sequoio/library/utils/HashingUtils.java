package ru.sequoio.library.utils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashingUtils {

    private static final String MD5 = "MD5";

    public static String md5(String s) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(MD5);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(s.getBytes());
        byte[] digest = md.digest();
        StringBuilder hashText = new StringBuilder(new BigInteger(1, digest).toString(16));
        while (hashText.length() < 32) {
            hashText.insert(0, "0");
        }
        return hashText.toString();
    }
}
