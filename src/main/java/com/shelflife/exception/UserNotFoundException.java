package com.shelflife.exception;

/**
 * Thrown when an authenticated user does not exist in local persistence.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
    }
}
