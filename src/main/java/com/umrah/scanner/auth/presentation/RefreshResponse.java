package com.umrah.scanner.auth.presentation;

import com.umrah.scanner.auth.application.TokenPair;
import java.time.Instant;

public record RefreshResponse(String accessToken, Instant accessTokenExpiresAt, String refreshToken) {

    public static RefreshResponse from(TokenPair tokens) {
        return new RefreshResponse(tokens.accessToken(), tokens.accessTokenExpiresAt(), tokens.refreshToken());
    }
}
