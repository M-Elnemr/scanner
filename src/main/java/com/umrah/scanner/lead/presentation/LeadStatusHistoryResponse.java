package com.umrah.scanner.lead.presentation;

import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.lead.domain.LeadStatusHistory;
import java.time.Instant;
import java.util.UUID;

public record LeadStatusHistoryResponse(LeadStatus fromStatus, LeadStatus toStatus, UUID changedBy, Instant changedAt, String note) {

    public static LeadStatusHistoryResponse from(LeadStatusHistory history) {
        return new LeadStatusHistoryResponse(
                history.getFromStatus(), history.getToStatus(), history.getChangedBy(), history.getChangedAt(), history.getNote());
    }
}
