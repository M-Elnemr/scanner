package com.umrah.scanner.auth.application;

import com.umrah.scanner.auth.domain.RefreshToken;
import com.umrah.scanner.auth.infrastructure.RefreshTokenRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository, TokenHasher tokenHasher) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
    }

    @Transactional
    public void execute(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(tokenHasher.hash(rawRefreshToken)).ifPresent(this::revoke);
    }

    private void revoke(RefreshToken token) {
        if (!token.isRevoked()) {
            token.setRevoked(true);
            token.setRevokedAt(Instant.now());
        }
    }
}
