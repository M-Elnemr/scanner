package com.umrah.scanner.auth.presentation;

import com.umrah.scanner.user.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Stands in for a real Google ID token's payload — dev/test only, see {@link DevAuthController}. */
public record DevGoogleTestRequest(
        @NotBlank @Email String email,
        String name,
        String pictureUrl,
        @NotNull Role role) {
}
