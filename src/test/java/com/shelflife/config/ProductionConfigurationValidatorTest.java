package com.shelflife.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionConfigurationValidatorTest {

    @Test
    void validate_shouldAllowLocalProfileWithRelaxedSettings() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "local"),
                false,
                "",
                "",
                List.of("http://localhost:3000")
        );

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_shouldRejectDisabledFirebaseOutsideLocalProfile() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                false,
                "shelflife-prod",
                "file:/secure/firebase.json",
                List.of("https://app.shelflife.com")
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertEquals("firebase.auth.enabled must be true outside local profile", ex.getMessage());
    }

    @Test
    void validate_shouldRejectMissingFirebaseProjectIdOutsideLocalProfile() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                true,
                " ",
                "file:/secure/firebase.json",
                List.of("https://app.shelflife.com")
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertEquals("firebase.auth.project-id must be configured outside local profile", ex.getMessage());
    }

    @Test
    void validate_shouldRejectMissingFirebaseServiceAccountPathOutsideLocalProfile() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                true,
                "shelflife-prod",
                " ",
                List.of("https://app.shelflife.com")
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertEquals("firebase.auth.service-account-path must be configured outside local profile", ex.getMessage());
    }

    @Test
    void validate_shouldRejectWildcardCorsOriginOutsideLocalProfile() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                true,
                "shelflife-prod",
                "file:/secure/firebase.json",
                List.of("*")
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertEquals("app.cors.allowed-origins must not contain wildcard '*'", ex.getMessage());
    }

    @Test
    void validate_shouldRejectLocalhostCorsOriginOutsideLocalProfile() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                true,
                "shelflife-prod",
                "file:/secure/firebase.json",
                List.of("http://localhost:3000")
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertEquals(
                "app.cors.allowed-origins must not contain localhost origins outside local profile",
                ex.getMessage()
        );
    }

    @Test
    void validate_shouldAllowSecureProductionConfiguration() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "prod"),
                true,
                "shelflife-prod",
                "file:/secure/firebase.json",
                List.of("https://app.shelflife.com", "https://admin.shelflife.com")
        );

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void validate_shouldRejectMixedLocalAndNonLocalProfiles() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                new MockEnvironment().withProperty("spring.profiles.active", "local,prod"),
                true,
                "shelflife-prod",
                "file:/secure/firebase.json",
                List.of("https://app.shelflife.com")
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertEquals("The local profile must not be combined with non-local profiles", ex.getMessage());
    }

    @Test
    void validate_shouldRejectMissingCorsOriginsWhenNoProfileIsActive() {
        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(
                new MockEnvironment(),
                true,
                "shelflife-prod",
                "file:/secure/firebase.json",
                List.of(" ")
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
        assertEquals("app.cors.allowed-origins must contain at least one origin", ex.getMessage());
    }
}
