package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.response.PageResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.company.application.CompanyQueryService;
import com.umrah.scanner.customer.application.CustomerQueryService;
import com.umrah.scanner.trip.application.ChangeTripStatusUseCase;
import com.umrah.scanner.trip.application.CompareTripsUseCase;
import com.umrah.scanner.trip.application.CreateTripCommand;
import com.umrah.scanner.trip.application.CreateTripUseCase;
import com.umrah.scanner.trip.application.DeleteTripUseCase;
import com.umrah.scanner.trip.application.RoomPriceInput;
import com.umrah.scanner.trip.application.TripHotelInput;
import com.umrah.scanner.trip.application.TripQueryService;
import com.umrah.scanner.trip.application.UpdateTripCommand;
import com.umrah.scanner.trip.application.UpdateTripUseCase;
import com.umrah.scanner.trip.application.UpsertRoomPricesUseCase;
import com.umrah.scanner.trip.domain.Trip;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
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

@RestController
public class TripController {

    private static final String DEFAULT_CURRENCY = "EGP";

    private final CreateTripUseCase createTripUseCase;
    private final UpdateTripUseCase updateTripUseCase;
    private final UpsertRoomPricesUseCase upsertRoomPricesUseCase;
    private final ChangeTripStatusUseCase changeTripStatusUseCase;
    private final DeleteTripUseCase deleteTripUseCase;
    private final CompareTripsUseCase compareTripsUseCase;
    private final TripQueryService tripQueryService;
    private final CompanyQueryService companyQueryService;
    private final CustomerQueryService customerQueryService;

    public TripController(
            CreateTripUseCase createTripUseCase,
            UpdateTripUseCase updateTripUseCase,
            UpsertRoomPricesUseCase upsertRoomPricesUseCase,
            ChangeTripStatusUseCase changeTripStatusUseCase,
            DeleteTripUseCase deleteTripUseCase,
            CompareTripsUseCase compareTripsUseCase,
            TripQueryService tripQueryService,
            CompanyQueryService companyQueryService,
            CustomerQueryService customerQueryService) {
        this.createTripUseCase = createTripUseCase;
        this.updateTripUseCase = updateTripUseCase;
        this.upsertRoomPricesUseCase = upsertRoomPricesUseCase;
        this.changeTripStatusUseCase = changeTripStatusUseCase;
        this.deleteTripUseCase = deleteTripUseCase;
        this.compareTripsUseCase = compareTripsUseCase;
        this.tripQueryService = tripQueryService;
        this.companyQueryService = companyQueryService;
        this.customerQueryService = customerQueryService;
    }

    // --- Company self-service ---

    @PreAuthorize("hasRole('COMPANY')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/companies/me/trips")
    public ApiResponse<TripDetailResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody CreateTripRequest request) {
        var trip = createTripUseCase.execute(currentUser.userId(), toCreateCommand(request));
        return ApiResponse.of(TripDetailResponse.ownedBy(trip));
    }

    @PreAuthorize("hasRole('COMPANY')")
    @PutMapping("/api/v1/companies/me/trips/{id}")
    public ApiResponse<TripDetailResponse> update(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id, @Valid @RequestBody UpdateTripRequest request) {
        var trip = updateTripUseCase.execute(currentUser.userId(), id, toUpdateCommand(request));
        return ApiResponse.of(TripDetailResponse.ownedBy(trip));
    }

    @PreAuthorize("hasRole('COMPANY')")
    @PutMapping("/api/v1/companies/me/trips/{id}/room-prices")
    public ApiResponse<TripDetailResponse> upsertRoomPrices(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id, @Valid @RequestBody RoomPricesRequest request) {
        List<RoomPriceInput> inputs = request.prices().stream()
                .map(p -> new RoomPriceInput(p.roomType(), p.price()))
                .toList();
        var trip = upsertRoomPricesUseCase.execute(currentUser.userId(), id, inputs);
        return ApiResponse.of(TripDetailResponse.ownedBy(trip));
    }

    @PreAuthorize("hasRole('COMPANY')")
    @PatchMapping("/api/v1/companies/me/trips/{id}/status")
    public ApiResponse<TripDetailResponse> changeStatus(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id, @Valid @RequestBody ChangeTripStatusRequest request) {
        var trip = changeTripStatusUseCase.execute(currentUser.userId(), id, request.status());
        return ApiResponse.of(TripDetailResponse.ownedBy(trip));
    }

    @PreAuthorize("hasRole('COMPANY')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/api/v1/companies/me/trips/{id}")
    public void delete(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        deleteTripUseCase.execute(currentUser.userId(), id);
    }

    @PreAuthorize("hasRole('COMPANY')")
    @GetMapping("/api/v1/companies/me/trips")
    public ApiResponse<PageResponse<TripSummaryResponse>> listMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser, Pageable pageable) {
        UUID companyId = companyQueryService.getByUserId(currentUser.userId()).getId();
        Page<Trip> trips = tripQueryService.listForCompany(companyId, pageable);
        Map<UUID, BigDecimal> priceStartsFrom = tripQueryService.priceStartsFrom(tripIds(trips));
        return ApiResponse.of(PageResponse.of(trips, trip -> TripSummaryResponse.from(trip, priceStartsFrom.get(trip.getId()))));
    }

    @PreAuthorize("hasRole('COMPANY')")
    @GetMapping("/api/v1/companies/me/trips/{id}")
    public ApiResponse<TripDetailResponse> getMine(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        UUID companyId = companyQueryService.getByUserId(currentUser.userId()).getId();
        return ApiResponse.of(TripDetailResponse.ownedBy(tripQueryService.getOwnedDetail(companyId, id)));
    }

    // --- Public browsing ---

    @GetMapping("/api/v1/trips")
    public ApiResponse<PageResponse<TripSummaryResponse>> browse(Pageable pageable) {
        Page<Trip> trips = tripQueryService.browsePublished(pageable);
        Map<UUID, BigDecimal> priceStartsFrom = tripQueryService.priceStartsFrom(tripIds(trips));
        return ApiResponse.of(PageResponse.of(trips, trip -> TripSummaryResponse.from(trip, priceStartsFrom.get(trip.getId()))));
    }

    @GetMapping("/api/v1/trips/{id}")
    public ApiResponse<TripDetailResponse> getDetail(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable UUID id) {
        Optional<UUID> viewingCustomerId = resolveCustomerProfileId(currentUser);
        return ApiResponse.of(TripDetailResponse.from(tripQueryService.getPublicDetail(id, viewingCustomerId)));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/api/v1/trips/compare")
    public ApiResponse<List<TripSummaryResponse>> compare(@RequestParam List<UUID> ids) {
        List<Trip> trips = compareTripsUseCase.execute(ids);
        Map<UUID, BigDecimal> priceStartsFrom = tripQueryService.priceStartsFrom(trips.stream().map(Trip::getId).toList());
        return ApiResponse.of(trips.stream().map(trip -> TripSummaryResponse.from(trip, priceStartsFrom.get(trip.getId()))).toList());
    }

    private List<UUID> tripIds(Page<Trip> trips) {
        return trips.getContent().stream().map(Trip::getId).toList();
    }

    private Optional<UUID> resolveCustomerProfileId(AuthenticatedUser currentUser) {
        if (currentUser == null || currentUser.role() != com.umrah.scanner.user.domain.Role.CUSTOMER) {
            return Optional.empty();
        }
        return customerQueryService.findByUserId(currentUser.userId()).map(profile -> profile.getId());
    }

    private CreateTripCommand toCreateCommand(CreateTripRequest r) {
        return new CreateTripCommand(
                r.tripCode(), r.title(), r.departureDate(), r.returnDate(), r.departureAirport(), r.arrivalAirport(),
                r.airline(), r.flightNumber(), r.transitCount(), r.transitCity(), r.transitDuration(),
                r.daysInMakkah(), r.daysInMadinah(), r.visaIncluded(), r.transportationIncluded(), r.mealsIncluded(),
                r.guideIncluded(), r.zamzamIncluded(), r.description(), currencyOrDefault(r.currency()), r.availableSeats(),
                toHotelInputs(r.hotels()), toRoomPriceInputs(r.prices()), r.tier());
    }

    private UpdateTripCommand toUpdateCommand(UpdateTripRequest r) {
        return new UpdateTripCommand(
                r.title(), r.departureDate(), r.returnDate(), r.departureAirport(), r.arrivalAirport(),
                r.airline(), r.flightNumber(), r.transitCount(), r.transitCity(), r.transitDuration(),
                r.daysInMakkah(), r.daysInMadinah(), r.visaIncluded(), r.transportationIncluded(), r.mealsIncluded(),
                r.guideIncluded(), r.zamzamIncluded(), r.description(), currencyOrDefault(r.currency()), r.availableSeats(),
                toHotelInputs(r.hotels()), toRoomPriceInputs(r.prices()), r.tier());
    }

    private String currencyOrDefault(String currency) {
        return (currency == null || currency.isBlank()) ? DEFAULT_CURRENCY : currency;
    }

    // null means "omitted" (leave untouched on update); an empty list is an explicit clear.
    private List<TripHotelInput> toHotelInputs(List<TripHotelRequest> hotels) {
        if (hotels == null) {
            return null;
        }
        return hotels.stream()
                .map(h -> new TripHotelInput(h.city(), h.hotelName(), h.stars(), h.distanceToHaramM(), h.locationUrl()))
                .toList();
    }

    // null means "omitted" (leave untouched on update); an empty list is an explicit clear.
    private List<RoomPriceInput> toRoomPriceInputs(List<RoomPriceRequest> prices) {
        if (prices == null) {
            return null;
        }
        return prices.stream().map(p -> new RoomPriceInput(p.roomType(), p.price())).toList();
    }
}
