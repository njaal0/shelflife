package com.shelflife.service;

import com.shelflife.dto.UserResponse;
import com.shelflife.dto.UserUpdateRequest;
import com.shelflife.exception.DuplicateEmailException;
import com.shelflife.exception.UserNotFoundException;
import com.shelflife.model.User;
import com.shelflife.repository.BookEntryRepository;
import com.shelflife.repository.ReadingTestRepository;
import com.shelflife.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Handles local user lifecycle operations for authenticated principals.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BookEntryRepository bookEntryRepository;
    private final ReadingTestRepository readingTestRepository;

    /**
     * Ensures a local user record exists for the authenticated principal.
     *
     * @param userId the authenticated principal identifier
     * @param email email resolved from the auth provider, if available
     * @param displayName display name resolved from the auth provider, if available
     */
    public void ensureUserExists(String userId, String email, String displayName) {
        Optional<User> existing = userRepository.findById(userId);
        LocalDateTime now = LocalDateTime.now();
        String normalizedEmail = normalizeEmail(email);

        if (existing.isPresent()) {
            User user = existing.get();
            if (isBlank(user.getEmail()) && !isBlank(normalizedEmail)) {
                user.setEmail(normalizedEmail);
            }
            if (isBlank(user.getEmailNormalized()) && !isBlank(user.getEmail())) {
                user.setEmailNormalized(normalizeEmail(user.getEmail()));
            }
            if (isBlank(user.getDisplayName()) && !isBlank(displayName)) {
                user.setDisplayName(displayName.trim());
            }
            user.setLastLoginAt(now);
            user.setUpdatedAt(now);
            userRepository.save(user);
            return;
        }

        User created = User.builder()
                .id(userId)
            .email(normalizedEmail)
            .emailNormalized(normalizedEmail)
                .displayName(normalize(displayName))
                .createdAt(now)
                .updatedAt(now)
                .lastLoginAt(now)
                .build();
        userRepository.save(created);
    }

    /**
     * Returns the authenticated user's profile.
     *
     * @param userId authenticated principal identifier
     * @return profile response payload
     */
    public UserResponse getProfile(String userId) {
        User user = getUserEntity(userId);
        return toResponse(user);
    }

    /**
     * Updates mutable profile fields for the authenticated user.
     *
     * @param userId authenticated principal identifier
     * @param request partial update request
     * @return updated profile response payload
     */
    public UserResponse updateProfile(String userId, UserUpdateRequest request) {
        User user = getUserEntity(userId);

        if (request.getEmail() != null) {
            String normalizedEmail = normalizeEmail(request.getEmail());
            if (!isBlank(normalizedEmail) && !normalizedEmail.equalsIgnoreCase(user.getEmail())) {
                userRepository.findByEmailNormalized(normalizedEmail)
                        .filter(existing -> !existing.getId().equals(userId))
                        .ifPresent(existing -> {
                            throw new DuplicateEmailException();
                        });
                user.setEmail(normalizedEmail);
                user.setEmailNormalized(normalizedEmail);
            }
            if (isBlank(normalizedEmail)) {
                user.setEmail(null);
                user.setEmailNormalized(null);
            }
        }

        if (request.getDisplayName() != null) {
            user.setDisplayName(normalize(request.getDisplayName()));
        }

        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    /**
     * Deletes the authenticated user and all associated books.
     *
     * @param userId authenticated principal identifier
     */
    public void deleteAccount(String userId) {
        User user = getUserEntity(userId);
        readingTestRepository.deleteByUserId(userId);
        bookEntryRepository.deleteByUserId(userId);
        userRepository.delete(user);
    }

    /**
     * Verifies a user exists locally.
     *
     * @param userId authenticated principal identifier
     */
    public void assertUserExists(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
    }

    private User getUserEntity(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .totalBooks(bookEntryRepository.countByUserId(user.getId()))
                .build();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeEmail(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
