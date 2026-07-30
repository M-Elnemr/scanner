package com.umrah.scanner.auth.application;

import com.umrah.scanner.auth.domain.RefreshToken;
import com.umrah.scanner.auth.infrastructure.RefreshTokenRepository;
import com.umrah.scanner.common.exception.UnauthorizedException;
import com.umrah.scanner.user.domain.User;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens rotate on every use. Presenting a token that was already rotated away
 * (i.e. already revoked) is treated as theft — it revokes every other active token for
 * that user, forcing a fresh login on all devices.
 */
@Service
public class RefreshAccessTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Duration refreshTokenTtl;

    public RefreshAccessTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            TokenHasher tokenHasher,
            AccessTokenIssuer accessTokenIssuer,
            @Value("${app.jwt.refresh-token-ttl}") Duration refreshTokenTtl) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public TokenPair execute(String rawRefreshToken) {
        String hash = tokenHasher.hash(rawRefreshToken);
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        User user = current.getUser();

        if (current.isRevoked()) {
            refreshTokenRepository.revokeAllActiveForUser(user.getId(), Instant.now());
            throw new UnauthorizedException("Refresh token reuse detected — all sessions have been signed out");
        }
        if (current.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }

        String rawNewToken = RawRefreshTokenGenerator.generate();
        RefreshToken next = new RefreshToken();
        next.setUser(user);
        next.setTokenHash(tokenHasher.hash(rawNewToken));
        next.setExpiresAt(Instant.now().plus(refreshTokenTtl));
        next.setRevoked(false);
        next = refreshTokenRepository.save(next);

        current.setRevoked(true);
        current.setRevokedAt(Instant.now());
        current.setReplacedByToken(next);

        AccessTokenIssuer.IssuedAccessToken accessToken = accessTokenIssuer.issue(user);
        return new TokenPair(accessToken.token(), accessToken.expiresAt(), rawNewToken);
    }
}
