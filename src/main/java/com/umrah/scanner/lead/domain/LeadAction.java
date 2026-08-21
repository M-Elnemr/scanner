package com.umrah.scanner.lead.domain;

import com.umrah.scanner.user.domain.Role;

/**
 * What an actor asks to do to a lead, rather than which status they want it to end up in.
 *
 * <p>Clients name the intent and the server derives the target status from the lead's current
 * state (see {@link LeadTransitionPolicy}). That is what makes "company confirms the customer's
 * report" and "company reports the payment itself" a single action with two starting points, and
 * it removes any way for a client to request an illegal status directly.
 */
public enum LeadAction {

    /** Admin sent the customer the trip details over WhatsApp. */
    MARK_CONTACTED(Role.ADMIN),

    /** Admin relayed the customer's booking to the company over WhatsApp and the company acknowledged it. */
    CONFIRM_VIA_COMPANY(Role.ADMIN),

    /** Company records that the deposit was paid — the customer no longer self-reports this. */
    MARK_DEPOSIT_PAID(Role.COMPANY),

    /** Company records that the trip was paid in full — the customer no longer self-reports this. */
    MARK_FULLY_PAID(Role.COMPANY),

    /** Company states it has settled the platform's commission; awaits admin confirmation. */
    REPORT_COMMISSION_PAID(Role.COMPANY),

    /** Admin confirms the company's commission payment, or records it directly from FULLY_PAID. */
    CONFIRM_COMMISSION_PAID(Role.ADMIN),

    /** Admin transfers the cashback to the customer's wallet. */
    PAY_CASHBACK(Role.ADMIN),

    /**
     * Customer withdraws from the journey, freeing their single preserved-trip slot.
     *
     * <p>Customer-only by design: this enum binds exactly one actor per action, so a company- or
     * admin-initiated cancellation would be its own action with its own transition rules rather
     * than a second actor on this one.
     */
    CANCEL(Role.CUSTOMER);

    private final Role actor;

    LeadAction(Role actor) {
        this.actor = actor;
    }

    public Role actor() {
        return actor;
    }
}
