package com.umrah.scanner.auth.application;

import com.umrah.scanner.user.domain.User;
import java.time.Instant;

/** Port for minting the short-lived JWT access token handed back to the client. */
public interface AccessTokenIssuer {

    IssuedAccessToken issue(User user);

    record IssuedAccessToken(String token, Instant expiresAt) {
    }
}
