package com.umrah.scanner.company.presentation;

import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Returned to the owning company and to admins only — never on a public or customer-facing route. */
@Schema(name = "Company", description = "A company profile. Only the owning company and admins can retrieve it.")
public record CompanyResponse(
        UUID id,
        String companyName,
        String licenseNumber,
        List<CompanyAddressResponse> addresses,
        String logoUrl,
        String whatsapp,
        String description,
        CompanyStatus status,
        String rejectionReason,
        BigDecimal ratingAvg,
        int ratingCount,
        @Schema(description = "EGP paid to the platform per billable traveler. Read-only for companies; only an admin can change it.")
        BigDecimal commissionPerTraveler,
        Instant approvedAt,
        Instant createdAt) {

    public static CompanyResponse from(CompanyProfile company) {
        return new CompanyResponse(
                company.getId(),
                company.getCompanyName(),
                company.getLicenseNumber(),
                company.getAddresses().stream().map(CompanyAddressResponse::from).toList(),
                company.getLogoUrl(),
                company.getWhatsapp(),
                company.getDescription(),
                company.getStatus(),
                company.getRejectionReason(),
                company.getRatingAvg(),
                company.getRatingCount(),
                company.getCommissionPerTraveler(),
                company.getApprovedAt(),
                company.getCreatedAt());
    }
}
