package com.shelflife.controller;

import com.shelflife.dto.UserResponse;
import com.shelflife.dto.UserUpdateRequest;
import com.shelflife.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users", description = "User profile management.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    /**
     * Gets the currently authenticated user's profile.
     *
     * @param principal authenticated principal
     * @return profile details
     */
    @Operation(summary = "Get my profile", description = "Returns the authenticated user's profile including book count.")
    @ApiResponse(responseCode = "200", description = "User profile")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
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
    @Operation(summary = "Update my profile", description = "Updates email and/or display name for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Updated user profile")
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "Email already taken", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
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
    @Operation(summary = "Delete my account", description = "Permanently deletes the authenticated user's account, all saved books, and all reading tests.")
    @ApiResponse(responseCode = "204", description = "Account deleted")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(Principal principal) {
        userService.deleteAccount(principal.getName());
    }
}
