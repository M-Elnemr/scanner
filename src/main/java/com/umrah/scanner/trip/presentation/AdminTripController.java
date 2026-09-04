package com.umrah.scanner.trip.presentation;

import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.response.PageResponse;
import com.umrah.scanner.common.security.AuthenticatedUser;
import com.umrah.scanner.trip.application.AdminTripFilter;
import com.umrah.scanner.trip.application.ChangeTripStatusUseCase;
import com.umrah.scanner.trip.application.CreateTripUseCase;
import com.umrah.scanner.trip.application.DeleteTripUseCase;
import com.umrah.scanner.trip.application.ReassignTripCompanyUseCase;
import com.umrah.scanner.trip.application.TripQueryService;
import com.umrah.scanner.trip.application.UpdateTripUseCase;
import com.umrah.scanner.trip.domain.RoomType;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.domain.TripTier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
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

/**
 * The admin's trip console — everything on {@link TripController}, minus company ownership. On a
 * separate controller rather than folded into {@code TripController} because every handler here
 * carries different authorization (an admin acts on any trip) and a different request shape (an
 * explicit {@code companyId} where the company routes imply one from the caller).
 */
@Tag(name = "Admin: Trips", description = "Full trip control across every company.")
@RestController
public class AdminTripController {

    private final CreateTripUseCase createTripUseCase;
    private final UpdateTripUseCase updateTripUseCase;
    private final ChangeTripStatusUseCase changeTripStatusUseCase;
    private final DeleteTripUseCase deleteTripUseCase;
    private final ReassignTripCompanyUseCase reassignTripCompanyUseCase;
    private final TripQueryService tripQueryService;

    public AdminTripController(
            CreateTripUseCase createTripUseCase,
            UpdateTripUseCase updateTripUseCase,
            ChangeTripStatusUseCase changeTripStatusUseCase,
            DeleteTripUseCase deleteTripUseCase,
            ReassignTripCompanyUseCase reassignTripCompanyUseCase,
            TripQueryService tripQueryService) {
        this.createTripUseCase = createTripUseCase;
        this.updateTripUseCase = updateTripUseCase;
        this.changeTripStatusUseCase = changeTripStatusUseCase;
        this.deleteTripUseCase = deleteTripUseCase;
        this.reassignTripCompanyUseCase = reassignTripCompanyUseCase;
        this.tripQueryService = tripQueryService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/v1/admin/trips")
    public ApiResponse<TripDetailResponse> create(
            @AuthenticationPrincipal AuthenticatedUser admin, @Valid @RequestBody AdminCreateTripRequest request) {
        var trip = createTripUseCase.executeAsAdmin(
                admin.userId(), request.companyId(), TripRequestMapper.toCreateCommand(request.trip()));
        return ApiResponse.of(TripDetailResponse.from(tripQueryService.ownedDetail(trip.getId())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/v1/admin/trips/{id}")
    public ApiResponse<TripDetailResponse> update(
            @AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody UpdateTripRequest request) {
        var trip = updateTripUseCase.executeAsAdmin(admin.userId(), id, TripRequestMapper.toUpdateCommand(request));
        return ApiResponse.of(TripDetailResponse.from(tripQueryService.ownedDetail(trip.getId())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/trips/{id}/status")
    public ApiResponse<TripDetailResponse> changeStatus(
            @AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody ChangeTripStatusRequest request) {
        var trip = changeTripStatusUseCase.executeAsAdmin(admin.userId(), id, request.status());
        return ApiResponse.of(TripDetailResponse.from(tripQueryService.ownedDetail(trip.getId())));
    }

    @Operation(summary = "Reassign a trip to a different company",
            description = "Existing leads on this trip keep their original company and its pricing snapshot — "
                    + "only leads created after this call follow the new owner. The target company must be APPROVED.")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/api/v1/admin/trips/{id}/company")
    public ApiResponse<TripDetailResponse> reassignCompany(
            @AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id, @Valid @RequestBody ReassignTripCompanyRequest request) {
        var trip = reassignTripCompanyUseCase.execute(admin.userId(), id, request.companyId());
        return ApiResponse.of(TripDetailResponse.from(tripQueryService.ownedDetail(trip.getId())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/api/v1/admin/trips/{id}")
    public void delete(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable UUID id) {
        deleteTripUseCase.executeAsAdmin(admin.userId(), id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/trips")
    public ApiResponse<PageResponse<AdminTripSummaryResponse>> list(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) TripStatus status,
            @RequestParam(required = false) TripTier tier,
            @RequestParam(required = false) LocalDate departureFrom,
            @RequestParam(required = false) LocalDate departureTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer roomSize,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            // Same "whole trip days" convention as the public browse endpoint — converted to
            // nights below before filtering.
            @RequestParam(required = false) Integer minDays,
            @RequestParam(required = false) Integer maxDays,
            @RequestParam(required = false) UUID cityId,
            @RequestParam(required = false) UUID departureAirportId,
            Pageable pageable) {
        var filter = new AdminTripFilter(
                companyId, status, tier, departureFrom, departureTo, search,
                RoomType.forSize(roomSize), minPrice, maxPrice,
                minDays != null ? minDays - 1 : null, maxDays != null ? maxDays - 1 : null,
                cityId, departureAirportId);
        Page<Trip> trips = tripQueryService.listForAdmin(filter, pageable);
        return ApiResponse.of(PageResponse.of(trips, AdminTripSummaryResponse::from));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/trips/{id}")
    public ApiResponse<TripDetailResponse> getOne(@PathVariable UUID id) {
        return ApiResponse.of(TripDetailResponse.from(tripQueryService.ownedDetail(id)));
    }
}
