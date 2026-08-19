package com.umrah.scanner.lead.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.response.PageResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.lead.application.CreateLeadCommand;
import com.umrah.scanner.lead.application.CreateLeadUseCase;
import com.umrah.scanner.lead.application.ExecuteLeadActionUseCase;
import com.umrah.scanner.lead.application.LeadQueryService;
import com.umrah.scanner.lead.application.UpdateLeadTravelersUseCase;
import com.umrah.scanner.lead.domain.Lead;
import com.umrah.scanner.lead.domain.LeadAction;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.user.domain.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Each lifecycle step gets its own named endpoint rather than one "set status to X" call: the URL
 * states the intent, the role that owns it is enforced at the edge by {@code @PreAuthorize}, and
 * the target status is derived server-side. Every one of them is a thin delegation to
 * {@link ExecuteLeadActionUseCase} — the workflow rules exist in exactly one place.
 */
@Tag(name = "Leads", description = "Booking requests: traveler counts, payment lifecycle, commission and cashback.")
@RestController
public class LeadController {

    private final CreateLeadUseCase createLeadUseCase;
    private final UpdateLeadTravelersUseCase updateLeadTravelersUseCase;
    private final ExecuteLeadActionUseCase executeLeadActionUseCase;
    private final LeadQueryService leadQueryService;

    public LeadController(
            CreateLeadUseCase createLeadUseCase,
            UpdateLeadTravelersUseCase updateLeadTravelersUseCase,
            ExecuteLeadActionUseCase executeLeadActionUseCase,
            LeadQueryService leadQueryService) {
        this.createLeadUseCase = createLeadUseCase;
        this.updateLeadTravelersUseCase = updateLeadTravelersUseCase;
        this.executeLeadActionUseCase = executeLeadActionUseCase;
        this.leadQueryService = leadQueryService;
    }

    // --- Customer ---

    @Operation(summary = "Contact a company about a trip",
            description = "Creates the lead and locks in its commission and cashback for the given traveler counts. "
                    + "Idempotent: contacting the same trip again returns the existing lead.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Lead created or resumed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Profile incomplete, or trip not published")})
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/trips/{tripId}/contact-company")
    public ApiResponse<LeadResponse> contactCompany(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID tripId,
            @Valid @RequestBody ContactCompanyRequest request) {
        var command = new CreateLeadCommand(tripId, request.adultCount(), request.childCount(), request.infantCount());
        return ApiResponse.of(LeadResponse.forCustomer(createLeadUseCase.execute(currentUser.userId(), command)));
    }

    @Operation(summary = "The journey I am currently holding",
            description = "A customer may preserve one trip at a time. Returns that lead, or null data if they "
                    + "have none — the trip-details button reads this to choose between \"preserve\", \"preserved\" "
                    + "and \"you must cancel <other trip> first\". A cancelled or completed journey frees the slot.")
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/api/v1/customers/me/leads/active")
    public ApiResponse<LeadResponse> activeLead(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.of(leadQueryService.findActiveForCustomer(currentUser.userId())
                .map(LeadResponse::forCustomer)
                .orElse(null));
    }

    @Operation(summary = "Cancel my preserved journey",
            description = "Frees the customer's single preserved-trip slot. Allowed from every status except "
                    + "CASHBACK_PAID — read the lead's availableActions rather than comparing statuses. A note "
                    + "explaining why is optional. Any commission the company owed on this lead is voided; money "
                    + "already paid to the company is not refunded by the platform.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Journey cancelled"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Already cancelled, or the cashback has been paid")})
    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/api/v1/customers/me/leads/{id}/cancel")
    public ApiResponse<LeadResponse> cancel(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody LeadActionRequest request) {
        return ApiResponse.of(LeadResponse.forCustomer(
                act(id, LeadAction.CANCEL, Role.CUSTOMER, currentUser, request)));
    }

    @Operation(summary = "List my leads")
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/api/v1/customers/me/leads")
    public ApiResponse<PageResponse<LeadResponse>> listMineAsCustomer(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) LeadStatus status,
            Pageable pageable) {
        var leads = leadQueryService.listForCustomer(currentUser.userId(), status, pageable);
        return ApiResponse.of(PageResponse.of(leads, LeadResponse::forCustomer));
    }

    @Operation(summary = "Change traveler counts",
            description = "Allowed only while the lead is before FULLY_PAID — read the lead's travelersEditable flag "
                    + "rather than comparing statuses. Does not reprice the lead: commission and cashback stay as "
                    + "they were fixed at creation.")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/api/v1/customers/me/leads/{id}/travelers")
    public ApiResponse<LeadResponse> updateTravelers(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTravelersRequest request) {
        Lead lead = updateLeadTravelersUseCase.execute(
                id, currentUser.userId(), request.adultCount(), request.childCount(), request.infantCount());
        return ApiResponse.of(LeadResponse.forCustomer(lead));
    }

    // --- Company ---

    @Operation(summary = "List leads for my company")
    @PreAuthorize("hasRole('COMPANY')")
    @GetMapping("/api/v1/companies/me/leads")
    public ApiResponse<PageResponse<LeadResponse>> listMineAsCompany(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(required = false) LeadStatus status,
            Pageable pageable) {
        var leads = leadQueryService.listForCompany(currentUser.userId(), status, pageable);
        return ApiResponse.of(PageResponse.of(leads, LeadResponse::forCompany));
    }

    @Operation(summary = "Mark the deposit as paid",
            description = "Confirms a customer's deposit report, or records the deposit directly from INTERESTED.")
    @PreAuthorize("hasRole('COMPANY')")
    @PatchMapping("/api/v1/companies/me/leads/{id}/deposit-paid")
    public ApiResponse<LeadResponse> markDepositPaid(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @RequestBody(required = false) LeadActionRequest request) {
        return ApiResponse.of(LeadResponse.forCompany(
                act(id, LeadAction.MARK_DEPOSIT_PAID, Role.COMPANY, currentUser, request)));
    }

    @Operation(summary = "Mark the booking as fully paid",
            description = "Confirms a customer's full-payment report, or records it directly from DEPOSIT_PAID.")
    @PreAuthorize("hasRole('COMPANY')")
    @PatchMapping("/api/v1/companies/me/leads/{id}/fully-paid")
    public ApiResponse<LeadResponse> markFullyPaid(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @RequestBody(required = false) LeadActionRequest request) {
        return ApiResponse.of(LeadResponse.forCompany(
                act(id, LeadAction.MARK_FULLY_PAID, Role.COMPANY, currentUser, request)));
    }

    @Operation(summary = "Report the commission as paid to the platform",
            description = "Moves the lead to PENDING_COMMISSION_CONFIRMATION. An admin must confirm before cashback can be released.")
    @PreAuthorize("hasRole('COMPANY')")
    @PatchMapping("/api/v1/companies/me/leads/{id}/report-commission-paid")
    public ApiResponse<LeadResponse> reportCommissionPaid(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @RequestBody(required = false) LeadActionRequest request) {
        return ApiResponse.of(LeadResponse.forCompany(
                act(id, LeadAction.REPORT_COMMISSION_PAID, Role.COMPANY, currentUser, request)));
    }

    // --- Admin ---
    // Listing, detail-with-trip-and-company, and the status override live on AdminLeadController —
    // they need TripQueryService and CompanyQueryService, which the actions below do not.

    @Operation(summary = "Confirm the company's commission payment",
            description = "Accepts a company's report, or records the payment directly from FULLY_PAID. "
                    + "Cashback becomes payable only after this.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/leads/{id}/confirm-commission")
    public ApiResponse<LeadResponse> confirmCommission(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable UUID id,
            @RequestBody(required = false) LeadActionRequest request) {
        return ApiResponse.of(LeadResponse.forAdmin(
                act(id, LeadAction.CONFIRM_COMMISSION_PAID, Role.ADMIN, admin, request)));
    }

    @Operation(summary = "Pay the customer's cashback",
            description = "Allowed only from COMMISSION_PAID. Requires a wallet on the customer's profile.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/leads/{id}/pay-cashback")
    public ApiResponse<LeadResponse> payCashback(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable UUID id,
            @RequestBody(required = false) LeadActionRequest request) {
        return ApiResponse.of(LeadResponse.forAdmin(
                act(id, LeadAction.PAY_CASHBACK, Role.ADMIN, admin, request)));
    }

    // --- Shared ---

    @Operation(summary = "Get one lead", description = "Visible to the lead's customer, its company, or any admin.")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/leads/{id}")
    public ApiResponse<LeadResponse> getOne(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        Lead lead = leadQueryService.getForCaller(id, currentUser.userId(), currentUser.role() == Role.ADMIN);
        return ApiResponse.of(LeadResponse.forRole(lead, currentUser.role()));
    }

    @Operation(summary = "Get a lead's transition history")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/leads/{id}/history")
    public ApiResponse<List<LeadStatusHistoryResponse>> history(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        var history = leadQueryService.getHistory(id, currentUser.userId(), currentUser.role() == Role.ADMIN);
        return ApiResponse.of(history.stream().map(LeadStatusHistoryResponse::from).toList());
    }

    private Lead act(UUID leadId, LeadAction action, Role role, AuthenticatedUser user, LeadActionRequest request) {
        return executeLeadActionUseCase.execute(leadId, action, role, user.userId(), LeadActionRequest.noteOf(request));
    }
}
