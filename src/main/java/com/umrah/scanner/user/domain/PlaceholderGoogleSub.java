package com.umrah.scanner.user.domain;

/**
 * The synthetic {@code google_sub} an admin-provisioned account is created with before its owner
 * ever signs in. {@code users.google_sub} is {@code NOT NULL} with its own partial unique index, so
 * a company created by an admin needs some value there from the first row — this is deliberately
 * derived from the email so a retry after a failed create collides loudly instead of leaving a
 * second ghost account, mirroring the {@code dev-test:} convention {@code DevGoogleLoginUseCase}
 * already uses for the same reason.
 */
public final class PlaceholderGoogleSub {

    private static final String PENDING_CLAIM_PREFIX = "pending-claim:";

    private PlaceholderGoogleSub() {
    }

    public static String forEmail(String email) {
        return PENDING_CLAIM_PREFIX + email.trim().toLowerCase();
    }

    public static boolean isPlaceholder(String googleSub) {
        return googleSub != null && googleSub.startsWith(PENDING_CLAIM_PREFIX);
    }
}
