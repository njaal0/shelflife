package com.shelflife.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class DefaultStartupConfigurationTest {

    @Test
    void startup_shouldFailFastWhenNoProfileAndCorsOriginsAreUnset() {
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder()
                .sources(com.shelflife.ShelfLifeApplication.class)
                .properties(
                        "spring.main.web-application-type=none",
                        "firebase.auth.enabled=true",
                        "app.users.email-index-initializer.enabled=false",
                        "app.cors.allowed-origins=",
                        "spring.autoconfigure.exclude="
                                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
                )
                .run()) {
            fail("Expected startup to fail when app.cors.allowed-origins is unset outside local profile");
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage();
            Throwable cause = ex.getCause();
            while (cause != null) {
                String causeMessage = cause.getMessage();
                if (causeMessage != null) {
                    message = message + "\n" + causeMessage;
                }
                cause = cause.getCause();
            }

            assertTrue(
                    message.contains("app.cors.allowed-origins must contain at least one origin"),
                    "Startup error should mention missing CORS origins"
            );
        }
    }
}
