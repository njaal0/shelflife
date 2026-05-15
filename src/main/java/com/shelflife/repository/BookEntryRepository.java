package com.shelflife.repository;

import com.shelflife.model.BookEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BookEntryRepository extends MongoRepository<BookEntry, String> {
    List<BookEntry> findByUserId(String userId);

    Page<BookEntry> findByUserId(String userId, Pageable pageable);

    List<BookEntry> findByUserIdAndShelf(String userId, String shelf);

    Page<BookEntry> findByUserIdAndShelf(String userId, String shelf, Pageable pageable);

    /**
     * Finds a single book for a user by ISBN.
     *
     * @param userId owner user id
     * @param isbn normalized ISBN value
     * @return matching book entry when present
     */
    Optional<BookEntry> findByUserIdAndIsbn(String userId, String isbn);

    /**
     * Checks whether a user already has a book with the given ISBN.
     *
     * @param userId owner user id
     * @param isbn normalized ISBN value
     * @return true when a matching book entry exists
     */
    boolean existsByUserIdAndIsbn(String userId, String isbn);

    /**
     * Checks whether a user already has a book with the given Google Books volume id.
     *
     * @param userId owner user id
     * @param googleBookId Google Books volume identifier
     * @return true when a matching book entry exists
     */
    boolean existsByUserIdAndGoogleBookId(String userId, String googleBookId);

    Optional<BookEntry> findByIdAndUserId(String id, String userId);

    long deleteByUserId(String userId);

    long countByUserId(String userId);
}
