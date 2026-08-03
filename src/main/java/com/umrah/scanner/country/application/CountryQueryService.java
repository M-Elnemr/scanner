package com.umrah.scanner.country.application;

import com.umrah.scanner.country.domain.Country;
import com.umrah.scanner.country.infrastructure.CountryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CountryQueryService {

    private final CountryRepository countryRepository;

    public CountryQueryService(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @Transactional(readOnly = true)
    public List<Country> listAll() {
        return countryRepository.findAllByOrderByNameAsc();
    }
}
