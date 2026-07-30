package com.umrah.scanner.audit.infrastructure;

import com.umrah.scanner.audit.domain.AuditLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findAllByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);
}
