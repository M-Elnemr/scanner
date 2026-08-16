package com.umrah.scanner.lead.domain;

import com.umrah.scanner.user.domain.Role;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The lead lifecycle's allow-list: for each {@link LeadAction}, who may invoke it and which
 * starting statuses it is legal from. Anything not on this list is rejected — silence is not
 * permission, so illegal jumps such as INTERESTED to FULLY_PAID, DEPOSIT_PAID to CASHBACK_PAID or
 * any backwards move are impossible by construction rather than by a hand-written check.
 *
 * <p>Pure domain logic with no framework dependency, so it is unit testable without a Spring
 * context. Extending the workflow — an extra approval step, a cancellation path — means editing
 * this table and nothing else.
 */
public final class LeadTransitionPolicy {

    private record Rule(Role actor, Map<LeadStatus, LeadStatus> transitions) {
    }

    private static final Map<LeadAction, Rule> RULES = new EnumMap<>(LeadAction.class);

    static {
        // Deposit: the customer reports and parks in PENDING; the company either confirms that
        // report or records the deposit itself straight from INTERESTED.
        rule(LeadAction.REPORT_DEPOSIT, Map.of(
                LeadStatus.INTERESTED, LeadStatus.PENDING_DEPOSIT_CONFIRMATION));
        rule(LeadAction.MARK_DEPOSIT_PAID, Map.of(
                LeadStatus.INTERESTED, LeadStatus.DEPOSIT_PAID,
                LeadStatus.PENDING_DEPOSIT_CONFIRMATION, LeadStatus.DEPOSIT_PAID));

        // Full payment: same shape as the deposit.
        rule(LeadAction.REPORT_FULL_PAYMENT, Map.of(
                LeadStatus.DEPOSIT_PAID, LeadStatus.PENDING_FULL_PAYMENT_CONFIRMATION));
        rule(LeadAction.MARK_FULLY_PAID, Map.of(
                LeadStatus.DEPOSIT_PAID, LeadStatus.FULLY_PAID,
                LeadStatus.PENDING_FULL_PAYMENT_CONFIRMATION, LeadStatus.FULLY_PAID));

        // Commission owed to the platform: the company reports payment and the admin confirms it,
        // or the admin records it directly. This is the same report/confirm shape as the two above,
        // so moving to (or away from) admin approval is a one-line change in this table.
        rule(LeadAction.REPORT_COMMISSION_PAID, Map.of(
                LeadStatus.FULLY_PAID, LeadStatus.PENDING_COMMISSION_CONFIRMATION));
        rule(LeadAction.CONFIRM_COMMISSION_PAID, Map.of(
                LeadStatus.FULLY_PAID, LeadStatus.COMMISSION_PAID,
                LeadStatus.PENDING_COMMISSION_CONFIRMATION, LeadStatus.COMMISSION_PAID));

        // Cashback is unreachable until the company's commission has actually been confirmed.
        rule(LeadAction.PAY_CASHBACK, Map.of(
                LeadStatus.COMMISSION_PAID, LeadStatus.CASHBACK_PAID));

        // Withdrawal. Legal from every status except CASHBACK_PAID — once the customer has been paid
        // out there is nothing left to withdraw from. CANCELLED is absent as a source, so a second
        // cancel is a conflict by construction rather than by a hand-written guard.
        //
        // Note what this does *not* do: cancelling after a deposit or a full payment voids the
        // platform's commission record, but no money moves back to the customer. Refunds stay a
        // matter between them and the company.
        rule(LeadAction.CANCEL, Map.of(
                LeadStatus.INTERESTED, LeadStatus.CANCELLED,
                LeadStatus.PENDING_DEPOSIT_CONFIRMATION, LeadStatus.CANCELLED,
                LeadStatus.DEPOSIT_PAID, LeadStatus.CANCELLED,
                LeadStatus.PENDING_FULL_PAYMENT_CONFIRMATION, LeadStatus.CANCELLED,
                LeadStatus.FULLY_PAID, LeadStatus.CANCELLED,
                LeadStatus.PENDING_COMMISSION_CONFIRMATION, LeadStatus.CANCELLED,
                LeadStatus.COMMISSION_PAID, LeadStatus.CANCELLED));
    }

    private LeadTransitionPolicy() {
    }

    private static void rule(LeadAction action, Map<LeadStatus, LeadStatus> transitions) {
        RULES.put(action, new Rule(action.actor(), Collections.unmodifiableMap(new EnumMap<>(transitions))));
    }

    /** The status this action moves the lead to from {@code from}, or empty if it is not legal there. */
    public static Optional<LeadStatus> targetOf(LeadAction action, LeadStatus from) {
        Rule rule = RULES.get(action);
        return rule == null ? Optional.empty() : Optional.ofNullable(rule.transitions().get(from));
    }

    /** The single role permitted to invoke this action. */
    public static Role actorOf(LeadAction action) {
        return action.actor();
    }

    public static boolean isAllowed(LeadAction action, LeadStatus from, Role actor) {
        return actorOf(action) == actor && targetOf(action, from).isPresent();
    }

    /**
     * Everything {@code actor} may do to a lead sitting in {@code status} — returned to clients so
     * the UI can render exactly the buttons the server will accept, instead of guessing.
     */
    public static Set<LeadAction> availableActions(LeadStatus status, Role actor) {
        Set<LeadAction> actions = EnumSet.noneOf(LeadAction.class);
        for (LeadAction action : LeadAction.values()) {
            if (isAllowed(action, status, actor)) {
                actions.add(action);
            }
        }
        return actions;
    }
}
