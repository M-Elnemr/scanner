package com.umrah.scanner.hotel.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.hotel.domain.Hotel;
import com.umrah.scanner.hotel.infrastructure.HotelRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateHotelUseCase {

    private final HotelRepository hotelRepository;
    private final AuditLogService auditLogService;

    public UpdateHotelUseCase(HotelRepository hotelRepository, AuditLogService auditLogService) {
        this.hotelRepository = hotelRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Hotel execute(UUID adminUserId, UUID hotelId, UpdateHotelCommand command) {
        Hotel hotel = hotelRepository.findById(hotelId).orElseThrow(() -> NotFoundException.of("Hotel", hotelId));

        if (command.name() == null || command.name().isBlank()) {
            throw new ValidationException("name is required");
        }
        if (command.stars() < 1 || command.stars() > 5) {
            throw new ValidationException("stars must be between 1 and 5");
        }
        if (command.distanceToHaramM() != null && command.distanceToHaramM() < 0) {
            throw new ValidationException("distanceToHaramM cannot be negative");
        }
        if (hotelRepository.existsByCityAndNameIgnoreCaseAndIdNot(hotel.getCity(), command.name(), hotelId)) {
            throw new ConflictException("A " + hotel.getCity() + " hotel named \"" + command.name() + "\" already exists");
        }

        String previousName = hotel.getName();
        hotel.setName(command.name());
        hotel.setNameAr(command.nameAr());
        hotel.setStars(command.stars());
        hotel.setDistanceToHaramM(command.distanceToHaramM());
        hotel.setCanWalk(command.canWalk());
        hotel.setLocationUrl(command.locationUrl());
        hotel.setLatitude(command.latitude());
        hotel.setLongitude(command.longitude());
        hotel.setActive(command.active());

        auditLogService.record(adminUserId, "HOTEL_UPDATED", "Hotel", hotel.getId(), previousName, hotel.getName());
        return hotel;
    }
}
