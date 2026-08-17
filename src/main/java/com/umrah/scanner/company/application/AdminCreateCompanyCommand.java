package com.umrah.scanner.company.application;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param ownerEmail            the person who will sign in as this company. If no account exists
 *                               for this email yet, one is provisioned with a placeholder Google
 *                               subject that the owner's first real Google sign-in replaces — see
 *                               {@code PlaceholderGoogleSub} and the adoption logic in
 *                               {@code LoginWithGoogleUseCase}.
 * @param commissionPerTraveler optional; defaults to zero, same as a self-registered company, and
 *                               is set the normal way afterwards through
 *                               {@link SetCompanyCommissionUseCase}.
 * @param autoApprove            true skips the PENDING queue — the admin creating the company is
 *                               itself the vetting step. Kept explicit rather than implied so a
 *                               caller has to decide, not default into it.
 */
public record AdminCreateCompanyCommand(
        String ownerEmail,
        String companyName,
        String licenseNumber,
        String logoUrl,
        String whatsapp,
        String description,
        List<CompanyAddressInput> addresses,
        BigDecimal commissionPerTraveler,
        boolean autoApprove) {
}
