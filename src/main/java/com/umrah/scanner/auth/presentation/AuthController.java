package com.umrah.scanner.auth.presentation;

import com.umrah.scanner.auth.application.LogoutAllDevicesUseCase;
import com.umrah.scanner.auth.application.LogoutUseCase;
import com.umrah.scanner.auth.application.LoginWithGoogleUseCase;
import com.umrah.scanner.auth.application.RefreshAccessTokenUseCase;
import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginWithGoogleUseCase loginWithGoogleUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final LogoutAllDevicesUseCase logoutAllDevicesUseCase;

    public AuthController(
            LoginWithGoogleUseCase loginWithGoogleUseCase,
            RefreshAccessTokenUseCase refreshAccessTokenUseCase,
            LogoutUseCase logoutUseCase,
            LogoutAllDevicesUseCase logoutAllDevicesUseCase) {
        this.loginWithGoogleUseCase = loginWithGoogleUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
        this.logoutUseCase = logoutUseCase;
        this.logoutAllDevicesUseCase = logoutAllDevicesUseCase;
    }

    @PostMapping("/google")
    public ApiResponse<TokenResponse> loginWithGoogle(@Valid @RequestBody LoginRequest request) {
        var result = loginWithGoogleUseCase.execute(request.idToken(), request.role());
        return ApiResponse.of(TokenResponse.from(result));
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var tokens = refreshAccessTokenUseCase.execute(request.refreshToken());
        return ApiResponse.of(RefreshResponse.from(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        logoutUseCase.execute(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        logoutAllDevicesUseCase.execute(currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
