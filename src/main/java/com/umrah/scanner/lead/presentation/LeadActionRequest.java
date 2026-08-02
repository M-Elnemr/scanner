package com.umrah.scanner.lead.presentation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/** Optional free-text context stored on the resulting status-history entry. */
@Schema(name = "LeadActionRequest", description = "Optional note recorded against the lifecycle step.")
public record LeadActionRequest(@Size(max = 500) String note) {

    /** The body is optional on every action endpoint, so a missing request means "no note". */
    public static String noteOf(LeadActionRequest request) {
        return request == null ? null : request.note();
    }
}
