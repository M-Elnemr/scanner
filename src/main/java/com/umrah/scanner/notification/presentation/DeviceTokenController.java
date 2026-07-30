package com.umrah.scanner.notification.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.notification.application.RegisterDeviceTokenUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceTokenController {

    private final RegisterDeviceTokenUseCase registerDeviceTokenUseCase;

    public DeviceTokenController(RegisterDeviceTokenUseCase registerDeviceTokenUseCase) {
        this.registerDeviceTokenUseCase = registerDeviceTokenUseCase;
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/api/v1/devices/register-token")
    public void register(@AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody RegisterDeviceTokenRequest request) {
        registerDeviceTokenUseCase.execute(currentUser.userId(), request.fcmToken(), request.platform());
    }
}
