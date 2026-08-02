package com.umrah.scanner.company.presentation;

import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
                company.getApprovedAt(),
                company.getCreatedAt());
    }
}
