package com.umrah.scanner.customer.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.customer.application.CustomerQueryService;
import com.umrah.scanner.customer.application.UpdateCustomerProfileCommand;
import com.umrah.scanner.customer.application.UpdateCustomerProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/me")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final UpdateCustomerProfileUseCase updateCustomerProfileUseCase;
    private final CustomerQueryService customerQueryService;

    public CustomerController(UpdateCustomerProfileUseCase updateCustomerProfileUseCase, CustomerQueryService customerQueryService) {
        this.updateCustomerProfileUseCase = updateCustomerProfileUseCase;
        this.customerQueryService = customerQueryService;
    }

    @GetMapping
    public ApiResponse<CustomerResponse> getMine(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ApiResponse.of(customerQueryService.findByUserId(currentUser.userId())
                .map(CustomerResponse::from)
                .orElseGet(CustomerResponse::empty));
    }

    @PutMapping
    public ApiResponse<CustomerResponse> updateMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody UpdateCustomerProfileRequest request) {
        var command = new UpdateCustomerProfileCommand(request.fullName(), request.phone());
        return ApiResponse.of(CustomerResponse.from(updateCustomerProfileUseCase.execute(currentUser.userId(), command)));
    }
}
