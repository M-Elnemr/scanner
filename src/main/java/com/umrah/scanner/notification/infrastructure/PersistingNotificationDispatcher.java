package com.umrah.scanner.notification.infrastructure;

import com.umrah.scanner.notification.application.NotificationDispatcher;
import com.umrah.scanner.notification.domain.Notification;
import com.umrah.scanner.user.infrastructure.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes the in-app notification row so the inbox and unread count work today.
 * Push delivery via Firebase Cloud Messaging is not wired yet — swap this adapter
 * (or extend it) once the FCM client is added; {@link NotificationDispatcher} callers
 * do not change.
 */
@Component
public class PersistingNotificationDispatcher implements NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PersistingNotificationDispatcher.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public PersistingNotificationDispatcher(
            NotificationRepository notificationRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void dispatch(UUID recipientUserId, String type, String title, String body, Map<String, Object> data) {
        Notification notification = new Notification();
        notification.setRecipient(userRepository.getReferenceById(recipientUserId));
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setData(toJson(data));
        notificationRepository.save(notification);

        log.info("Notification queued for user={} type={} (FCM push delivery not yet wired)", recipientUserId, type);
    }

    private String toJson(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JacksonException e) {
            log.warn("Failed to serialize notification data payload", e);
            return null;
        }
    }
}
