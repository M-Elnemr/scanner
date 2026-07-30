package com.umrah.scanner.auth.application;

import java.security.SecureRandom;
import java.util.Base64;

/** Generates the high-entropy raw refresh token handed to the client. Only its hash is ever stored. */
final class RawRefreshTokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 64;

    private RawRefreshTokenGenerator() {
    }

    static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
