package com.shelflife.controller;

import com.shelflife.dto.BookSearchResult;
import com.shelflife.service.GoogleBooksService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class SearchController {

    private final GoogleBooksService googleBooksService;

    @GetMapping("/search")
    public List<BookSearchResult> searchBooks(@RequestParam("q") String query) {
        return googleBooksService.searchBooks(query);
    }
}
