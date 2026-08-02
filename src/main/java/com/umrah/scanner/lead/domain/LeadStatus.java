package com.umrah.scanner.lead.domain;

/**
 * The lead lifecycle, in order. {@code stage} makes "has this lead got at least as far as X?"
 * explicit rather than leaning on {@link Enum#ordinal()}, so reordering or inserting a constant
 * can never silently change a business rule.
 *
 * <p>The PENDING_* states exist because either side may report a payment first: when the customer
 * reports, the lead parks in a PENDING state until the other party confirms; when the company (or,
 * for commission, the admin) records the payment itself, it goes straight to the settled state with
 * no confirmation needed.
 */
public enum LeadStatus {

    INTERESTED(0),
    PENDING_DEPOSIT_CONFIRMATION(1),
    DEPOSIT_PAID(2),
    PENDING_FULL_PAYMENT_CONFIRMATION(3),
    FULLY_PAID(4),
    PENDING_COMMISSION_CONFIRMATION(5),
    COMMISSION_PAID(6),
    CASHBACK_PAID(7);

    private final int stage;

    LeadStatus(int stage) {
        this.stage = stage;
    }

    public int stage() {
        return stage;
    }

    /** True once the lead has reached {@code other} or moved past it. */
    public boolean isAtLeast(LeadStatus other) {
        return stage >= other.stage;
    }

    /** Nothing further can happen to the lead. */
    public boolean isTerminal() {
        return this == CASHBACK_PAID;
    }
}
