package com.umrah.scanner.company.presentation;

import com.umrah.scanner.company.domain.CompanyDocument;
import com.umrah.scanner.company.domain.CompanyDocumentStatus;
import com.umrah.scanner.company.domain.CompanyDocumentType;
import java.time.Instant;
import java.util.UUID;

public record CompanyDocumentResponse(UUID id, CompanyDocumentType docType, String fileUrl, CompanyDocumentStatus status, Instant createdAt) {

    public static CompanyDocumentResponse from(CompanyDocument document) {
        return new CompanyDocumentResponse(
                document.getId(), document.getDocType(), document.getFileUrl(), document.getStatus(), document.getCreatedAt());
    }
}
