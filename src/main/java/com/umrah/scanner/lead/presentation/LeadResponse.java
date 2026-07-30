package com.umrah.scanner.lead.presentation;

import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.trip.domain.RoomType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        UUID customerId,
        UUID tripId,
        String tripTitle,
        UUID companyId,
        String companyName,
        LeadStatus status,
        RoomType confirmedRoomType,
        BigDecimal confirmedPrice,
        Instant createdAt,
        Instant updatedAt) {

    public static LeadResponse from(Lead lead) {
        return new LeadResponse(
                lead.getId(), lead.getCustomer().getId(), lead.getTrip().getId(), lead.getTrip().getTitle(),
                lead.getCompany().getId(), lead.getCompany().getCompanyName(), lead.getStatus(),
                lead.getConfirmedRoomType(), lead.getConfirmedPrice(), lead.getCreatedAt(), lead.getUpdatedAt());
    }
}
