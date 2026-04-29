package com.shelflife.config;

import com.shelflife.model.ReadingTest;
import com.shelflife.model.ReadingTestStatus;
import com.shelflife.repository.ReadingTestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ensures active reading-test uniqueness can be enforced safely on startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.reading-tests.active-index-initializer",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ReadingTestIndexInitializer implements ApplicationRunner {

    private static final String ACTIVE_UNIQUE_INDEX_NAME = "rt_user_active_unique_idx";
    private static final String LEGACY_ACTIVE_UNIQUE_INDEX_NAME = "rt_user_book_active_unique_idx";
    private static final Set<ReadingTestStatus> ACTIVE_STATUSES = Set.of(
            ReadingTestStatus.DRAFT,
            ReadingTestStatus.IN_PROGRESS
    );

    private final ReadingTestRepository readingTestRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Deduplicates legacy active records and creates the active unique partial index.
     *
     * @param args application startup arguments
     */
    @Override
    public void run(ApplicationArguments args) {
        List<ReadingTest> activeTests = readingTestRepository.findByStatusIn(ACTIVE_STATUSES);
        List<ReadingTest> testsToDeactivate = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (ReadingTest test : activeTests) {
            if (isBlank(test.getUserId())) {
                test.setStatus(ReadingTestStatus.SUBMITTED);
                test.setUpdatedAt(now);
                testsToDeactivate.add(test);
            }
        }

        Map<String, List<ReadingTest>> groupedByUser = activeTests.stream()
                .filter(test -> !isBlank(test.getUserId()))
                .collect(Collectors.groupingBy(ReadingTest::getUserId));

        for (List<ReadingTest> group : groupedByUser.values()) {
            if (group.size() <= 1) {
                continue;
            }

            List<ReadingTest> sorted = group.stream()
                    .sorted(Comparator
                            .comparing(this::effectiveTimestamp, Comparator.nullsFirst(Comparator.naturalOrder()))
                            .reversed()
                            .thenComparing(test -> Objects.toString(test.getId(), ""), Comparator.reverseOrder()))
                    .toList();

            for (int i = 1; i < sorted.size(); i++) {
                ReadingTest duplicate = sorted.get(i);
                duplicate.setStatus(ReadingTestStatus.SUBMITTED);
                duplicate.setUpdatedAt(now);
                testsToDeactivate.add(duplicate);
            }
        }

        if (!testsToDeactivate.isEmpty()) {
            readingTestRepository.saveAll(testsToDeactivate);
            log.info("Deactivated {} legacy active reading test records before index enforcement",
                    testsToDeactivate.size());
        }

        IndexOperations indexOperations = mongoTemplate.indexOps(ReadingTest.class);
        try {
            indexOperations.dropIndex(LEGACY_ACTIVE_UNIQUE_INDEX_NAME);
        } catch (RuntimeException ignored) {
            // Legacy index may not exist in all environments.
        }

        indexOperations.ensureIndex(new Index()
                .on("userId", Sort.Direction.ASC)
                .unique()
                .partial(PartialIndexFilter.of(Criteria.where("status").in(
                        ReadingTestStatus.DRAFT.name(),
                        ReadingTestStatus.IN_PROGRESS.name()
                )))
                .named(ACTIVE_UNIQUE_INDEX_NAME));

        log.info("Ensured MongoDB index '{}' for active reading tests", ACTIVE_UNIQUE_INDEX_NAME);
    }

    private LocalDateTime effectiveTimestamp(ReadingTest test) {
        if (test.getUpdatedAt() != null) {
            return test.getUpdatedAt();
        }
        if (test.getCreatedAt() != null) {
            return test.getCreatedAt();
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
