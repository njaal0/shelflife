package com.shelflife.service;

import com.shelflife.dto.BookRequest;
import com.shelflife.dto.BookResponse;
import com.shelflife.dto.PagedResponse;
import com.shelflife.model.BookEntry;
import com.shelflife.repository.BookEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookService {

    private static final Set<String> ALLOWED_SHELVES = Set.of("reading", "finished", "want-to-read");
    private static final Pattern ISBN_10_PATTERN = Pattern.compile("^[0-9]{9}[0-9Xx]$");
    private static final Pattern ISBN_13_PATTERN = Pattern.compile("^[0-9]{13}$");

    private final BookEntryRepository bookEntryRepository;
    private final UserService userService;

    /**
     * Returns a paginated list of all saved books for the given user.
     *
     * @param userId authenticated principal identifier
     * @param page zero-based page index
     * @param size maximum number of results per page
     * @return paged book entries
     */
    public PagedResponse<BookResponse> getBooksForUser(String userId, int page, int size) {
        userService.assertUserExists(userId);
        Pageable pageable = PageRequest.of(page, size);
        Page<BookEntry> result = bookEntryRepository.findByUserId(userId, pageable);
        return toPagedResponse(result, page, size);
    }

    /**
     * Returns a paginated list of books on the specified shelf for the given user.
     *
     * @param userId authenticated principal identifier
     * @param shelf shelf name to filter by
     * @param page zero-based page index
     * @param size maximum number of results per page
     * @return paged book entries for the shelf
     */
    public PagedResponse<BookResponse> getBooksForUserByShelf(String userId, String shelf, int page, int size) {
        userService.assertUserExists(userId);
        validateShelf(shelf);
        Pageable pageable = PageRequest.of(page, size);
        Page<BookEntry> result = bookEntryRepository.findByUserIdAndShelf(userId, shelf, pageable);
        return toPagedResponse(result, page, size);
    }

    /**
     * Returns a single saved book belonging to the given user.
     *
     * @param id book-entry identifier
     * @param userId authenticated principal identifier
     * @return book response payload
     */
    public BookResponse getBookForUser(String id, String userId) {
        userService.assertUserExists(userId);
        BookEntry entry = bookEntryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book entry not found"));
        return toResponse(entry);
    }

    /**
     * Saves a new book entry for the given user, rejecting duplicates.
     *
     * @param userId authenticated principal identifier
     * @param request book creation payload
     * @return saved book response payload
     */
    public BookResponse createBook(String userId, BookRequest request) {
        userService.assertUserExists(userId);
        validateShelf(request.getShelf());
        validateRating(request.getRating());

        String normalizedIsbn = normalizeIsbn(request.getIsbn());
        checkForDuplicate(userId, normalizedIsbn, request.getGoogleBookId());

        BookEntry bookEntry = BookEntry.builder()
                .userId(userId)
                .googleBookId(request.getGoogleBookId())
                .isbn(normalizedIsbn)
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

        return toResponse(bookEntryRepository.save(bookEntry));
    }

    /**
     * Applies partial updates to an existing book entry.
     *
     * @param id book-entry identifier
     * @param userId authenticated principal identifier
     * @param request partial update payload
     * @return updated book response payload
     */
    public BookResponse updateBook(String id, String userId, BookRequest request) {
        userService.assertUserExists(userId);
        BookEntry existing = bookEntryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book entry not found"));

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
        if (request.getIsbn() != null) {
            existing.setIsbn(normalizeIsbn(request.getIsbn()));
        }
        if (request.getStartedAt() != null) {
            existing.setStartedAt(request.getStartedAt());
        }
        if (request.getFinishedAt() != null) {
            existing.setFinishedAt(request.getFinishedAt());
        }

        return toResponse(bookEntryRepository.save(existing));
    }

    /**
     * Deletes a saved book entry belonging to the given user.
     *
     * @param id book-entry identifier
     * @param userId authenticated principal identifier
     */
    public void deleteBook(String id, String userId) {
        userService.assertUserExists(userId);
        BookEntry existing = bookEntryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book entry not found"));
        bookEntryRepository.delete(existing);
    }

    /**
     * Maps a {@link BookEntry} domain object to a {@link BookResponse} DTO.
     *
     * @param entry domain object to map
     * @return corresponding response DTO
     */
    BookResponse toResponse(BookEntry entry) {
        return BookResponse.builder()
                .id(entry.getId())
                .userId(entry.getUserId())
                .googleBookId(entry.getGoogleBookId())
                .isbn(entry.getIsbn())
                .title(entry.getTitle())
                .authors(entry.getAuthors())
                .coverImageUrl(entry.getCoverImageUrl())
                .shelf(entry.getShelf())
                .rating(entry.getRating())
                .notes(entry.getNotes())
                .startedAt(entry.getStartedAt())
                .finishedAt(entry.getFinishedAt())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private PagedResponse<BookResponse> toPagedResponse(Page<BookEntry> page, int requestedPage, int requestedSize) {
        List<BookResponse> content = page.getContent().stream().map(this::toResponse).toList();
        return PagedResponse.<BookResponse>builder()
                .content(content)
                .page(requestedPage)
                .size(requestedSize)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    private void checkForDuplicate(String userId, String normalizedIsbn, String googleBookId) {
        if (normalizedIsbn != null && bookEntryRepository.existsByUserIdAndIsbn(userId, normalizedIsbn)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Book is already on your shelf");
        }
        if (googleBookId != null && !googleBookId.isBlank()
                && bookEntryRepository.existsByUserIdAndGoogleBookId(userId, googleBookId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Book is already on your shelf");
        }
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
