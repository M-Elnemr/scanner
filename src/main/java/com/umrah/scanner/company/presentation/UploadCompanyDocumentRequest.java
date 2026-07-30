package com.umrah.scanner.company.presentation;

import com.umrah.scanner.company.domain.CompanyDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** {@code fileUrl} points at an object already uploaded to storage (e.g. via a pre-signed URL) — no file bytes travel through this API. */
public record UploadCompanyDocumentRequest(@NotNull CompanyDocumentType docType, @NotBlank String fileUrl) {
}
