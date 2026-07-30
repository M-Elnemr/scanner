package com.umrah.scanner.notification.presentation;

import com.umrah.scanner.notification.domain.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDeviceTokenRequest(@NotBlank String fcmToken, @NotNull DevicePlatform platform) {
}
