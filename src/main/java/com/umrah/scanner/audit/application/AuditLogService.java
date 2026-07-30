package com.umrah.scanner.audit.application;

import com.umrah.scanner.audit.domain.AuditLog;
import com.umrah.scanner.audit.infrastructure.AuditLogRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Before/after audit trail for sensitive state changes (approvals, lead transitions,
 * commission/cashback release). Runs in its own transaction (REQUIRES_NEW) so a logging
 * failure never rolls back the business change it is describing.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorUserId, String action, String entityType, UUID entityId, Object oldValue, Object newValue) {
        AuditLog entry = new AuditLog();
        entry.setActorUserId(actorUserId);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setOldValue(toJson(oldValue));
        entry.setNewValue(toJson(newValue));
        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(String entityType, UUID entityId, Pageable pageable) {
        return auditLogRepository.findAllByEntityTypeAndEntityId(entityType, entityId, pageable);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            log.warn("Failed to serialize audit payload for {}", value.getClass(), e);
            return null;
        }
    }
}
