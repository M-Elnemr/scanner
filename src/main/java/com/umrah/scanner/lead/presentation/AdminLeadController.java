package com.umrah.scanner.lead.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.response.PageResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.company.application.CompanyQueryService;
import com.umrah.scanner.company.presentation.CompanyResponse;
import com.umrah.scanner.lead.application.AdminLeadFilter;
import com.umrah.scanner.lead.application.LeadQueryService;
import com.umrah.scanner.lead.application.OverrideLeadStatusUseCase;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.messaging.application.WhatsAppLanguage;
import com.umrah.scanner.messaging.application.WhatsAppLeadMessageService;
import com.umrah.scanner.messaging.presentation.WhatsAppMessageResponse;
import com.umrah.scanner.trip.application.TripQueryService;
import com.umrah.scanner.trip.presentation.TripDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The admin lead console: cross-customer, cross-company filtering, a detail view with the trip and
 * company already embedded, and the status override escape hatch. Separate from
 * {@link LeadController} because every handler here needs {@link TripQueryService} and
 * {@link CompanyQueryService} to compose {@link AdminLeadDetailResponse}, which no other role's
 * lead endpoints need.
 */
@Tag(name = "Admin: Leads", description = "Cross-company lead console: filtering, detail, and the status override.")
@RestController
public class AdminLeadController {

    private final LeadQueryService leadQueryService;
    private final OverrideLeadStatusUseCase overrideLeadStatusUseCase;
    private final TripQueryService tripQueryService;
    private final CompanyQueryService companyQueryService;
    private final WhatsAppLeadMessageService whatsAppLeadMessageService;

    public AdminLeadController(
            LeadQueryService leadQueryService,
            OverrideLeadStatusUseCase overrideLeadStatusUseCase,
            TripQueryService tripQueryService,
            CompanyQueryService companyQueryService,
            WhatsAppLeadMessageService whatsAppLeadMessageService) {
        this.leadQueryService = leadQueryService;
        this.overrideLeadStatusUseCase = overrideLeadStatusUseCase;
        this.tripQueryService = tripQueryService;
        this.companyQueryService = companyQueryService;
        this.whatsAppLeadMessageService = whatsAppLeadMessageService;
    }

    @Operation(summary = "List leads, filtered any way the console needs",
            description = "Every parameter is optional and they combine — e.g. companyId + createdFrom/createdTo "
                    + "for \"this company's bookings this month\". search matches customer name/phone or trip title/code.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/leads")
    public ApiResponse<PageResponse<LeadResponse>> list(
            @RequestParam(required = false) LeadStatus status,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID tripId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        var filter = new AdminLeadFilter(status, companyId, tripId, customerId, createdFrom, createdTo, search);
        Page<Lead> leads = leadQueryService.listForAdmin(filter, pageable);
        return ApiResponse.of(PageResponse.of(leads, LeadResponse::forAdmin));
    }

    @Operation(summary = "Get a lead with its trip and company detail embedded",
            description = "Also see the trip he preserved and the trip's company in one call, rather than three.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/leads/{id}")
    public ApiResponse<AdminLeadDetailResponse> getOne(@PathVariable UUID id) {
        Lead lead = leadQueryService.getForAdmin(id);
        TripDetailResponse trip = TripDetailResponse.from(tripQueryService.ownedDetail(lead.getTrip().getId()));
        CompanyResponse company = CompanyResponse.from(companyQueryService.getById(lead.getCompany().getId()));
        return ApiResponse.of(new AdminLeadDetailResponse(LeadResponse.forAdmin(lead), trip, company));
    }

    @Operation(summary = "Force a lead directly to any status",
            description = "Bypasses the normal report/confirm workflow entirely — a reason is required and both "
                    + "the customer and company are notified. Does NOT move the commission or cashback ledgers; "
                    + "use confirm-commission / pay-cashback for that. 409 if the lead is already in that status, "
                    + "or if reviving it out of CANCELLED/CASHBACK_PAID would give the customer a second preserved "
                    + "journey — cancel their other one first.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/leads/{id}/status")
    public ApiResponse<LeadResponse> overrideStatus(
            @AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody OverrideLeadStatusRequest request) {
        Lead lead = overrideLeadStatusUseCase.execute(admin.userId(), id, request.status(), request.reason());
        return ApiResponse.of(LeadResponse.forAdmin(lead));
    }

    @Operation(summary = "Compose a WhatsApp message with this trip's details",
            description = "Returns the message text and a wa.me link pre-filled with it, addressed to the "
                    + "customer behind this lead. Nothing is sent automatically. lang defaults to Arabic; pass "
                    + "lang=en for English.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/leads/{id}/whatsapp/trip")
    public ApiResponse<WhatsAppMessageResponse> whatsAppTripMessage(
            @PathVariable UUID id, @RequestParam(required = false) String lang) {
        var message = whatsAppLeadMessageService.tripMessage(id, WhatsAppLanguage.fromParam(lang));
        return ApiResponse.of(WhatsAppMessageResponse.from(message));
    }

    @Operation(summary = "Compose a WhatsApp message with the trip's company details",
            description = "Same shape as the trip variant, addressed to the same customer, but with the "
                    + "operator's identity, licence, rating and branch contact numbers instead.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/leads/{id}/whatsapp/company")
    public ApiResponse<WhatsAppMessageResponse> whatsAppCompanyMessage(
            @PathVariable UUID id, @RequestParam(required = false) String lang) {
        var message = whatsAppLeadMessageService.companyMessage(id, WhatsAppLanguage.fromParam(lang));
        return ApiResponse.of(WhatsAppMessageResponse.from(message));
    }
}
