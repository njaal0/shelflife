package com.shelflife.exception;

/**
 * Domain-specific error codes returned in API error responses.
 * These codes enable frontend to make deterministic decisions based on error type.
 */
public class ErrorCodes {

    // Validation errors
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INVALID_PAGINATION_PARAMS = "INVALID_PAGINATION_PARAMS";

    // Not found errors
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String BOOK_NOT_FOUND = "BOOK_NOT_FOUND";
    public static final String READING_TEST_NOT_FOUND = "READING_TEST_NOT_FOUND";

    // Conflict errors
    public static final String DUPLICATE_EMAIL = "DUPLICATE_EMAIL";
    public static final String BOOK_ALREADY_ON_SHELF = "BOOK_ALREADY_ON_SHELF";
    public static final String ACTIVE_READING_TEST_EXISTS = "ACTIVE_READING_TEST_EXISTS";

    // Invalid state errors
    public static final String INVALID_SHELF = "INVALID_SHELF";
    public static final String INVALID_RATING = "INVALID_RATING";
    public static final String INVALID_ISBN = "INVALID_ISBN";
    public static final String INVALID_READING_TEST_STATE = "INVALID_READING_TEST_STATE";
    public static final String READING_TEST_MISSING_METADATA = "READING_TEST_MISSING_METADATA";
    public static final String READING_TEST_MISSING_PAGE_COUNT = "READING_TEST_MISSING_PAGE_COUNT";
    public static final String READING_TEST_NO_BOOK_ESTIMATES = "READING_TEST_NO_BOOK_ESTIMATES";
    public static final String INVALID_DATE_RANGE = "INVALID_DATE_RANGE";

    // Upstream/service errors
    public static final String GOOGLE_BOOKS_UNAVAILABLE = "GOOGLE_BOOKS_UNAVAILABLE";
}
