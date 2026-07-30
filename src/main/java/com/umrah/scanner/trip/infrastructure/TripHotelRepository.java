package com.umrah.scanner.trip.infrastructure;

import com.umrah.scanner.trip.domain.TripHotel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripHotelRepository extends JpaRepository<TripHotel, UUID> {

    List<TripHotel> findAllByTripId(UUID tripId);
}
