package com.umrah.scanner.auth.presentation;

import com.umrah.scanner.user.domain.Role;
import jakarta.validation.constraints.NotBlank;

/** {@code role} is only consulted the first time this Google subject logs in; it's ignored for returning users. */
public record LoginRequest(
        @NotBlank String idToken,
        Role role) {
}
