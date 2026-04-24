package com.shelflife.exception;

/**
 * Thrown when attempting to assign an email that already belongs to another user.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("Email already in use");
    }
}
