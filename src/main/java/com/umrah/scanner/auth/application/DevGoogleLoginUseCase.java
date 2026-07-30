package com.umrah.scanner.auth.application;

import com.umrah.scanner.user.domain.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Stands in for {@link LoginWithGoogleUseCase} in local/dev testing when a real Google ID token
 * isn't available. Bypasses Google entirely — never wire this into anything reachable outside
 * the "dev" profile. The {@code @Profile("dev")} here means this bean does not exist at all
 * unless the dev profile is active; {@link com.umrah.scanner.auth.presentation.DevAuthController}
 * carries the same restriction independently, plus an explicit {@code app.dev-auth.enabled} check.
 */
@Profile("dev")
@Service
public class DevGoogleLoginUseCase {

    private static final Logger log = LoggerFactory.getLogger(DevGoogleLoginUseCase.class);

    /** Prefix makes dev-created accounts unmistakable in the database — never a real Google sub. */
    private static final String DEV_SUBJECT_PREFIX = "dev-test:";

    private final LoginWithGoogleUseCase loginWithGoogleUseCase;

    public DevGoogleLoginUseCase(LoginWithGoogleUseCase loginWithGoogleUseCase) {
        this.loginWithGoogleUseCase = loginWithGoogleUseCase;
    }

    public LoginResult execute(String email, String name, String pictureUrl, Role requestedRoleForNewAccount) {
        log.warn(
                "DEV AUTH BYPASS USED — email={} name={} pictureUrl={} role={} — "
                        + "this endpoint must never be reachable in production",
                email, name, pictureUrl, requestedRoleForNewAccount);

        // name/pictureUrl are accepted for parity with a real Google ID token payload but, like
        // real Google login, are not persisted anywhere — User has no name/picture fields.
        GoogleIdentity syntheticIdentity = new GoogleIdentity(DEV_SUBJECT_PREFIX + email.toLowerCase(), email);
        return loginWithGoogleUseCase.executeForVerifiedIdentity(syntheticIdentity, requestedRoleForNewAccount);
    }
}
