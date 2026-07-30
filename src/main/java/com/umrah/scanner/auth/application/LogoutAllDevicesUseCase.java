package com.umrah.scanner.auth.application;

import com.umrah.scanner.auth.infrastructure.RefreshTokenRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutAllDevicesUseCase {

    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutAllDevicesUseCase(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public void execute(UUID userId) {
        refreshTokenRepository.revokeAllActiveForUser(userId, Instant.now());
    }
}
