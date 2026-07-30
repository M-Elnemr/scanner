package com.umrah.scanner.trip.application;

import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.trip.domain.Trip;
import com.umrah.scanner.trip.domain.TripStatus;
import com.umrah.scanner.trip.infrastructure.TripRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompareTripsUseCase {

    private static final int MAX_TRIPS = 4;

    private final TripRepository tripRepository;

    public CompareTripsUseCase(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @Transactional(readOnly = true)
    public List<Trip> execute(List<java.util.UUID> tripIds) {
        if (tripIds == null || tripIds.isEmpty()) {
            throw new ValidationException("At least one trip id is required");
        }
        if (tripIds.size() > MAX_TRIPS) {
            throw new ValidationException("At most " + MAX_TRIPS + " trips can be compared at once");
        }
        return tripRepository.findAllByIdInAndStatus(tripIds, TripStatus.PUBLISHED);
    }
}
