package com.umrah.scanner.trip.infrastructure;

import com.umrah.scanner.trip.domain.RoomPrice;
import com.umrah.scanner.trip.domain.RoomType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomPriceRepository extends JpaRepository<RoomPrice, UUID> {

    List<RoomPrice> findAllByTripId(UUID tripId);

    Optional<RoomPrice> findByTripIdAndRoomType(UUID tripId, RoomType roomType);

    List<RoomPrice> findAllByTripIdInAndRoomType(List<UUID> tripIds, RoomType roomType);
}
