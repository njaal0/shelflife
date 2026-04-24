package com.shelflife.service;

import com.shelflife.dto.UserResponse;
import com.shelflife.dto.UserUpdateRequest;
import com.shelflife.exception.DuplicateEmailException;
import com.shelflife.exception.UserNotFoundException;
import com.shelflife.model.User;
import com.shelflife.repository.BookEntryRepository;
import com.shelflife.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookEntryRepository bookEntryRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void ensureUserExists_shouldCreateOnFirstAuth() {
        when(userRepository.findById("uid-1")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.ensureUserExists("uid-1", "u1@example.com", "User One");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("uid-1", saved.getId());
        assertEquals("u1@example.com", saved.getEmail());
        assertEquals("User One", saved.getDisplayName());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertNotNull(saved.getLastLoginAt());
    }

    @Test
    void ensureUserExists_shouldBeIdempotentAndUpdateLogin() {
        User existing = User.builder()
                .id("uid-1")
                .email("u1@example.com")
                .displayName("User One")
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userRepository.findById("uid-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.ensureUserExists("uid-1", "u1@example.com", "User One");

        verify(userRepository).save(existing);
        assertNotNull(existing.getUpdatedAt());
        assertNotNull(existing.getLastLoginAt());
    }

    @Test
    void updateProfile_shouldRejectDuplicateEmail() {
        User existing = User.builder().id("uid-1").email("old@example.com").build();
        User other = User.builder().id("uid-2").email("taken@example.com").build();

        when(userRepository.findById("uid-1")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmailNormalized("taken@example.com")).thenReturn(Optional.of(other));

        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("taken@example.com")
                .build();

        assertThrows(DuplicateEmailException.class, () -> userService.updateProfile("uid-1", request));
    }

    @Test
    void updateProfile_shouldNormalizeEmailToLowercase() {
        User existing = User.builder().id("uid-1").email("old@example.com").build();
        when(userRepository.findById("uid-1")).thenReturn(Optional.of(existing));
        when(userRepository.findByEmailNormalized("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("NEW@Example.com")
                .build();

        UserResponse response = userService.updateProfile("uid-1", request);

        assertEquals("new@example.com", response.getEmail());
    }

    @Test
    void deleteAccount_shouldDeleteBooksThenUser() {
        User existing = User.builder().id("uid-1").build();
        when(userRepository.findById("uid-1")).thenReturn(Optional.of(existing));

        userService.deleteAccount("uid-1");

        verify(bookEntryRepository).deleteByUserId("uid-1");
        verify(userRepository).delete(existing);
    }

    @Test
    void getProfile_shouldThrowWhenMissing() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getProfile("missing"));
    }

    @Test
    void getProfile_shouldReturnBookCount() {
        User existing = User.builder().id("uid-1").email("u1@example.com").displayName("User One").build();
        when(userRepository.findById("uid-1")).thenReturn(Optional.of(existing));
        when(bookEntryRepository.countByUserId("uid-1")).thenReturn(3L);

        UserResponse response = userService.getProfile("uid-1");

        assertEquals(3L, response.getTotalBooks());
        assertEquals("uid-1", response.getId());
    }
}
