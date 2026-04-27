package com.shelflife.filter;

import com.shelflife.service.UserService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class FirebaseAuthFilterTest {

    @Mock
    private UserService userService;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldAuthenticateAndAutoCreateUserInDevMode() throws Exception {
        FirebaseAuthFilter filter = new FirebaseAuthFilter(false, userService,
            new MockEnvironment().withProperty("spring.profiles.active", "local"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/shelves");
        request.addHeader("Authorization", "Bearer dev-user-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals("dev-user-1", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(userService).ensureUserExists("dev-user-1", null, null);
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorizedOnTokenFailure() throws Exception {
        FirebaseAuthFilter filter = new FirebaseAuthFilter(false, userService,
                new MockEnvironment().withProperty("spring.profiles.active", "local")) {
            @Override
            protected ResolvedPrincipal validateAndResolvePrincipal(String jwt) throws Exception {
                throw new IllegalArgumentException("Invalid token");
            }
        };

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/shelves");
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorizedWhenUserSyncFails() throws Exception {
        doThrow(new RuntimeException("db down")).when(userService)
                .ensureUserExists("dev-user-2", null, null);

        FirebaseAuthFilter filter = new FirebaseAuthFilter(false, userService,
                new MockEnvironment().withProperty("spring.profiles.active", "local"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/shelves");
        request.addHeader("Authorization", "Bearer dev-user-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorizedWhenAuthorizationHeaderMissing() throws Exception {
        FirebaseAuthFilter filter = new FirebaseAuthFilter(false, userService,
                new MockEnvironment().withProperty("spring.profiles.active", "local"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/shelves");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertEquals("Missing Authorization header", response.getContentAsString());
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorizedWhenAuthorizationHeaderMalformed() throws Exception {
        FirebaseAuthFilter filter = new FirebaseAuthFilter(false, userService,
                new MockEnvironment().withProperty("spring.profiles.active", "local"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/shelves");
        request.addHeader("Authorization", "Token dev-user-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertEquals("Invalid Authorization header", response.getContentAsString());
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilterInternal_shouldReturnUnauthorizedWhenBearerTokenMissing() throws Exception {
        FirebaseAuthFilter filter = new FirebaseAuthFilter(false, userService,
                new MockEnvironment().withProperty("spring.profiles.active", "local"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/shelves");
        request.addHeader("Authorization", "Bearer   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertEquals("Missing bearer token", response.getContentAsString());
        verifyNoInteractions(filterChain);
    }

    @Test
    void doFilterInternal_shouldAllowAnonymousSearchEndpoint() throws Exception {
        FirebaseAuthFilter filter = new FirebaseAuthFilter(false, userService,
                new MockEnvironment().withProperty("spring.profiles.active", "local"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/books/search");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userService);
    }

    @Test
    void constructor_shouldRejectDisabledFirebaseOutsideLocalProfile() {
        IllegalStateException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> new FirebaseAuthFilter(false, userService,
                        new MockEnvironment().withProperty("spring.profiles.active", "prod"))
        );

        assertEquals("firebase.auth.enabled=false is only allowed under the local profile", ex.getMessage());
    }
}
