package com.shelflife.repository;

import com.shelflife.model.BookEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BookEntryRepository extends MongoRepository<BookEntry, String> {
    List<BookEntry> findByUserId(String userId);

    List<BookEntry> findByUserIdAndShelf(String userId, String shelf);

    Optional<BookEntry> findByIdAndUserId(String id, String userId);

    long deleteByUserId(String userId);

    long countByUserId(String userId);
}
