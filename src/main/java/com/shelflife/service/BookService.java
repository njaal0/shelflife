package com.shelflife.service;

import com.shelflife.dto.BookRequest;
import com.shelflife.model.BookEntry;
import com.shelflife.repository.BookEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookService {

    private static final Set<String> ALLOWED_SHELVES = Set.of("reading", "finished", "want-to-read");

    private final BookEntryRepository bookEntryRepository;
    private final UserService userService;

    public List<BookEntry> getBooksForUser(String userId) {
        userService.assertUserExists(userId);
        return bookEntryRepository.findByUserId(userId);
    }

    public List<BookEntry> getBooksForUserByShelf(String userId, String shelf) {
        userService.assertUserExists(userId);
        validateShelf(shelf);
        return bookEntryRepository.findByUserIdAndShelf(userId, shelf);
    }

    public BookEntry getBookForUser(String id, String userId) {
        userService.assertUserExists(userId);
        return bookEntryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book entry not found"));
    }

    public BookEntry createBook(String userId, BookRequest request) {
        userService.assertUserExists(userId);
        validateShelf(request.getShelf());
        validateRating(request.getRating());

        BookEntry bookEntry = BookEntry.builder()
                .userId(userId)
                .googleBookId(request.getGoogleBookId())
                .title(request.getTitle())
                .authors(request.getAuthors())
                .coverImageUrl(request.getCoverImageUrl())
                .shelf(request.getShelf())
                .rating(request.getRating())
                .notes(request.getNotes())
                .startedAt(request.getStartedAt())
                .finishedAt(request.getFinishedAt())
                .createdAt(LocalDateTime.now())
                .build();

        return bookEntryRepository.save(bookEntry);
    }

    public BookEntry updateBook(String id, String userId, BookRequest request) {
        userService.assertUserExists(userId);
        BookEntry existing = getBookForUser(id, userId);

        if (request.getShelf() != null) {
            validateShelf(request.getShelf());
            existing.setShelf(request.getShelf());
        }
        if (request.getRating() != null) {
            validateRating(request.getRating());
            existing.setRating(request.getRating());
        }
        if (request.getNotes() != null) {
            existing.setNotes(request.getNotes());
        }
        if (request.getStartedAt() != null) {
            existing.setStartedAt(request.getStartedAt());
        }
        if (request.getFinishedAt() != null) {
            existing.setFinishedAt(request.getFinishedAt());
        }

        return bookEntryRepository.save(existing);
    }

    public void deleteBook(String id, String userId) {
        userService.assertUserExists(userId);
        BookEntry existing = getBookForUser(id, userId);
        bookEntryRepository.delete(existing);
    }

    private void validateShelf(String shelf) {
        if (shelf == null || !ALLOWED_SHELVES.contains(shelf)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid shelf. Allowed values: reading, finished, want-to-read");
        }
    }

    private void validateRating(Integer rating) {
        if (rating != null && (rating < 1 || rating > 6)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 6");
        }
    }
}
