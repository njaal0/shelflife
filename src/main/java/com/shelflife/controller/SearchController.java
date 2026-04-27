package com.shelflife.controller;

import com.shelflife.dto.BookSearchResult;
import com.shelflife.service.GoogleBooksService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class SearchController {

    private static final Pattern ISBN_10_PATTERN = Pattern.compile("^[0-9]{9}[0-9Xx]$");
    private static final Pattern ISBN_13_PATTERN = Pattern.compile("^[0-9]{13}$");

    private final GoogleBooksService googleBooksService;

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
