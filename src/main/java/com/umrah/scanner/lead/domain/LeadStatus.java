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
 *
 * <p>{@link #CANCELLED} is deliberately <em>off</em> the ladder: the customer abandoned the journey
 * rather than progressing through it. Its stage is negative so that no accidental
 * {@code isAtLeast(...)} comparison ever reads it as progress, but reasoning about a cancelled lead
 * should go through {@link #isCancelled()} or {@link #isTerminal()}, never through the ordering.
 */
public enum LeadStatus {

    INTERESTED(0),
    PENDING_DEPOSIT_CONFIRMATION(1),
    DEPOSIT_PAID(2),
    PENDING_FULL_PAYMENT_CONFIRMATION(3),
    FULLY_PAID(4),
    PENDING_COMMISSION_CONFIRMATION(5),
    COMMISSION_PAID(6),
    CASHBACK_PAID(7),

    /** The customer withdrew. Off the ladder — see the class comment before comparing stages. */
    CANCELLED(-1);

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

    public boolean isCancelled() {
        return this == CANCELLED;
    }

    /** Nothing further can happen to the lead — it either ran to completion or was cancelled. */
    public boolean isTerminal() {
        return this == CASHBACK_PAID || this == CANCELLED;
    }

    /**
     * True while the lead still occupies the customer's single "preserved journey" slot. The
     * counterpart of this rule lives in the {@code uq_leads_customer_active} partial index, so the
     * two must be changed together.
     */
    public boolean isActive() {
        return !isTerminal();
    }
}
