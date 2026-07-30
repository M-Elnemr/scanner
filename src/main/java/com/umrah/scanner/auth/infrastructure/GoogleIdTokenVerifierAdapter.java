package com.umrah.scanner.auth.infrastructure;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.umrah.scanner.auth.application.GoogleIdentity;
import com.umrah.scanner.common.exception.UnauthorizedException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Verifies a Google ID token locally against Google's cached public keys — no per-login round trip to Google. */
@Component
public class GoogleIdTokenVerifierAdapter implements com.umrah.scanner.auth.application.GoogleIdTokenVerifier {

    private final com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier delegate;

    public GoogleIdTokenVerifierAdapter(@Value("${app.google.client-ids}") String clientIdsCsv) {
        List<String> audience = Arrays.stream(clientIdsCsv.split(","))
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .toList();

        this.delegate = new Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(audience)
                .build();
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException("Missing Google ID token");
        }
        try {
            GoogleIdToken token = delegate.verify(idToken);
            if (token == null) {
                throw new UnauthorizedException("Google ID token failed verification");
            }
            GoogleIdToken.Payload payload = token.getPayload();
            return new GoogleIdentity(payload.getSubject(), payload.getEmail());
        } catch (GeneralSecurityException | IOException | IllegalArgumentException e) {
            throw new UnauthorizedException("Failed to verify Google ID token");
        }
    }
}
