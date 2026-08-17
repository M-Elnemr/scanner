package com.umrah.scanner.hotel.application;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.exception.ConflictException;
import com.umrah.scanner.common.exception.ValidationException;
import com.umrah.scanner.hotel.domain.Hotel;
import com.umrah.scanner.hotel.infrastructure.HotelRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateHotelUseCase {

    private final HotelRepository hotelRepository;
    private final AuditLogService auditLogService;

    public CreateHotelUseCase(HotelRepository hotelRepository, AuditLogService auditLogService) {
        this.hotelRepository = hotelRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Hotel execute(UUID adminUserId, HotelCommand command) {
        validate(command);
        if (hotelRepository.existsByCityAndNameIgnoreCase(command.city(), command.name())) {
            throw new ConflictException("A " + command.city() + " hotel named \"" + command.name() + "\" already exists");
        }

        Hotel hotel = new Hotel();
        apply(hotel, command);

        hotel = hotelRepository.save(hotel);
        auditLogService.record(adminUserId, "HOTEL_CREATED", "Hotel", hotel.getId(), null, hotel.getName());
        return hotel;
    }

    static void validate(HotelCommand command) {
        if (command.city() == null) {
            throw new ValidationException("city is required");
        }
        if (command.name() == null || command.name().isBlank()) {
            throw new ValidationException("name is required");
        }
        if (command.stars() < 1 || command.stars() > 5) {
            throw new ValidationException("stars must be between 1 and 5");
        }
        if (command.distanceToHaramM() != null && command.distanceToHaramM() < 0) {
            throw new ValidationException("distanceToHaramM cannot be negative");
        }
    }

    static void apply(Hotel hotel, HotelCommand command) {
        hotel.setCity(command.city());
        hotel.setName(command.name());
        hotel.setNameAr(command.nameAr());
        hotel.setStars(command.stars());
        hotel.setDistanceToHaramM(command.distanceToHaramM());
        hotel.setCanWalk(command.canWalk());
        hotel.setLocationUrl(command.locationUrl());
        hotel.setActive(command.active());
    }
}
