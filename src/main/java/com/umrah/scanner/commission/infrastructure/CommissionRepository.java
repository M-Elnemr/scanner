package com.umrah.scanner.commission.infrastructure;

import com.umrah.scanner.commission.domain.Commission;
import com.umrah.scanner.commission.domain.CommissionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRepository extends JpaRepository<Commission, UUID> {

    Optional<Commission> findByLeadId(UUID leadId);

    boolean existsByLeadId(UUID leadId);

    Page<Commission> findAllByCompanyIdAndStatus(UUID companyId, CommissionStatus status, Pageable pageable);
}
