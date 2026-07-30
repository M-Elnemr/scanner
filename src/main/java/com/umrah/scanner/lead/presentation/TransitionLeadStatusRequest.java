package com.umrah.scanner.lead.presentation;

import com.umrah.scanner.lead.domain.LeadStatus;
import jakarta.validation.constraints.NotNull;

public record TransitionLeadStatusRequest(@NotNull LeadStatus status) {
}
