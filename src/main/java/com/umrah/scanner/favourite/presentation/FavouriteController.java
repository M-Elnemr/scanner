package com.umrah.scanner.favourite.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.response.PageResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.customer.application.EnsureCustomerProfileUseCase;
import com.umrah.scanner.favourite.application.FavouriteService;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/me/favourites")
@PreAuthorize("hasRole('CUSTOMER')")
public class FavouriteController {

    private final FavouriteService favouriteService;
    private final EnsureCustomerProfileUseCase ensureCustomerProfileUseCase;

    public FavouriteController(FavouriteService favouriteService, EnsureCustomerProfileUseCase ensureCustomerProfileUseCase) {
        this.favouriteService = favouriteService;
        this.ensureCustomerProfileUseCase = ensureCustomerProfileUseCase;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{tripId}")
    public ApiResponse<FavouriteResponse> add(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID tripId) {
        UUID customerId = ensureCustomerProfileUseCase.execute(currentUser.userId());
        return ApiResponse.of(FavouriteResponse.from(favouriteService.addFavourite(customerId, tripId)));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{tripId}")
    public void remove(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID tripId) {
        UUID customerId = ensureCustomerProfileUseCase.execute(currentUser.userId());
        favouriteService.removeFavourite(customerId, tripId);
    }

    @GetMapping
    public ApiResponse<PageResponse<FavouriteResponse>> list(@AuthenticationPrincipal AuthenticatedUser currentUser, Pageable pageable) {
        UUID customerId = ensureCustomerProfileUseCase.execute(currentUser.userId());
        return ApiResponse.of(PageResponse.of(favouriteService.listFavourites(customerId, pageable), FavouriteResponse::from));
    }
}
