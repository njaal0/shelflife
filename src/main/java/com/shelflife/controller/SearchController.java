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

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class SearchController {

    private final GoogleBooksService googleBooksService;

    @GetMapping("/search")
    public List<BookSearchResult> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String publisher,
            @RequestParam(required = false) String year) {

        boolean hasQuery = (title != null && !title.isBlank())
                || (author != null && !author.isBlank())
                || (publisher != null && !publisher.isBlank())
                || (year != null && !year.isBlank());

        if (!hasQuery) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one search parameter is required: title, author, publisher, year");
        }

        return googleBooksService.searchBooks(title, author, publisher, year);
    }
}
