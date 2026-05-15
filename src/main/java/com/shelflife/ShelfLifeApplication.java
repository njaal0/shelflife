package com.shelflife;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the ShelfLife Spring Boot application.
 *
 * <p>API documentation is available at {@code /swagger-ui.html} and the raw OpenAPI spec
 * at {@code /v3/api-docs}.
 *
 * <p>All {@code /api/**} endpoints (except {@code GET /api/books/search}) require a Firebase
 * ID token supplied as a Bearer token in the {@code Authorization} header. Obtain a token by
 * signing in via the Firebase client SDK and calling {@code user.getIdToken()}; refresh it
 * automatically using the Firebase SDK's token-refresh mechanism before it expires (1 hour).
 * The backend has no {@code /login} endpoint — authentication is fully delegated to Firebase.
 */
@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "ShelfLife API",
                version = "1.0",
                description = "Personal bookshelf and reading planner API. "
                        + "Most endpoints require a Firebase ID token as a Bearer token in the Authorization header. "
                        + "The search endpoint (GET /api/books/search) is public."
        ),
        servers = @Server(url = "/", description = "Default server")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "Firebase ID Token",
        description = "Provide a Firebase ID Token obtained from the Firebase client SDK."
)
public class ShelfLifeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShelfLifeApplication.class, args);
    }
}
