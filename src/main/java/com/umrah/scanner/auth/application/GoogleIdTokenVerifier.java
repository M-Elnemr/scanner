package com.umrah.scanner.auth.application;

import com.umrah.scanner.common.exception.UnauthorizedException;

/** Port for verifying a Google-issued ID token. Implemented in infrastructure against Google's public keys. */
public interface GoogleIdTokenVerifier {

    /**
     * @throws UnauthorizedException if the token is missing, expired, or fails signature/issuer/audience checks.
     */
    GoogleIdentity verify(String idToken);
}
