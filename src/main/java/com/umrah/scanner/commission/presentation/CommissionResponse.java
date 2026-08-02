package com.umrah.scanner.commission.presentation;

import com.umrah.scanner.commission.domain.Commission;
import com.umrah.scanner.commission.domain.CommissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "Commission", description = "What a company owes the platform on one lead. Never exposed to customers.")
public record CommissionResponse(
        UUID id,
        UUID leadId,
        UUID companyId,
        @Schema(description = "EGP, copied from the lead's pricing snapshot") BigDecimal amount,
        CommissionStatus status,
        Instant reportedAt,
        Instant confirmedAt) {

    public static CommissionResponse from(Commission commission) {
        return new CommissionResponse(
                commission.getId(), commission.getLead().getId(), commission.getCompany().getId(),
                commission.getAmount(), commission.getStatus(), commission.getReportedAt(), commission.getConfirmedAt());
    }
}
