package com.umrah.scanner.rating.infrastructure;

import com.umrah.scanner.rating.domain.Rating;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RatingRepository extends JpaRepository<Rating, UUID> {

    boolean existsByLeadId(UUID leadId);

    Optional<Rating> findByLeadId(UUID leadId);

    Page<Rating> findAllByCompanyId(UUID companyId, Pageable pageable);

    Page<Rating> findAllByTripId(UUID tripId, Pageable pageable);

    long countByCompanyId(UUID companyId);

    @Query("select avg(r.stars) from Rating r where r.company.id = :companyId")
    Double avgStarsByCompanyId(@Param("companyId") UUID companyId);
}
