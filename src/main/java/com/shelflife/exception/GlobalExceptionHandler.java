package com.shelflife.exception;

import com.shelflife.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides consistent API error payloads across controllers with domain-specific error codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(base(ErrorCodes.USER_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(base(ErrorCodes.DUPLICATE_EMAIL, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (first, second) -> first
                ));

        ErrorResponse response = base(ErrorCodes.VALIDATION_ERROR, "Validation failed");
        response.setValidationErrors(fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() == null ? "Request failed" : ex.getReason();
        String errorCode = mapToErrorCode(message, ex.getStatusCode());
        ErrorResponse response = base(errorCode, message);
        return ResponseEntity.status(ex.getStatusCode()).body(response);
    }

    /**
     * Maps ResponseStatusException messages to domain-specific error codes for deterministic frontend handling.
     *
     * IMPORTANT: This method relies on message content patterns. When adding new error throws or modifying
     * error messages, ensure the patterns here are updated correspondingly:
     *
     * Message patterns used for mapping:
     * - 400 "Page..." → INVALID_PAGINATION_PARAMS (e.g., "Page index must be >= 0")
     * - 404 "Book..." → BOOK_NOT_FOUND
     * - 404 "Reading test..." → READING_TEST_NOT_FOUND
     * - 404 (default) → USER_NOT_FOUND
     * - 409 "already on shelf" → BOOK_ALREADY_ON_SHELF
     * - 409 "active" → ACTIVE_READING_TEST_EXISTS
     * - 409 "test state" → INVALID_READING_TEST_STATE
     * - 400 "shelf" → INVALID_SHELF
     * - 400 "rating" → INVALID_RATING
     * - 400 "isbn" → INVALID_ISBN
     * - 400 "date range" → INVALID_DATE_RANGE
     * - 400 "page count" → READING_TEST_MISSING_PAGE_COUNT
     * - 400 "metadata" → READING_TEST_MISSING_METADATA
     * - 502/503 → GOOGLE_BOOKS_UNAVAILABLE
     *
     * Future refactoring should consider custom exception classes (e.g., PaginationException, ValidationException)
     * to decouple error codes from message patterns.
     */
    private String mapToErrorCode(String message, org.springframework.http.HttpStatusCode statusCode) {
        if (message == null) {
            return "REQUEST_ERROR";
        }

        // Pagination validation errors: "Page index must be >= 0, got -1" → INVALID_PAGINATION_PARAMS
        if (message.contains("Page")) {
            return ErrorCodes.INVALID_PAGINATION_PARAMS;
        }

        // Not found errors
        if (statusCode.value() == 404) {
            if (message.contains("Book")) {
                return ErrorCodes.BOOK_NOT_FOUND;
            }
            if (message.contains("Reading test")) {
                return ErrorCodes.READING_TEST_NOT_FOUND;
            }
            return ErrorCodes.USER_NOT_FOUND;
        }

        // Conflict errors
        if (statusCode.value() == 409) {
            if (message.contains("already on shelf")) {
                return ErrorCodes.BOOK_ALREADY_ON_SHELF;
            }
            if (message.contains("active")) {
                return ErrorCodes.ACTIVE_READING_TEST_EXISTS;
            }
            if (message.contains("test state")) {
                return ErrorCodes.INVALID_READING_TEST_STATE;
            }
            return "CONFLICT";
        }

        // Invalid state/validation for 400
        if (statusCode.value() == 400) {
            if (message.contains("shelf")) {
                return ErrorCodes.INVALID_SHELF;
            }
            if (message.contains("rating")) {
                return ErrorCodes.INVALID_RATING;
            }
            if (message.contains("isbn")) {
                return ErrorCodes.INVALID_ISBN;
            }
            if (message.contains("date range")) {
                return ErrorCodes.INVALID_DATE_RANGE;
            }
            if (message.contains("page count")) {
                return ErrorCodes.READING_TEST_MISSING_PAGE_COUNT;
            }
            if (message.contains("metadata")) {
                return ErrorCodes.READING_TEST_MISSING_METADATA;
            }
            return ErrorCodes.VALIDATION_ERROR;
        }

        // Upstream/service errors
        if (statusCode.value() == 502 || statusCode.value() == 503) {
            return ErrorCodes.GOOGLE_BOOKS_UNAVAILABLE;
        }

        return "REQUEST_ERROR";
    }

    private ErrorResponse base(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
