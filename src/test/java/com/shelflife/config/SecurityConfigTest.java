package com.shelflife.config;

import com.shelflife.filter.FirebaseAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void corsConfigurationSource_shouldNormalizeAllowedOrigins() {
        SecurityConfig config = new SecurityConfig(mock(FirebaseAuthFilter.class));

        CorsConfigurationSource source =
                config.corsConfigurationSource(List.of(" https://app.shelflife.com ", "http://localhost:3000"));

        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest());
        assertNotNull(cors);
        assertEquals(List.of("https://app.shelflife.com", "http://localhost:3000"), cors.getAllowedOrigins());
    }

    @Test
    void corsConfigurationSource_shouldRejectWildcardOrigin() {
        SecurityConfig config = new SecurityConfig(mock(FirebaseAuthFilter.class));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> config.corsConfigurationSource(List.of("*")));

        assertEquals("Wildcard CORS origin '*' is not allowed when credentials are enabled", ex.getMessage());
    }

    @Test
    void corsConfigurationSource_shouldRejectBlankOriginList() {
        SecurityConfig config = new SecurityConfig(mock(FirebaseAuthFilter.class));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> config.corsConfigurationSource(List.of(" ", "")));

        assertEquals("app.cors.allowed-origins must contain at least one origin", ex.getMessage());
    }
}
