package com.umrah.scanner.notification.application;

import java.util.Map;
import java.util.UUID;

/**
 * Port for notifying a user of something that happened elsewhere in the system.
 * Implemented today by a persistence-only adapter; a Firebase Cloud Messaging adapter
 * will implement the same port once push delivery is wired in, with no change to callers.
 */
public interface NotificationDispatcher {

    void dispatch(UUID recipientUserId, String type, String title, String body, Map<String, Object> data);
}
