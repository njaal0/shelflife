package com.shelflife.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Utility for validating pagination parameters to ensure deterministic 4xx errors.
 */
public class PaginationValidator {

    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Validates page and size parameters for pagination requests.
     *
     * @param page zero-based page index
     * @param size maximum results per page
     * @throws ResponseStatusException with 400 status if parameters are invalid
     */
    public static void validatePageAndSize(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page index must be >= 0, got " + page);
        }
        if (size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page size must be > 0, got " + size);
        }
        if (size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page size must be <= " + MAX_PAGE_SIZE + ", got " + size);
        }
    }
}
