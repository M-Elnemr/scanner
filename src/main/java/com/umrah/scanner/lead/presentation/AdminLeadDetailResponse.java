package com.umrah.scanner.lead.presentation;

import com.umrah.scanner.company.presentation.CompanyResponse;
import com.umrah.scanner.trip.presentation.TripDetailResponse;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * {@link LeadResponse} plus the trip and company an admin needs on screen without a second and
 * third round trip. No new query logic — this composes {@code TripQueryService.ownedDetail} and
 * {@code CompanyQueryService.getById}, both of which already exist for their own endpoints.
 */
@Schema(name = "AdminLeadDetail", description = "A lead with its full trip and company detail embedded.")
public record AdminLeadDetailResponse(LeadResponse lead, TripDetailResponse trip, CompanyResponse company) {
}
