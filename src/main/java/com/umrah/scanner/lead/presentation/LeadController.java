package com.umrah.scanner.lead.presentation;

import com.umrah.scanner.cashback.application.SendCashbackUseCase;
import com.umrah.scanner.cashback.presentation.CashbackResponse;
import com.umrah.scanner.cashback.presentation.SendCashbackRequest;
import com.umrah.scanner.commission.application.ReleaseCommissionUseCase;
import com.umrah.scanner.commission.presentation.CommissionResponse;
import com.umrah.scanner.commission.presentation.ReleaseCommissionRequest;
import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.response.PageResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.lead.application.CreateLeadUseCase;
import com.umrah.scanner.lead.application.LeadQueryService;
import com.umrah.scanner.lead.application.TransitionLeadStatusUseCase;
import com.umrah.scanner.lead.domain.LeadStatus;
import com.umrah.scanner.user.domain.Role;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LeadController {

    private final CreateLeadUseCase createLeadUseCase;
    private final TransitionLeadStatusUseCase transitionLeadStatusUseCase;
    private final ReleaseCommissionUseCase releaseCommissionUseCase;
    private final SendCashbackUseCase sendCashbackUseCase;
    private final LeadQueryService leadQueryService;

    public LeadController(
            CreateLeadUseCase createLeadUseCase,
            TransitionLeadStatusUseCase transitionLeadStatusUseCase,
            ReleaseCommissionUseCase releaseCommissionUseCase,
            SendCashbackUseCase sendCashbackUseCase,
            LeadQueryService leadQueryService) {
        this.createLeadUseCase = createLeadUseCase;
        this.transitionLeadStatusUseCase = transitionLeadStatusUseCase;
        this.releaseCommissionUseCase = releaseCommissionUseCase;
        this.sendCashbackUseCase = sendCashbackUseCase;
        this.leadQueryService = leadQueryService;
    }

    // --- Customer ---

    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/trips/{tripId}/contact-company")
    public ApiResponse<LeadResponse> contactCompany(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID tripId) {
        return ApiResponse.of(LeadResponse.from(createLeadUseCase.execute(currentUser.userId(), tripId)));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/api/v1/customers/me/leads")
    public ApiResponse<PageResponse<LeadResponse>> listMineAsCustomer(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @RequestParam(required = false) LeadStatus status, Pageable pageable) {
        return ApiResponse.of(PageResponse.of(leadQueryService.listForCustomer(currentUser.userId(), status, pageable), LeadResponse::from));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/api/v1/customers/me/leads/{id}/confirm-payment")
    public ApiResponse<LeadResponse> confirmPayment(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        return ApiResponse.of(LeadResponse.from(transitionLeadStatusUseCase.confirmPayment(id, currentUser.userId())));
    }

    // --- Company ---

    @PreAuthorize("hasRole('COMPANY')")
    @GetMapping("/api/v1/companies/me/leads")
    public ApiResponse<PageResponse<LeadResponse>> listMineAsCompany(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @RequestParam(required = false) LeadStatus status, Pageable pageable) {
        return ApiResponse.of(PageResponse.of(leadQueryService.listForCompany(currentUser.userId(), status, pageable), LeadResponse::from));
    }

    @PreAuthorize("hasRole('COMPANY')")
    @PatchMapping("/api/v1/companies/me/leads/{id}/status")
    public ApiResponse<LeadResponse> updateStatusAsCompany(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id, @Valid @RequestBody TransitionLeadStatusRequest request) {
        var lead = transitionLeadStatusUseCase.execute(id, request.status(), Role.COMPANY, currentUser.userId());
        return ApiResponse.of(LeadResponse.from(lead));
    }

    // --- Admin ---

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/leads")
    public ApiResponse<PageResponse<LeadResponse>> listForAdmin(
            @RequestParam(required = false) LeadStatus status, Pageable pageable) {
        return ApiResponse.of(PageResponse.of(leadQueryService.listForAdmin(status, pageable), LeadResponse::from));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/leads/{id}/release-commission")
    public ApiResponse<CommissionResponse> releaseCommission(
            @AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody ReleaseCommissionRequest request) {
        var commission = releaseCommissionUseCase.execute(admin.userId(), id, request.amount());
        return ApiResponse.of(CommissionResponse.from(commission));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/leads/{id}/send-cashback")
    public ApiResponse<CashbackResponse> sendCashback(
            @AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody SendCashbackRequest request) {
        var cashback = sendCashbackUseCase.execute(admin.userId(), id, request.amount());
        return ApiResponse.of(CashbackResponse.from(cashback));
    }

    // --- Shared ---

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/leads/{id}/history")
    public ApiResponse<List<LeadStatusHistoryResponse>> history(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        var history = leadQueryService.getHistory(id, currentUser.userId(), currentUser.role() == Role.ADMIN);
        return ApiResponse.of(history.stream().map(LeadStatusHistoryResponse::from).toList());
    }
}
