package com.umrah.scanner.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.FileInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the Firebase Admin SDK from the service-account file at
 * {@code app.firebase.credentials-path}, if one is configured. Left blank (the default), no
 * {@link FirebaseMessaging} bean is published and push notifications are silently skipped —
 * {@link com.umrah.scanner.notification.infrastructure.FcmNotificationDispatcher} tolerates its
 * absence, so local/dev environments never need real Firebase credentials.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public FirebaseMessaging firebaseMessaging(@Value("${app.firebase.credentials-path}") String credentialsPath) {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("app.firebase.credentials-path not set; FCM push notifications are disabled");
            return null;
        }
        try (FileInputStream credentialsStream = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            log.info("Firebase Admin SDK initialized from {}", credentialsPath);
            return FirebaseMessaging.getInstance(app);
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK from {}; FCM push notifications are disabled",
                    credentialsPath, e);
            return null;
        }
    }
}
