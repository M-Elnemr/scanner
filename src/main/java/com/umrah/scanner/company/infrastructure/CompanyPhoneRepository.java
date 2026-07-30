package com.umrah.scanner.company.infrastructure;

import com.umrah.scanner.company.domain.CompanyPhone;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyPhoneRepository extends JpaRepository<CompanyPhone, UUID> {

    List<CompanyPhone> findAllByCompanyId(UUID companyId);

    Optional<CompanyPhone> findByCompanyIdAndPrimaryTrue(UUID companyId);

    boolean existsByCompanyIdAndPhoneNumber(UUID companyId, String phoneNumber);
}
