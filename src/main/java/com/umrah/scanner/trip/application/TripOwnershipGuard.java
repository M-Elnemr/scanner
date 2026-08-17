package com.umrah.scanner.trip.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Resolves a trip owned by the calling company user, or fails — shared by every company-side trip use case. */
@Component
public class TripOwnershipGuard {

    private final TripRepository tripRepository;
    private final CompanyProfileRepository companyProfileRepository;

    public TripOwnershipGuard(TripRepository tripRepository, CompanyProfileRepository companyProfileRepository) {
        this.tripRepository = tripRepository;
        this.companyProfileRepository = companyProfileRepository;
    }

    @Transactional(readOnly = true)
    public Trip findOwnedTrip(UUID companyUserId, UUID tripId) {
        UUID companyId = companyProfileRepository.findByUserId(companyUserId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyUserId))
                .getId();
        return tripRepository.findByIdAndCompanyId(tripId, companyId)
                .orElseThrow(() -> NotFoundException.of("Trip", tripId));
    }

    /** Admin reach: ownership is not checked, because an admin owns nothing and reaches everything. */
    @Transactional(readOnly = true)
    public Trip findAnyTrip(UUID tripId) {
        return tripRepository.findById(tripId).orElseThrow(() -> NotFoundException.of("Trip", tripId));
    }
}
