package com.umrah.scanner.notification.presentation;

import com.umrah.scanner.notification.domain.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(UUID id, String type, String title, String body, Instant readAt, Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getType(), notification.getTitle(),
                notification.getBody(), notification.getReadAt(), notification.getCreatedAt());
    }
}
