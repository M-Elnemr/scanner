package com.umrah.scanner.notification.infrastructure;

import com.umrah.scanner.notification.domain.Notification;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findAllByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(UUID recipientId);
}
