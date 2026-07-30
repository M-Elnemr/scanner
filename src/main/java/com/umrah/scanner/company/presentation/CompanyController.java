package com.umrah.scanner.company.presentation;

import com.umrah.scanner.company.application.ApproveCompanyUseCase;
import com.umrah.scanner.company.application.CompanyQueryService;
import com.umrah.scanner.company.application.RegisterCompanyCommand;
import com.umrah.scanner.company.application.RegisterCompanyUseCase;
import com.umrah.scanner.company.application.RejectCompanyUseCase;
import com.umrah.scanner.company.application.SuspendCompanyUseCase;
import com.umrah.scanner.company.application.UpdateCompanyProfileCommand;
import com.umrah.scanner.company.application.UpdateCompanyProfileUseCase;
import com.umrah.scanner.company.application.UploadCompanyDocumentUseCase;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.response.PageResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
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

@RestController
public class CompanyController {

    private final RegisterCompanyUseCase registerCompanyUseCase;
    private final UpdateCompanyProfileUseCase updateCompanyProfileUseCase;
    private final ApproveCompanyUseCase approveCompanyUseCase;
    private final RejectCompanyUseCase rejectCompanyUseCase;
    private final SuspendCompanyUseCase suspendCompanyUseCase;
    private final UploadCompanyDocumentUseCase uploadCompanyDocumentUseCase;
    private final CompanyQueryService companyQueryService;

    public CompanyController(
            RegisterCompanyUseCase registerCompanyUseCase,
            UpdateCompanyProfileUseCase updateCompanyProfileUseCase,
            ApproveCompanyUseCase approveCompanyUseCase,
            RejectCompanyUseCase rejectCompanyUseCase,
            SuspendCompanyUseCase suspendCompanyUseCase,
            UploadCompanyDocumentUseCase uploadCompanyDocumentUseCase,
            CompanyQueryService companyQueryService) {
        this.registerCompanyUseCase = registerCompanyUseCase;
        this.updateCompanyProfileUseCase = updateCompanyProfileUseCase;
        this.approveCompanyUseCase = approveCompanyUseCase;
        this.rejectCompanyUseCase = rejectCompanyUseCase;
        this.suspendCompanyUseCase = suspendCompanyUseCase;
        this.uploadCompanyDocumentUseCase = uploadCompanyDocumentUseCase;
        this.companyQueryService = companyQueryService;
    }

    // --- Self-service ---

    @PreAuthorize("hasRole('COMPANY')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/companies/me")
    public ApiResponse<CompanyResponse> register(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody RegisterCompanyRequest request) {
        var command = new RegisterCompanyCommand(
                request.companyName(), request.licenseNumber(), request.city(), request.address(),
                request.logoUrl(), request.whatsapp(), request.description(), request.phoneNumbers());
        var company = registerCompanyUseCase.execute(currentUser.userId(), command);
        return ApiResponse.of(CompanyResponse.from(company));
    }

    @PreAuthorize("hasRole('COMPANY')")
    @GetMapping("/api/v1/companies/me")
    public ApiResponse<CompanyResponse> getMine(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.of(CompanyResponse.from(companyQueryService.getByUserId(currentUser.userId())));
    }

    @PreAuthorize("hasRole('COMPANY')")
    @PutMapping("/api/v1/companies/me")
    public ApiResponse<CompanyResponse> updateMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody UpdateCompanyProfileRequest request) {
        var command = new UpdateCompanyProfileCommand(
                request.companyName(), request.city(), request.address(), request.logoUrl(), request.whatsapp(), request.description());
        var company = updateCompanyProfileUseCase.execute(currentUser.userId(), command);
        return ApiResponse.of(CompanyResponse.from(company));
    }

    @PreAuthorize("hasRole('COMPANY')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/companies/me/documents")
    public ApiResponse<CompanyDocumentResponse> uploadDocument(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody UploadCompanyDocumentRequest request) {
        var document = uploadCompanyDocumentUseCase.execute(currentUser.userId(), request.docType(), request.fileUrl());
        return ApiResponse.of(CompanyDocumentResponse.from(document));
    }

    // --- Admin ---

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/companies")
    public ApiResponse<PageResponse<CompanyResponse>> list(
            @RequestParam(defaultValue = "PENDING") CompanyStatus status, Pageable pageable) {
        return ApiResponse.of(PageResponse.of(companyQueryService.listByStatus(status, pageable), CompanyResponse::from));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/companies/{id}")
    public ApiResponse<CompanyResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(CompanyResponse.from(companyQueryService.getById(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/companies/{id}/approve")
    public ApiResponse<CompanyResponse> approve(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
        return ApiResponse.of(CompanyResponse.from(approveCompanyUseCase.execute(admin.userId(), id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/companies/{id}/reject")
    public ApiResponse<CompanyResponse> reject(
            @AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody CompanyDecisionRequest request) {
        return ApiResponse.of(CompanyResponse.from(rejectCompanyUseCase.execute(admin.userId(), id, request.reason())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/companies/{id}/suspend")
    public ApiResponse<CompanyResponse> suspend(
            @AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody CompanyDecisionRequest request) {
        return ApiResponse.of(CompanyResponse.from(suspendCompanyUseCase.execute(admin.userId(), id, request.reason())));
    }
}
