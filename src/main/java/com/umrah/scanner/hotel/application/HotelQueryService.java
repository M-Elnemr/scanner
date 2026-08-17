package com.umrah.scanner.hotel.application;

import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.hotel.domain.Hotel;
import com.umrah.scanner.hotel.domain.HotelCity;
import com.umrah.scanner.hotel.infrastructure.HotelRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelQueryService {

    private final HotelRepository hotelRepository;

    public HotelQueryService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    /** The public picker: active hotels only, optionally scoped to one city. */
    @Transactional(readOnly = true)
    public List<Hotel> listActive(HotelCity city) {
        return city == null
                ? hotelRepository.findAllByOrderByCityAscNameAsc().stream().filter(Hotel::isActive).toList()
                : hotelRepository.findAllByCityAndActiveTrueOrderByNameAsc(city);
    }

    /** The admin listing: every hotel, retired ones included. */
    @Transactional(readOnly = true)
    public List<Hotel> listAll() {
        return hotelRepository.findAllByOrderByCityAscNameAsc();
    }

    @Transactional(readOnly = true)
    public Hotel getById(UUID id) {
        return hotelRepository.findById(id).orElseThrow(() -> NotFoundException.of("Hotel", id));
    }
}
