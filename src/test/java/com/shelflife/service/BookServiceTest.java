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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookEntryRepository bookEntryRepository;

        @Mock
        private UserService userService;

    @InjectMocks
    private BookService bookService;

    @Test
    void createBook_shouldCreateBookEntry() {
        BookRequest request = BookRequest.builder()
                .googleBookId("book-1")
                                .isbn("978-0134685991")
                .title("Test Book")
                .shelf("reading")
                .rating(4)
                .build();

        when(bookEntryRepository.save(any(BookEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookEntry created = bookService.createBook("user-123", request);

        assertEquals("user-123", created.getUserId());
        assertEquals("book-1", created.getGoogleBookId());
        assertEquals("9780134685991", created.getIsbn());
        assertEquals("reading", created.getShelf());
        assertNotNull(created.getCreatedAt());
        verify(userService).assertUserExists("user-123");
    }

    @Test
    void updateBook_shouldUpdateIsbnWhenProvided() {
        BookEntry existing = BookEntry.builder()
                .id("book-9")
                .userId("user-123")
                .isbn("9780321356680")
                .shelf("reading")
                .build();

        BookRequest request = BookRequest.builder()
                .isbn("978-0132350884")
                .build();

        when(bookEntryRepository.findByIdAndUserId("book-9", "user-123")).thenReturn(Optional.of(existing));
        when(bookEntryRepository.save(any(BookEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookEntry updated = bookService.updateBook("book-9", "user-123", request);

        assertEquals("9780132350884", updated.getIsbn());
        verify(userService, times(2)).assertUserExists("user-123");
    }

    @Test
    void createBook_shouldRejectInvalidIsbn() {
        BookRequest request = BookRequest.builder()
                .title("Bad isbn")
                .shelf("reading")
                .isbn("invalid")
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookService.createBook("user-123", request));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Invalid isbn format. Use ISBN-10 or ISBN-13.", ex.getReason());
        verify(userService).assertUserExists("user-123");
    }

    @Test
    void updateBook_shouldClearIsbnWhenBlank() {
        BookEntry existing = BookEntry.builder()
                .id("book-10")
                .userId("user-123")
                .isbn("9780132350884")
                .shelf("reading")
                .build();

        BookRequest request = BookRequest.builder()
                .isbn("   ")
                .build();

        when(bookEntryRepository.findByIdAndUserId("book-10", "user-123")).thenReturn(Optional.of(existing));
        when(bookEntryRepository.save(any(BookEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookEntry updated = bookService.updateBook("book-10", "user-123", request);

        assertNull(updated.getIsbn());
        verify(userService, times(2)).assertUserExists("user-123");
    }

    @Test
    void getBookForUser_shouldThrowWhenMissing() {
        when(bookEntryRepository.findByIdAndUserId("missing", "user-123")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookService.getBookForUser("missing", "user-123"));

        assertEquals(404, ex.getStatusCode().value());
        verify(userService).assertUserExists("user-123");
    }

        @Test
        void getBookForUser_shouldNotExposeBookOwnedByAnotherUser() {
                when(bookEntryRepository.findByIdAndUserId("book-1", "other-user")).thenReturn(Optional.empty());

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                                () -> bookService.getBookForUser("book-1", "other-user"));

                assertEquals(404, ex.getStatusCode().value());
                verify(userService).assertUserExists("other-user");
                verify(bookEntryRepository).findByIdAndUserId("book-1", "other-user");
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
        assertTrue(ex.getReason().contains("Invalid shelf"));
        verify(userService).assertUserExists("user-123");
    }

    @Test
    void createBook_shouldAcceptMaxRating() {
        BookRequest request = BookRequest.builder()
                .googleBookId("book-2")
                .title("Dice Book")
                .shelf("finished")
                .rating(6)
                .build();

        when(bookEntryRepository.save(any(BookEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookEntry created = bookService.createBook("user-123", request);
        assertEquals(6, created.getRating());
        verify(userService).assertUserExists("user-123");
    }

    @Test
    void createBook_shouldRejectRatingAboveSix() {
        BookRequest request = BookRequest.builder()
                .title("Over rated")
                .shelf("finished")
                .rating(7)
                .build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> bookService.createBook("user-123", request));

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Rating must be between 1 and 6"));
        verify(userService).assertUserExists("user-123");
    }

    @Test
    void deleteBook_shouldDeleteBookForUser() {
        BookEntry existing = BookEntry.builder()
                .id("book-1")
                .userId("user-123")
                .build();

        when(bookEntryRepository.findByIdAndUserId("book-1", "user-123")).thenReturn(Optional.of(existing));

        bookService.deleteBook("book-1", "user-123");

        verify(userService, times(2)).assertUserExists("user-123");
        verify(bookEntryRepository).delete(existing);
    }
}
