package com.umrah.scanner.auth.application;

import java.time.Instant;

public record TokenPair(String accessToken, Instant accessTokenExpiresAt, String refreshToken) {
}
