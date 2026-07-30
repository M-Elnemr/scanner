package com.umrah.scanner.commission.presentation;

import com.umrah.scanner.commission.domain.Commission;
import com.umrah.scanner.commission.domain.CommissionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CommissionResponse(UUID id, UUID leadId, UUID companyId, BigDecimal amount, CommissionStatus status, Instant releasedAt) {

    public static CommissionResponse from(Commission commission) {
        return new CommissionResponse(
                commission.getId(), commission.getLead().getId(), commission.getCompany().getId(),
                commission.getAmount(), commission.getStatus(), commission.getReleasedAt());
    }
}
