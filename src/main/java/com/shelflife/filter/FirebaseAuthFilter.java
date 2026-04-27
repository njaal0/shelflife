package com.shelflife.filter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.shelflife.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PUBLIC_SEARCH_PATH = "/api/books/search";

    private final boolean firebaseAuthEnabled;
    private final UserService userService;

    public FirebaseAuthFilter(@Value("${firebase.auth.enabled:false}") boolean firebaseAuthEnabled,
                              UserService userService,
                              Environment environment) {
        this.firebaseAuthEnabled = firebaseAuthEnabled;
        this.userService = userService;

        if (!firebaseAuthEnabled && !isLocalProfile(environment)) {
            throw new IllegalStateException(
                    "firebase.auth.enabled=false is only allowed under the local profile"
            );
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!requiresAuthentication(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || authHeader.isBlank()) {
            rejectUnauthorized(response, "Missing Authorization header");
            return;
        }

        if (!authHeader.startsWith(BEARER_PREFIX)) {
            rejectUnauthorized(response, "Invalid Authorization header");
            return;
        }

        String jwt = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (jwt.isEmpty()) {
            rejectUnauthorized(response, "Missing bearer token");
            return;
        }

        try {
            ResolvedPrincipal principal = validateAndResolvePrincipal(jwt);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal.userId(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            userService.ensureUserExists(principal.userId(), principal.email(), principal.displayName());
        } catch (Exception e) {
            log.warn("Authentication rejected: {}", e.getMessage());
            rejectUnauthorized(response, "Invalid token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    protected ResolvedPrincipal validateAndResolvePrincipal(String jwt) throws Exception {
        if (firebaseAuthEnabled) {
            FirebaseToken token = FirebaseAuth.getInstance().verifyIdToken(jwt);
            return new ResolvedPrincipal(token.getUid(), token.getEmail(), token.getName());
        }
        return new ResolvedPrincipal(jwt, null, null);
    }

    private boolean isLocalProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch("local"::equalsIgnoreCase);
    }

    private boolean requiresAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/")) {
            return false;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        return !(
                "GET".equalsIgnoreCase(request.getMethod())
                        && PUBLIC_SEARCH_PATH.equals(path)
        );
    }

    private void rejectUnauthorized(HttpServletResponse response, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(message);
    }

    protected record ResolvedPrincipal(String userId, String email, String displayName) {
    }
}
