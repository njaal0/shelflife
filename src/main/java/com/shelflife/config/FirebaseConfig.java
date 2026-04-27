package com.shelflife.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Initializes the Firebase Admin SDK singleton ({@link FirebaseApp}) on application startup.
 *
 * <p>This configuration is only active when {@code firebase.auth.enabled=true}. It reads the
 * Firebase service-account JSON from the path specified by
 * {@code firebase.auth.service-account-path}, builds {@link GoogleCredentials}, and calls
 * {@link FirebaseApp#initializeApp(FirebaseOptions)} exactly once (idempotent against duplicate
 * calls, e.g. in tests).
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "firebase.auth.enabled", havingValue = "true")
public class FirebaseConfig {

    private final Resource serviceAccountResource;

    /**
     * Constructs {@code FirebaseConfig} with the service-account resource.
     *
     * @param serviceAccountResource Spring {@link Resource} resolved from
     *                               {@code firebase.auth.service-account-path}
     */
    public FirebaseConfig(
            @Value("${firebase.auth.service-account-path}") Resource serviceAccountResource) {
        this.serviceAccountResource = serviceAccountResource;
    }

    /**
     * Initializes the default {@link FirebaseApp} from the service-account JSON.
     *
     * <p>If a default {@code FirebaseApp} is already registered (e.g. during test reruns),
     * initialization is skipped to avoid an {@link IllegalStateException}.
     *
     * @throws IOException if the service-account file cannot be read
     */
    @PostConstruct
    public void initializeFirebase() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.debug("FirebaseApp already initialized — skipping");
            return;
        }

        try (InputStream stream = serviceAccountResource.getInputStream()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("FirebaseApp initialized successfully");
        }
    }
}
