package com.shelflife.controller;

import com.shelflife.dto.BookSearchResult;
import com.shelflife.service.GoogleBooksService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Exposes public book-search endpoints backed by Google Books.
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Public Google Books search. No authentication required.")
public class SearchController {

    private static final Pattern ISBN_10_PATTERN = Pattern.compile("^[0-9]{9}[0-9Xx]$");
    private static final Pattern ISBN_13_PATTERN = Pattern.compile("^[0-9]{13}$");

    private final GoogleBooksService googleBooksService;

    /**
     * Searches books by metadata or ISBN.
     *
     * <p>At least one parameter is required. When {@code isbn} is provided, this endpoint
     * runs in ISBN-only mode and ignores title/author/publisher/year parameters.
     *
     * @param title optional book title filter
     * @param author optional author filter
     * @param publisher optional publisher filter
     * @param year optional publication year filter
     * @param isbn optional ISBN-10/ISBN-13 filter (hyphens/spaces allowed)
     * @return list of matching books from Google Books
     */
    @Operation(
            summary = "Search books",
            description = "Searches Google Books by title, author, publisher, year, or ISBN. "
                    + "At least one parameter is required. When isbn is provided, ISBN-only search mode is used. "
                    + "This endpoint is public — no Authorization header is required."
    )
    @ApiResponse(responseCode = "200", description = "List of matching books")
    @ApiResponse(responseCode = "400", description = "No search parameters supplied or invalid ISBN", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "503", description = "Google Books API is temporarily unavailable", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @GetMapping("/search")
    public List<BookSearchResult> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String isbn) {

        String normalizedIsbn = normalizeIsbn(isbn);

        boolean hasQuery = (title != null && !title.isBlank())
                || (author != null && !author.isBlank())
                || (publisher != null && !publisher.isBlank())
                || (year != null && !year.isBlank())
                || normalizedIsbn != null;

        if (!hasQuery) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one search parameter is required: title, author, publisher, year, isbn");
        }

        if (normalizedIsbn != null) {
            return googleBooksService.searchBooks(null, null, null, null, normalizedIsbn);
        }

        return googleBooksService.searchBooks(title, author, publisher, year, null);
    }

    private String normalizeIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return null;
        }

        String cleaned = isbn.replaceAll("[\\s-]", "").toUpperCase();
        if (ISBN_10_PATTERN.matcher(cleaned).matches() || ISBN_13_PATTERN.matcher(cleaned).matches()) {
            return cleaned;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid isbn format. Use ISBN-10 or ISBN-13.");
    }
}
