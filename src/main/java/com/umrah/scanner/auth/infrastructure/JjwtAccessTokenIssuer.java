package com.umrah.scanner.auth.infrastructure;

import com.umrah.scanner.auth.application.AccessTokenIssuer;
import com.umrah.scanner.common.exception.UnauthorizedException;
import com.umrah.scanner.user.domain.Role;
import com.umrah.scanner.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Signs and verifies the HS256 JWT access token. One secret, one issuer — this is a monolith, not a federation of services. */
@Component
public class JjwtAccessTokenIssuer implements AccessTokenIssuer {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_EMAIL = "email";

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JjwtAccessTokenIssuer(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-ttl}") Duration accessTokenTtl) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = accessTokenTtl;
    }

    @Override
    public IssuedAccessToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenTtl);

        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_EMAIL, user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();

        return new IssuedAccessToken(token, expiresAt);
    }

    /** Used by the JWT authentication filter — not part of the {@link AccessTokenIssuer} port, which only issues tokens. */
    public AccessTokenClaims parseAndValidate(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return new AccessTokenClaims(UUID.fromString(claims.getSubject()), Role.valueOf(claims.get(CLAIM_ROLE, String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Invalid or expired access token");
        }
    }

    public record AccessTokenClaims(UUID userId, Role role) {
    }
}
