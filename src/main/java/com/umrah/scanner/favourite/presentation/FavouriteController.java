package com.umrah.scanner.favourite.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.response.PageResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.customer.application.EnsureCustomerProfileUseCase;
import com.umrah.scanner.favourite.application.FavouriteService;
import com.umrah.scanner.favourite.domain.Favourite;
import com.umrah.scanner.trip.application.TripHotelPhotos;
import com.umrah.scanner.trip.application.TripQueryService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
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
    private final TripQueryService tripQueryService;

    public FavouriteController(
            FavouriteService favouriteService,
            EnsureCustomerProfileUseCase ensureCustomerProfileUseCase,
            TripQueryService tripQueryService) {
        this.favouriteService = favouriteService;
        this.ensureCustomerProfileUseCase = ensureCustomerProfileUseCase;
        this.tripQueryService = tripQueryService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{tripId}")
    public ApiResponse<FavouriteResponse> add(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID tripId) {
        UUID customerId = ensureCustomerProfileUseCase.execute(currentUser.userId());
        Favourite favourite = favouriteService.addFavourite(customerId, tripId);
        BigDecimal priceStartsFrom = tripQueryService.priceStartsFrom(List.of(tripId)).get(tripId);
        TripHotelPhotos hotelPhotos = tripQueryService.hotelPhotos(List.of(tripId)).getOrDefault(tripId, TripHotelPhotos.EMPTY);
        return ApiResponse.of(FavouriteResponse.from(favourite, priceStartsFrom, hotelPhotos));
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
        Page<Favourite> favourites = favouriteService.listFavourites(customerId, pageable);
        List<UUID> tripIds = favourites.getContent().stream().map(f -> f.getTrip().getId()).toList();
        Map<UUID, BigDecimal> priceStartsFrom = tripQueryService.priceStartsFrom(tripIds);
        Map<UUID, TripHotelPhotos> hotelPhotos = tripQueryService.hotelPhotos(tripIds);
        return ApiResponse.of(PageResponse.of(favourites, f -> FavouriteResponse.from(
                f, priceStartsFrom.get(f.getTrip().getId()), hotelPhotos.getOrDefault(f.getTrip().getId(), TripHotelPhotos.EMPTY))));
    }
}
