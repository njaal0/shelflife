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
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
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
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid token");
                log.warn("Authentication rejected: {}", e.getMessage());
                return;
            }
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

    protected record ResolvedPrincipal(String userId, String email, String displayName) {
    }
}
