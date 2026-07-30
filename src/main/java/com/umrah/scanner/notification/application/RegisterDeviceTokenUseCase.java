package com.umrah.scanner.notification.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.notification.domain.DevicePlatform;
import com.umrah.scanner.notification.domain.DeviceToken;
import com.umrah.scanner.notification.infrastructure.DeviceTokenRepository;
import com.umrah.scanner.user.domain.User;
import com.umrah.scanner.user.infrastructure.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterDeviceTokenUseCase {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    public RegisterDeviceTokenUseCase(DeviceTokenRepository deviceTokenRepository, UserRepository userRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DeviceToken execute(UUID userId, String fcmToken, DevicePlatform platform) {
        DeviceToken token = deviceTokenRepository.findByFcmToken(fcmToken).orElseGet(DeviceToken::new);

        if (token.getId() == null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> NotFoundException.of("User", userId));
            token.setUser(user);
        } else if (!token.getUser().getId().equals(userId)) {
            // Same physical device, different account (e.g. shared device, or a re-install under a new login).
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> NotFoundException.of("User", userId));
            token.setUser(user);
        }

        token.setFcmToken(fcmToken);
        token.setPlatform(platform);
        token.touch(Instant.now());
        return deviceTokenRepository.save(token);
    }
}
