package com.shelflife.controller;

import com.shelflife.dto.BookRequest;
import com.shelflife.model.BookEntry;
import com.shelflife.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping("/shelves")
    public List<BookEntry> getAllShelves(Authentication authentication) {
        return bookService.getBooksForUser(getUserId(authentication));
    }

    @GetMapping("/shelves/{shelf}")
    public List<BookEntry> getShelf(@PathVariable String shelf, Authentication authentication) {
        return bookService.getBooksForUserByShelf(getUserId(authentication), shelf);
    }

    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookEntry createBook(@RequestBody BookRequest request, Authentication authentication) {
        return bookService.createBook(getUserId(authentication), request);
    }

    @PutMapping("/books/{id}")
    public BookEntry updateBook(@PathVariable String id,
                                @RequestBody BookRequest request,
                                Authentication authentication) {
        return bookService.updateBook(id, getUserId(authentication), request);
    }

    @DeleteMapping("/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable String id, Authentication authentication) {
        bookService.deleteBook(id, getUserId(authentication));
    }

    @GetMapping("/books/{id}")
    public BookEntry getBook(@PathVariable String id, Authentication authentication) {
        return bookService.getBookForUser(id, getUserId(authentication));
    }

    private String getUserId(Authentication authentication) {
        return authentication.getName();
    }
}
