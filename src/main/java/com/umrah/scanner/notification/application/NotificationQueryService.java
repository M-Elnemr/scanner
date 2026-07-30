package com.umrah.scanner.notification.application;

import com.umrah.scanner.common.exception.ForbiddenException;
import com.umrah.scanner.common.exception.NotFoundException;
import com.umrah.scanner.notification.domain.Notification;
import com.umrah.scanner.notification.infrastructure.NotificationRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    public NotificationQueryService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public Page<Notification> listForUser(UUID userId, Pageable pageable) {
        return notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(userId);
    }

    @Transactional
    public Notification markRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> NotFoundException.of("Notification", notificationId));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ForbiddenException("Notification does not belong to this user");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
        }
        return notification;
    }
}
