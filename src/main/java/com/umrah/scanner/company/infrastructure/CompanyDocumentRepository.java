package com.umrah.scanner.company.infrastructure;

import com.umrah.scanner.company.domain.CompanyDocument;
import com.umrah.scanner.company.domain.CompanyDocumentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyDocumentRepository extends JpaRepository<CompanyDocument, UUID> {

    List<CompanyDocument> findAllByCompanyId(UUID companyId);

    List<CompanyDocument> findAllByCompanyIdAndStatus(UUID companyId, CompanyDocumentStatus status);
}
