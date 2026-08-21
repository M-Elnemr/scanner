package com.umrah.scanner.company.presentation;

import com.umrah.scanner.company.application.AdminCreateCompanyCommand;
import com.umrah.scanner.company.application.AdminCreateCompanyUseCase;
import com.umrah.scanner.company.application.AdminUpdateCompanyProfileCommand;
import com.umrah.scanner.company.application.ApproveCompanyUseCase;
import com.umrah.scanner.company.application.CompanyAddressInput;
import com.umrah.scanner.company.application.CompanyQueryService;
import com.umrah.scanner.company.application.DeleteCompanyUseCase;
import com.umrah.scanner.company.application.RegisterCompanyCommand;
import com.umrah.scanner.company.application.RegisterCompanyUseCase;
import com.umrah.scanner.company.application.RejectCompanyUseCase;
import com.umrah.scanner.company.application.ReinstateCompanyUseCase;
import com.umrah.scanner.company.application.SetCompanyCommissionUseCase;
import com.umrah.scanner.company.application.SuspendCompanyUseCase;
import com.umrah.scanner.company.application.UpdateCompanyProfileCommand;
import com.umrah.scanner.company.application.UpdateCompanyProfileUseCase;
import com.umrah.scanner.company.application.UploadCompanyDocumentUseCase;
import com.umrah.scanner.company.domain.CompanyStatus;
import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.response.PageResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Companies", description = "Company profiles, verification, and admin-configured commission.")
@RestController
public class CompanyController {

    private final RegisterCompanyUseCase registerCompanyUseCase;
    private final UpdateCompanyProfileUseCase updateCompanyProfileUseCase;
    private final ApproveCompanyUseCase approveCompanyUseCase;
    private final RejectCompanyUseCase rejectCompanyUseCase;
    private final SuspendCompanyUseCase suspendCompanyUseCase;
    private final ReinstateCompanyUseCase reinstateCompanyUseCase;
    private final AdminCreateCompanyUseCase adminCreateCompanyUseCase;
    private final DeleteCompanyUseCase deleteCompanyUseCase;
    private final UploadCompanyDocumentUseCase uploadCompanyDocumentUseCase;
    private final SetCompanyCommissionUseCase setCompanyCommissionUseCase;
    private final CompanyQueryService companyQueryService;

    public CompanyController(
            RegisterCompanyUseCase registerCompanyUseCase,
            UpdateCompanyProfileUseCase updateCompanyProfileUseCase,
            ApproveCompanyUseCase approveCompanyUseCase,
            RejectCompanyUseCase rejectCompanyUseCase,
            SuspendCompanyUseCase suspendCompanyUseCase,
            ReinstateCompanyUseCase reinstateCompanyUseCase,
            AdminCreateCompanyUseCase adminCreateCompanyUseCase,
            DeleteCompanyUseCase deleteCompanyUseCase,
            UploadCompanyDocumentUseCase uploadCompanyDocumentUseCase,
            SetCompanyCommissionUseCase setCompanyCommissionUseCase,
            CompanyQueryService companyQueryService) {
        this.registerCompanyUseCase = registerCompanyUseCase;
        this.updateCompanyProfileUseCase = updateCompanyProfileUseCase;
        this.approveCompanyUseCase = approveCompanyUseCase;
        this.rejectCompanyUseCase = rejectCompanyUseCase;
        this.suspendCompanyUseCase = suspendCompanyUseCase;
        this.reinstateCompanyUseCase = reinstateCompanyUseCase;
        this.adminCreateCompanyUseCase = adminCreateCompanyUseCase;
        this.deleteCompanyUseCase = deleteCompanyUseCase;
        this.uploadCompanyDocumentUseCase = uploadCompanyDocumentUseCase;
        this.setCompanyCommissionUseCase = setCompanyCommissionUseCase;
        this.companyQueryService = companyQueryService;
    }

    // --- Self-service ---

    @PreAuthorize("hasRole('COMPANY')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/companies/me")
    public ApiResponse<CompanyResponse> register(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody RegisterCompanyRequest request) {
        var command = new RegisterCompanyCommand(
                request.companyName(), request.licenseNumber(), request.whatsapp(),
                request.description(), toAddressInputs(request.addresses()));
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
                request.companyName(), request.whatsapp(), request.description(),
                toAddressInputs(request.addresses()));
        var company = updateCompanyProfileUseCase.executeAsCompany(currentUser.userId(), command);
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

    // --- Customer-facing ---

    @Operation(summary = "Get a company's profile",
            description = "Branches, contact numbers, licence and rating. Visible to a customer only once they have "
                    + "contacted this company about a trip, to the company itself, and to admins. Contains no "
                    + "commission or verification data — see the PublicCompany schema.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Company profile"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Caller has no lead with this company")})
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/api/v1/companies/{id}")
    public ApiResponse<PublicCompanyResponse> getPublicById(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        var company = companyQueryService.getVisibleTo(id, currentUser.userId(), currentUser.role());
        return ApiResponse.of(PublicCompanyResponse.from(company));
    }

    // --- Admin ---

    @Operation(summary = "List companies",
            description = "Both status and search are optional — omit status to see every company, not just PENDING.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/companies")
    public ApiResponse<PageResponse<CompanyResponse>> list(
            @RequestParam(required = false) CompanyStatus status,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ApiResponse.of(PageResponse.of(companyQueryService.listForAdmin(status, search, pageable), CompanyResponse::from));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/companies/{id}")
    public ApiResponse<CompanyResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(CompanyResponse.from(companyQueryService.getById(id)));
    }

    @Operation(summary = "Create a company from scratch",
            description = "Provisions the owner's account if one does not already exist for ownerEmail, using a "
                    + "placeholder Google subject the owner's first real sign-in replaces. autoApprove=true skips "
                    + "the PENDING queue, since the admin creating it is itself the vetting step.")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/admin/companies")
    public ApiResponse<CompanyResponse> adminCreate(
            @AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody AdminCreateCompanyRequest request) {
        var command = new AdminCreateCompanyCommand(
                request.ownerEmail(), request.companyName(), request.licenseNumber(),
                request.whatsapp(), request.description(), toAddressInputs(request.addresses()),
                request.commissionPerTraveler(), request.autoApprove());
        var company = adminCreateCompanyUseCase.execute(admin.userId(), command);
        return ApiResponse.of(CompanyResponse.from(company));
    }

    @Operation(summary = "Edit any company's profile",
            description = "Unlike the self-service update, this may also change licenseNumber and works on a "
                    + "suspended company.")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/v1/admin/companies/{id}")
    public ApiResponse<CompanyResponse> adminUpdate(@PathVariable UUID id, @Valid @RequestBody AdminUpdateCompanyProfileRequest request) {
        var profile = new UpdateCompanyProfileCommand(
                request.companyName(), request.whatsapp(), request.description(),
                toAddressInputs(request.addresses()));
        var company = updateCompanyProfileUseCase.executeAsAdmin(id, new AdminUpdateCompanyProfileCommand(request.licenseNumber(), profile));
        return ApiResponse.of(CompanyResponse.from(company));
    }

    @Operation(summary = "Delete a company",
            description = "Refuses with 409 while it has live bookings or an unsettled commission. Otherwise "
                    + "soft-deletes the company, its trips, and suspends its owner's account.")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/api/v1/admin/companies/{id}")
    public void adminDelete(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
        deleteCompanyUseCase.execute(admin.userId(), id);
    }

    @Operation(summary = "Reinstate a suspended company", description = "Restores it straight to APPROVED.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/companies/{id}/reinstate")
    public ApiResponse<CompanyResponse> reinstate(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
        return ApiResponse.of(CompanyResponse.from(reinstateCompanyUseCase.execute(admin.userId(), id)));
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

    @Operation(summary = "Set a company's commission per traveler",
            description = "Admin-only. Companies can read this value on their own profile but can never change it. "
                    + "Takes effect for leads created after the change; existing leads keep the amounts they were priced with.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/companies/{id}/commission")
    public ApiResponse<CompanyResponse> setCommission(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyCommissionRequest request) {
        var company = setCompanyCommissionUseCase.execute(admin.userId(), id, request.commissionPerTraveler());
        return ApiResponse.of(CompanyResponse.from(company));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/companies/{id}/suspend")
    public ApiResponse<CompanyResponse> suspend(
            @AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody CompanyDecisionRequest request) {
        return ApiResponse.of(CompanyResponse.from(suspendCompanyUseCase.execute(admin.userId(), id, request.reason())));
    }

    private List<CompanyAddressInput> toAddressInputs(List<CompanyAddressRequest> addresses) {
        return addresses.stream().map(r -> new CompanyAddressInput(r.cityId(), r.addressText(), r.mobileNumber())).toList();
    }
}
