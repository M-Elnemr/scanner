package com.umrah.scanner.city.infrastructure;

import com.umrah.scanner.city.domain.City;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, UUID> {

    List<City> findAllByOrderByNameAsc();
}
