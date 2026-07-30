package com.umrah.scanner.audit.presentation;

import com.umrah.scanner.audit.application.AuditLogService;
import com.umrah.scanner.common.response.ApiResponse;
import com.umrah.scanner.common.response.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/v1/admin/audit-logs")
    public ApiResponse<PageResponse<AuditLogResponse>> search(
            @RequestParam String entityType, @RequestParam UUID entityId, Pageable pageable) {
        return ApiResponse.of(PageResponse.of(auditLogService.search(entityType, entityId, pageable), AuditLogResponse::from));
    }
}
