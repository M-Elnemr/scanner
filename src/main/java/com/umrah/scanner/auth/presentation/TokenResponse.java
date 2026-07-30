package com.umrah.scanner.auth.presentation;

import com.umrah.scanner.auth.application.LoginResult;
import com.umrah.scanner.auth.application.TokenPair;
import com.umrah.scanner.user.domain.Role;
import java.time.Instant;
import java.util.UUID;

public record TokenResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        UUID userId,
        Role role) {

    public static TokenResponse from(LoginResult result) {
        TokenPair tokens = result.tokens();
        return new TokenResponse(
                tokens.accessToken(), tokens.accessTokenExpiresAt(), tokens.refreshToken(),
                result.user().getId(), result.user().getRole());
    }
}
