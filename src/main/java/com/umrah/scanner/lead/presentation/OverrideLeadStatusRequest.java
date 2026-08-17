package com.umrah.scanner.lead.presentation;

import com.umrah.scanner.lead.domain.LeadStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OverrideLeadStatusRequest(
        @NotNull LeadStatus status,
        @NotBlank @Size(max = 500) String reason) {
}
