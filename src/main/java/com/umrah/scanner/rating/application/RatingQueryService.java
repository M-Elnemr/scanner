package com.umrah.scanner.rating.application;

import com.umrah.scanner.rating.domain.Rating;
import com.umrah.scanner.rating.infrastructure.RatingRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RatingQueryService {

    private final RatingRepository ratingRepository;

    public RatingQueryService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @Transactional(readOnly = true)
    public Page<Rating> listForCompany(UUID companyId, Pageable pageable) {
        return ratingRepository.findAllByCompanyId(companyId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Rating> listForTrip(UUID tripId, Pageable pageable) {
        return ratingRepository.findAllByTripId(tripId, pageable);
    }
}
