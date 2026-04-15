package com.shelflife.service;

import com.shelflife.dto.BookRequest;
import com.shelflife.model.BookEntry;
import com.shelflife.repository.BookEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookEntryRepository bookEntryRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void createBook_shouldCreateBookEntry() {
        BookRequest request = BookRequest.builder()
                .googleBookId("book-1")
                .title("Test Book")
                .shelf("reading")
                .rating(4)
                .build();

        when(bookEntryRepository.save(any(BookEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookEntry created = bookService.createBook("user-123", request);

        assertEquals("user-123", created.getUserId());
        assertEquals("book-1", created.getGoogleBookId());
        assertEquals("reading", created.getShelf());
        assertNotNull(created.getCreatedAt());
    }

    @Test
    void getBookForUser_shouldThrowWhenMissing() {
        when(bookEntryRepository.findByIdAndUserId("missing", "user-123")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookService.getBookForUser("missing", "user-123"));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void createBook_shouldValidateShelf() {
        BookRequest request = BookRequest.builder()
                .title("Bad shelf")
                .shelf("invalid")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookService.createBook("user-123", request));

        assertEquals(400, ex.getStatusCode().value());
    }
}
