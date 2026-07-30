package com.umrah.scanner.company.application;

import com.umrah.scanner.company.domain.CompanyDocument;
import com.umrah.scanner.company.domain.CompanyDocumentStatus;
import com.umrah.scanner.company.domain.CompanyDocumentType;
import com.umrah.scanner.company.domain.CompanyProfile;
import com.umrah.scanner.company.infrastructure.CompanyDocumentRepository;
import com.umrah.scanner.company.infrastructure.CompanyProfileRepository;
import com.umrah.scanner.common.exception.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a document reference for verification review. The file itself is uploaded directly
 * to object storage by the client beforehand (pre-signed URL) — this only persists where it landed.
 */
@Service
public class UploadCompanyDocumentUseCase {

    private final CompanyDocumentRepository companyDocumentRepository;
    private final CompanyProfileRepository companyProfileRepository;

    public UploadCompanyDocumentUseCase(
            CompanyDocumentRepository companyDocumentRepository, CompanyProfileRepository companyProfileRepository) {
        this.companyDocumentRepository = companyDocumentRepository;
        this.companyProfileRepository = companyProfileRepository;
    }

    @Transactional
    public CompanyDocument execute(UUID companyUserId, CompanyDocumentType docType, String fileUrl) {
        CompanyProfile company = companyProfileRepository.findByUserId(companyUserId)
                .orElseThrow(() -> NotFoundException.of("CompanyProfile", companyUserId));

        CompanyDocument document = new CompanyDocument();
        document.setCompany(company);
        document.setDocType(docType);
        document.setFileUrl(fileUrl);
        document.setStatus(CompanyDocumentStatus.PENDING);
        return companyDocumentRepository.save(document);
    }
}
