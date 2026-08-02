package com.umrah.scanner.city.application;

import com.umrah.scanner.city.domain.City;
import com.umrah.scanner.city.infrastructure.CityRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CityQueryService {

    private final CityRepository cityRepository;

    public CityQueryService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @Transactional(readOnly = true)
    public List<City> listAll() {
        return cityRepository.findAllByOrderByNameAsc();
    }
}
