package com.shelflife.repository;

import com.shelflife.model.ReadingTest;
import com.shelflife.model.ReadingTestStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Persists and retrieves user-scoped reading tests.
 */
public interface ReadingTestRepository extends MongoRepository<ReadingTest, String> {

    List<ReadingTest> findByStatusIn(Collection<ReadingTestStatus> statuses);

    List<ReadingTest> findByUserIdOrderByCreatedAtDesc(String userId);

    List<ReadingTest> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, ReadingTestStatus status);

    Optional<ReadingTest> findByIdAndUserId(String id, String userId);

    boolean existsByUserIdAndStatusIn(
            String userId,
            Collection<ReadingTestStatus> statuses
    );

    long deleteByUserId(String userId);
}
