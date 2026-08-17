package com.umrah.scanner.hotel.infrastructure;

import com.umrah.scanner.hotel.domain.Hotel;
import com.umrah.scanner.hotel.domain.HotelCity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, UUID> {

    List<Hotel> findAllByOrderByCityAscNameAsc();

    List<Hotel> findAllByCityOrderByNameAsc(HotelCity city);

    List<Hotel> findAllByCityAndActiveTrueOrderByNameAsc(HotelCity city);

    Optional<Hotel> findByIdAndActiveTrue(UUID id);

    /**
     * A friendly pre-check before the insert/update reaches {@code uq_hotels_city_name}, which is
     * whitespace/case-normalized and therefore the actual source of truth. This check is looser
     * (case-insensitive only), so it catches the common case without pretending to be the real rule.
     */
    boolean existsByCityAndNameIgnoreCaseAndIdNot(HotelCity city, String name, UUID excludedId);

    boolean existsByCityAndNameIgnoreCase(HotelCity city, String name);
}
