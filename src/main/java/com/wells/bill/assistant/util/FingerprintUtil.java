package com.wells.bill.assistant.util;

import java.security.MessageDigest;
import java.util.Base64;

public class FingerprintUtil {
    public static String hash(String card) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(card.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
