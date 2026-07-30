package com.umrah.scanner.auth.infrastructure;

import com.umrah.scanner.common.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Reads {@code Authorization: Bearer <token>}. A missing header simply leaves the request
 * unauthenticated (the security chain decides whether that endpoint requires auth); a present
 * but invalid/expired token fails the request immediately with 401, since a client presenting
 * a bad token should be told to re-authenticate, not silently downgraded to anonymous.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JjwtAccessTokenIssuer accessTokenIssuer;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JjwtAccessTokenIssuer accessTokenIssuer, ObjectMapper objectMapper) {
        this.accessTokenIssuer = accessTokenIssuer;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            JjwtAccessTokenIssuer.AccessTokenClaims claims =
                    accessTokenIssuer.parseAndValidate(header.substring(BEARER_PREFIX.length()));

            AuthenticatedUser principal = new AuthenticatedUser(claims.userId(), claims.role());
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name()));
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response, e.getMessage());
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
                java.util.Map.of("status", 401, "error", "Unauthorized", "message", message)));
    }
}
