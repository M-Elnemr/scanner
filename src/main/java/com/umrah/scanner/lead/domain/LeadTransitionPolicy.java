package com.umrah.scanner.lead.domain;

import com.umrah.scanner.user.domain.Role;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The lead lifecycle's allow-list: which role may move a lead from one status to another.
 * Anything not on this list is rejected — silence is not permission. Pure domain logic,
 * no framework dependency, so it can be unit tested without a Spring context.
 */
public final class LeadTransitionPolicy {

    private record Edge(LeadStatus to, Role actor) {
    }

    private static final Map<LeadStatus, Set<Edge>> USER_TRANSITIONS = new EnumMap<>(LeadStatus.class);
    private static final Map<LeadStatus, LeadStatus> SYSTEM_CASCADES = new EnumMap<>(LeadStatus.class);

    static {
        USER_TRANSITIONS.put(LeadStatus.INTERESTED, Set.of(
                new Edge(LeadStatus.COMPANY_CONTACTED, Role.COMPANY),
                new Edge(LeadStatus.CUSTOMER_CONFIRMED, Role.CUSTOMER)));
        USER_TRANSITIONS.put(LeadStatus.COMPANY_CONTACTED, Set.of(
                new Edge(LeadStatus.CUSTOMER_CONFIRMED, Role.CUSTOMER)));
        USER_TRANSITIONS.put(LeadStatus.CUSTOMER_CONFIRMED, Set.of(
                new Edge(LeadStatus.PAYMENT_PENDING, Role.CUSTOMER)));
        USER_TRANSITIONS.put(LeadStatus.PAYMENT_PENDING, Set.of(
                new Edge(LeadStatus.COMPANY_MARKED_PAID, Role.COMPANY)));
        USER_TRANSITIONS.put(LeadStatus.COMPANY_MARKED_PAID, Set.of());
        USER_TRANSITIONS.put(LeadStatus.ADMIN_REVIEW, Set.of(
                new Edge(LeadStatus.COMMISSION_RELEASED, Role.ADMIN)));
        USER_TRANSITIONS.put(LeadStatus.COMMISSION_RELEASED, Set.of(
                new Edge(LeadStatus.CASHBACK_SENT, Role.ADMIN)));
        USER_TRANSITIONS.put(LeadStatus.CASHBACK_SENT, Set.of());

        // Not user-invocable: applied automatically the instant a lead reaches COMPANY_MARKED_PAID.
        SYSTEM_CASCADES.put(LeadStatus.COMPANY_MARKED_PAID, LeadStatus.ADMIN_REVIEW);
    }

    private LeadTransitionPolicy() {
    }

    public static boolean isAllowed(LeadStatus from, LeadStatus to, Role actor) {
        return USER_TRANSITIONS.getOrDefault(from, Set.of()).stream()
                .anyMatch(edge -> edge.to() == to && edge.actor() == actor);
    }

    public static Optional<LeadStatus> systemCascadeAfter(LeadStatus status) {
        return Optional.ofNullable(SYSTEM_CASCADES.get(status));
    }
}
