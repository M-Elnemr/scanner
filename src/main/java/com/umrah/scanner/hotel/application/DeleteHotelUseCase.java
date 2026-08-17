package com.umrah.scanner.hotel.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.hotel.domain.Hotel;
import com.umrah.scanner.hotel.infrastructure.HotelRepository;
import com.umrah.scanner.trip.infrastructure.TripHotelRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A hard(er) delete than {@link UpdateHotelUseCase}'s {@code active=false}: this is for a hotel
 * created by mistake, not one that is simply no longer offered. Blocked while any trip still uses
 * it — {@code Hotel} carries {@code @SQLRestriction("deleted_at is null")} and {@code TripHotel.hotel}
 * is a non-optional association, so soft-deleting a referenced hotel would make every trip using it
 * unloadable.
 */
@Service
public class DeleteHotelUseCase {

    private final HotelRepository hotelRepository;
    private final TripHotelRepository tripHotelRepository;
    private final AuditLogService auditLogService;

    public DeleteHotelUseCase(
            HotelRepository hotelRepository, TripHotelRepository tripHotelRepository, AuditLogService auditLogService) {
        this.hotelRepository = hotelRepository;
        this.tripHotelRepository = tripHotelRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void execute(UUID adminUserId, UUID hotelId) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> NotFoundException.of("Hotel", hotelId));

        long usedByTrips = tripHotelRepository.countByHotelId(hotelId);
        if (usedByTrips > 0) {
            throw new ConflictException(
                    "This hotel is used by " + usedByTrips + " trip(s). Mark it inactive instead of deleting it.");
        }

        hotel.markDeleted(Instant.now());
        auditLogService.record(adminUserId, "HOTEL_DELETED", "Hotel", hotelId, hotel.getName(), null);
    }
}
