package com.umrah.scanner.common.security;

import com.umrah.scanner.user.domain.Role;
import java.util.UUID;

/** The JWT principal attached to the request by {@code JwtAuthenticationFilter}. Injectable via {@code @AuthenticationPrincipal}. */
public record AuthenticatedUser(UUID userId, Role role) {
}
