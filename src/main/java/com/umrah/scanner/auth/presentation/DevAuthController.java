package com.umrah.scanner.auth.presentation;

import com.umrah.scanner.auth.application.DevGoogleLoginUseCase;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEV/TEST ONLY. Skips real Google verification entirely so local and automated testing don't
 * need a genuine Google ID token. This class only exists as a Spring bean under the "dev"
 * profile ({@code @Profile("dev")}) — outside that profile the endpoint doesn't exist and the
 * path 404s. As a second, independent guard (in case "dev" is ever active somewhere it
 * shouldn't be), the handler also refuses to run unless {@code app.dev-auth.enabled=true} is
 * explicitly set — which only {@code application-dev.properties} does. Never relax either guard.
 */
@Profile("dev")
@RestController
public class DevAuthController {

    private final DevGoogleLoginUseCase devGoogleLoginUseCase;
    private final boolean devAuthEnabled;

    public DevAuthController(
            DevGoogleLoginUseCase devGoogleLoginUseCase,
            @Value("${app.dev-auth.enabled:false}") boolean devAuthEnabled) {
        this.devGoogleLoginUseCase = devGoogleLoginUseCase;
        this.devAuthEnabled = devAuthEnabled;
    }

    @PostMapping("/api/v1/auth/dev-google-test")
    public ApiResponse<TokenResponse> loginWithoutGoogle(@Valid @RequestBody DevGoogleTestRequest request) {
        if (!devAuthEnabled) {
            // 404, not 403 — this path should look like it doesn't exist, not like a locked door.
            throw new NotFoundException("Not found");
        }
        var result = devGoogleLoginUseCase.execute(request.email(), request.name(), request.pictureUrl(), request.role());
        return ApiResponse.of(TokenResponse.from(result));
    }
}
