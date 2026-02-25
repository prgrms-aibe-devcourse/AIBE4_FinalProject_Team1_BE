package kr.inventory.domain.dining.service;

import kr.inventory.domain.dining.exception.TokenErrorCode;
import kr.inventory.domain.dining.exception.TokenException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class TokenSupport {

    private static final SecureRandom RNG = new SecureRandom();

    private TokenSupport() {}

    /** 32 bytes => 256-bit token */
    public static String newOpaqueToken() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256(token) -> hex */
    public static String sha256Hex(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new TokenException(TokenErrorCode.TOKEN_HASHING_FAILED);
        }
    }
}
