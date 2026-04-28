package com.shelflife.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Validates production-sensitive configuration at startup.
 *
 * <p>Outside the local profile, this validator enforces that Firebase auth remains enabled,
 * required Firebase settings are present, and CORS origins are explicitly configured with
 * non-localhost values.
 */
@Slf4j
@Component
public class ProductionConfigurationValidator {

    private final Environment environment;
    private final boolean firebaseAuthEnabled;
    private final String firebaseProjectId;
    private final String firebaseServiceAccountPath;
    private final List<String> allowedOrigins;

    /**
     * Creates a validator for runtime configuration safeguards.
     *
     * @param environment active Spring environment
     * @param firebaseAuthEnabled whether Firebase authentication is enabled
     * @param firebaseProjectId configured Firebase project id
     * @param firebaseServiceAccountPath configured Firebase service-account path
     * @param allowedOrigins configured CORS allowed origins
     */
    public ProductionConfigurationValidator(
            Environment environment,
            @Value("${firebase.auth.enabled:false}") boolean firebaseAuthEnabled,
            @Value("${firebase.auth.project-id:}") String firebaseProjectId,
            @Value("${firebase.auth.service-account-path:}") String firebaseServiceAccountPath,
            @Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
        this.environment = environment;
        this.firebaseAuthEnabled = firebaseAuthEnabled;
        this.firebaseProjectId = firebaseProjectId;
        this.firebaseServiceAccountPath = firebaseServiceAccountPath;
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Performs fail-fast validation for non-local deployments.
     */
    @PostConstruct
    public void validate() {
        if (isStrictLocalProfile()) {
            log.debug("Skipping production configuration validation under local profile");
            return;
        }

        if (!firebaseAuthEnabled) {
            throw new IllegalStateException("firebase.auth.enabled must be true outside local profile");
        }

        if (firebaseProjectId == null || firebaseProjectId.isBlank()) {
            throw new IllegalStateException("firebase.auth.project-id must be configured outside local profile");
        }

        if (firebaseServiceAccountPath == null || firebaseServiceAccountPath.isBlank()) {
            throw new IllegalStateException(
                    "firebase.auth.service-account-path must be configured outside local profile"
            );
        }

        List<String> normalizedOrigins = normalizeOrigins(allowedOrigins);
        if (normalizedOrigins.isEmpty()) {
            throw new IllegalStateException("app.cors.allowed-origins must contain at least one origin");
        }

        if (normalizedOrigins.stream().anyMatch("*"::equals)) {
            throw new IllegalStateException("app.cors.allowed-origins must not contain wildcard '*'");
        }

        if (normalizedOrigins.stream().anyMatch(this::isLocalhostOrigin)) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins must not contain localhost origins outside local profile"
            );
        }
    }

    private boolean isStrictLocalProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean containsLocal = Arrays.stream(activeProfiles)
                .anyMatch("local"::equalsIgnoreCase);

        if (containsLocal && activeProfiles.length > 1) {
            throw new IllegalStateException(
                    "The local profile must not be combined with non-local profiles"
            );
        }

        return containsLocal;
    }

    private List<String> normalizeOrigins(List<String> origins) {
        if (origins == null) {
            return List.of();
        }

        return origins.stream()
                .map(origin -> origin == null ? "" : origin.trim())
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean isLocalhostOrigin(String origin) {
        String lower = origin.toLowerCase(Locale.ROOT);
        return lower.contains("://localhost")
                || lower.contains("://127.0.0.1")
                || lower.contains("://[::1]");
    }
}
