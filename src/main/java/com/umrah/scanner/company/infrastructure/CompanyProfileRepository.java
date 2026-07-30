package com.umrah.scanner.company.infrastructure;

import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.domain.CompanyStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID>,
        JpaSpecificationExecutor<CompanyProfile> {

    Optional<CompanyProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    Page<CompanyProfile> findAllByStatus(CompanyStatus status, Pageable pageable);
}
