package com.axconstantino.reservationsystem.user.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Utility class for HMAC-SHA256 cryptographic operations.
 * Provides secure token hashing capabilities.
 */
public class HmacUtil {

    /**
     * Generates HMAC-SHA256 signature for a given message.
     *
     * @param secret Secret key for signing
     * @param data Message to be signed
     * @return Base64-encoded HMAC signature
     * @throws RuntimeException If cryptographic operations fail
     */
    public static String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC generation failed", e);
        }
    }
}
