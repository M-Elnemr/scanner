package com.umrah.scanner.trip.infrastructure;

import com.umrah.scanner.trip.domain.TripHotel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TripHotelRepository extends JpaRepository<TripHotel, UUID> {

    List<TripHotel> findAllByTripId(UUID tripId);

    /** Fetch-joined so a list of many trips' hotel photos costs one query, not one per trip. */
    @Query("select th from TripHotel th join fetch th.hotel where th.trip.id in :tripIds")
    List<TripHotel> findAllByTripIdInWithHotel(@Param("tripIds") List<UUID> tripIds);

    /** The delete guard for a catalogue hotel: refuse while any trip still uses it. */
    long countByHotelId(UUID hotelId);
}
