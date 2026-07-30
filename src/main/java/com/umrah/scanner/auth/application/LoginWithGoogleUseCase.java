package com.umrah.scanner.auth.application;

import com.umrah.scanner.auth.domain.RefreshToken;
import com.umrah.scanner.auth.infrastructure.RefreshTokenRepository;
import com.umrah.scanner.common.exception.UnauthorizedException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.user.domain.Role;
import com.umrah.scanner.user.domain.User;
import com.umrah.scanner.user.domain.UserStatus;
import com.umrah.scanner.user.infrastructure.UserRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google is the sole identity provider — there is no password to check. First login for a
 * given Google subject creates the {@link User} with the role the client requested
 * (COMPANY or CUSTOMER only; ADMIN accounts are seeded directly in the database).
 */
@Service
public class LoginWithGoogleUseCase {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final AccessTokenIssuer accessTokenIssuer;
    private final TokenHasher tokenHasher;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTokenTtl;

    public LoginWithGoogleUseCase(
            GoogleIdTokenVerifier googleIdTokenVerifier,
            AccessTokenIssuer accessTokenIssuer,
            TokenHasher tokenHasher,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.jwt.refresh-token-ttl}") Duration refreshTokenTtl) {
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.accessTokenIssuer = accessTokenIssuer;
        this.tokenHasher = tokenHasher;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    @Transactional
    public LoginResult execute(String googleIdToken, Role requestedRoleForNewAccount) {
        GoogleIdentity identity = googleIdTokenVerifier.verify(googleIdToken);

        User user = userRepository.findByGoogleSub(identity.subject())
                .or(() -> userRepository.findByEmail(identity.email()))
                .orElseGet(() -> registerNewUser(identity, requestedRoleForNewAccount));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new UnauthorizedException("This account has been suspended");
        }

        return new LoginResult(user, issueTokenPair(user));
    }

    private User registerNewUser(GoogleIdentity identity, Role requestedRole) {
        if (requestedRole == null || requestedRole == Role.ADMIN) {
            throw new ValidationException("New accounts must register as COMPANY or CUSTOMER");
        }
        User user = new User();
        user.setEmail(identity.email());
        user.setGoogleSub(identity.subject());
        user.setRole(requestedRole);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }

    private TokenPair issueTokenPair(User user) {
        AccessTokenIssuer.IssuedAccessToken accessToken = accessTokenIssuer.issue(user);

        String rawRefreshToken = RawRefreshTokenGenerator.generate();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHasher.hash(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenTtl));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(accessToken.token(), accessToken.expiresAt(), rawRefreshToken);
    }
}
