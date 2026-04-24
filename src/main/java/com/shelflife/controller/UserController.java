package com.shelflife.controller;

import com.shelflife.dto.UserResponse;
import com.shelflife.dto.UserUpdateRequest;
import com.shelflife.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Exposes profile operations for the currently authenticated user.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Gets the currently authenticated user's profile.
     *
     * @param principal authenticated principal
     * @return profile details
     */
    @GetMapping
    public UserResponse getMyProfile(Principal principal) {
        return userService.getProfile(principal.getName());
    }

    /**
     * Updates mutable profile fields for the authenticated user.
     *
     * @param request update payload
     * @param principal authenticated principal
     * @return updated profile details
     */
    @PutMapping
    public UserResponse updateMyProfile(@Valid @RequestBody UserUpdateRequest request,
                                        Principal principal) {
        return userService.updateProfile(principal.getName(), request);
    }

    /**
     * Deletes the authenticated user's account and associated books.
     *
     * @param principal authenticated principal
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(Principal principal) {
        userService.deleteAccount(principal.getName());
    }
}
