package com.umrah.scanner.lead.application;

import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.lead.domain.Lead;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The customer is already holding a preserved journey on another trip.
 *
 * <p>Carries the blocking lead so the app can name it in the warning dialog and offer to cancel it,
 * which is the whole point of failing loudly here rather than silently creating a second lead.
 */
public class ActiveLeadExistsException extends ConflictException {

    public static final String CODE = "ACTIVE_LEAD_EXISTS";

    public ActiveLeadExistsException(Lead activeLead) {
        super("You already have a preserved journey. Cancel it before preserving another.",
                CODE, Map.of("activeLead", describe(activeLead)));
    }

    /**
     * A LinkedHashMap rather than {@code Map.of} so the fields serialise in a readable order, and
     * so a null trip title cannot blow up on an immutable-map null check.
     */
    private static Map<String, Object> describe(Lead lead) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("leadId", lead.getId());
        summary.put("tripId", lead.getTrip().getId());
        summary.put("tripTitle", lead.getTrip().getTitle());
        summary.put("status", lead.getStatus());
        return summary;
    }
}
