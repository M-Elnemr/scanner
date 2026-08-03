package com.umrah.scanner.country.infrastructure;

import com.umrah.scanner.country.domain.Country;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, UUID> {

    List<Country> findAllByOrderByNameAsc();

    Optional<Country> findByIso2(String iso2);
}
