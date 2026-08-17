package com.umrah.scanner.lead.application;

import com.umrah.scanner.lead.domain.LeadStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminLeadFilter(
        LeadStatus status, UUID companyId, UUID tripId, UUID customerId,
        Instant createdFrom, Instant createdTo, String search) {

    public static final AdminLeadFilter NONE = new AdminLeadFilter(null, null, null, null, null, null, null);
}
