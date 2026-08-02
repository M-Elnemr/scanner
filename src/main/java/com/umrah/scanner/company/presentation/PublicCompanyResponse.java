package com.umrah.scanner.company.presentation;

import com.umrah.scanner.company.domain.CompanyProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * A company as a customer may see it: who they are, how to reach them, and how they are rated.
 *
 * <p>Separate from {@link CompanyResponse} on purpose. Everything operational is absent by
 * construction rather than by a redaction flag — no verification status, no rejection reason, no
 * approval metadata, and above all no {@code commissionPerTraveler}. A customer only ever sees the
 * cashback that derives from the commission, never the commission itself, so the field cannot leak
 * through this shape even if someone reuses it on a new route.
 */
@Schema(name = "PublicCompany", description = "Customer-facing company profile. Contains no commission or verification data.")
public record PublicCompanyResponse(
        UUID id,
        String companyName,
        String licenseNumber,
        String logoUrl,
        String whatsapp,
        String description,
        @Schema(description = "Average of all reviews, 0 to 5") BigDecimal ratingAvg,
        int ratingCount,
        @Schema(description = "Branches, each with its city and its own contact number") List<CompanyAddressResponse> addresses) {

    public static PublicCompanyResponse from(CompanyProfile company) {
        return new PublicCompanyResponse(
                company.getId(),
                company.getCompanyName(),
                company.getLicenseNumber(),
                company.getLogoUrl(),
                company.getWhatsapp(),
                company.getDescription(),
                company.getRatingAvg(),
                company.getRatingCount(),
                company.getAddresses().stream().map(CompanyAddressResponse::from).toList());
    }
}
