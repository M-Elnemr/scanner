package com.umrah.scanner.notification.infrastructure;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.umrah.scanner.notification.application.NotificationDispatcher;
import com.umrah.scanner.notification.domain.DeviceToken;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Writes the in-app notification row exactly as {@link PersistingNotificationDispatcher} always
 * has, then additionally pushes the same notification to every device the recipient has
 * registered. A missing/misconfigured {@link FirebaseMessaging} bean (see
 * {@link com.umrah.scanner.config.FirebaseConfig}) degrades this to the persistence-only
 * behavior, so local/dev environments work unchanged without real Firebase credentials.
 *
 * <p>The {@code data} map may carry a reserved {@code "imageUrl"} key (an absolute HTTPS URL) to
 * feature an image in the push notification; every other entry becomes part of the FCM data
 * payload for the client to deep-link on, same as it does in the persisted notification row.
 */
@Primary
@Component
public class FcmNotificationDispatcher implements NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(FcmNotificationDispatcher.class);
    private static final String IMAGE_URL_KEY = "imageUrl";

    private final PersistingNotificationDispatcher persistingDispatcher;
    private final DeviceTokenRepository deviceTokenRepository;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    public FcmNotificationDispatcher(
            PersistingNotificationDispatcher persistingDispatcher,
            DeviceTokenRepository deviceTokenRepository,
            ObjectProvider<FirebaseMessaging> firebaseMessagingProvider) {
        this.persistingDispatcher = persistingDispatcher;
        this.deviceTokenRepository = deviceTokenRepository;
        this.firebaseMessagingProvider = firebaseMessagingProvider;
    }

    @Override
    public void dispatch(UUID recipientUserId, String type, String title, String body, Map<String, Object> data) {
        persistingDispatcher.dispatch(recipientUserId, type, title, body, data);

        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findAllByUserId(recipientUserId);
        if (tokens.isEmpty()) {
            return;
        }

        try {
            push(firebaseMessaging, tokens, title, body, data);
        } catch (Exception e) {
            log.warn("Failed to push FCM notification for user={} type={}", recipientUserId, type, e);
        }
    }

    // MulticastMessage.Builder#addToken is deprecated in favor of #addFid, which targets Firebase
    // Installation IDs rather than FCM registration tokens. This app's DeviceToken/register-token
    // contract is token-based (what the client SDK's getToken() returns), so token targeting is
    // intentional here, not a stale API call.
    @SuppressWarnings("deprecation")
    private void push(FirebaseMessaging firebaseMessaging, List<DeviceToken> tokens,
            String title, String body, Map<String, Object> data) throws FirebaseMessagingException {
        String imageUrl = imageUrl(data);

        Notification.Builder notification = Notification.builder().setTitle(title).setBody(body);
        AndroidNotification.Builder androidNotification = AndroidNotification.builder();
        if (imageUrl != null) {
            notification.setImage(imageUrl);
            androidNotification.setImage(imageUrl);
        }

        MulticastMessage.Builder message = MulticastMessage.builder()
                .setNotification(notification.build())
                // Ensures the tray shows the title/body/image even when the app is backgrounded or
                // killed, instead of relying on client code to build a notification from data-only payloads.
                .setAndroidConfig(AndroidConfig.builder().setNotification(androidNotification.build()).build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder().setSound("default").build())
                        .build());
        tokens.forEach(deviceToken -> message.addToken(deviceToken.getFcmToken()));

        if (data != null) {
            data.forEach((key, value) -> {
                if (value != null) {
                    message.putData(key, value.toString());
                }
            });
        }

        BatchResponse response = firebaseMessaging.sendEachForMulticast(message.build());
        pruneStaleTokens(tokens, response);
    }

    private String imageUrl(Map<String, Object> data) {
        Object imageUrl = data == null ? null : data.get(IMAGE_URL_KEY);
        return imageUrl instanceof String url && !url.isBlank() ? url : null;
    }

    private void pruneStaleTokens(List<DeviceToken> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) {
                continue;
            }
            MessagingErrorCode errorCode = sendResponse.getException() == null
                    ? null : sendResponse.getException().getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                String staleToken = tokens.get(i).getFcmToken();
                deviceTokenRepository.deleteByFcmToken(staleToken);
                log.info("Pruned stale FCM token for user={}", tokens.get(i).getUser().getId());
            }
        }
    }
}
