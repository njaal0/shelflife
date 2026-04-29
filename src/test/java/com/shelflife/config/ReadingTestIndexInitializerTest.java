package com.shelflife.config;

import com.shelflife.model.ReadingTest;
import com.shelflife.model.ReadingTestStatus;
import com.shelflife.repository.ReadingTestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingTestIndexInitializerTest {

    @Mock
    private ReadingTestRepository readingTestRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private IndexOperations indexOperations;

    @Test
    void run_shouldDeactivateDuplicateAndMalformedActiveRecordsBeforeEnsuringIndex() throws Exception {
        ReadingTest newest = ReadingTest.builder()
                .id("t-new")
                .userId("u1")
                .status(ReadingTestStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();

        ReadingTest olderDuplicate = ReadingTest.builder()
                .id("t-old")
                .userId("u1")
                .status(ReadingTestStatus.DRAFT)
                .createdAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                .build();

        ReadingTest malformed = ReadingTest.builder()
                .id("t-bad")
                .userId("   ")
                .status(ReadingTestStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.of(2026, 2, 1, 10, 0))
                .build();

        when(readingTestRepository.findByStatusIn(Set.of(ReadingTestStatus.DRAFT, ReadingTestStatus.IN_PROGRESS)))
                .thenReturn(List.of(newest, olderDuplicate, malformed));
        when(mongoTemplate.indexOps(ReadingTest.class)).thenReturn(indexOperations);

        ReadingTestIndexInitializer initializer = new ReadingTestIndexInitializer(readingTestRepository, mongoTemplate);
        initializer.run(new DefaultApplicationArguments(new String[]{}));

        verify(readingTestRepository).saveAll(argThat(tests -> {
            List<ReadingTest> saved = new ArrayList<>();
            tests.forEach(saved::add);
            return saved.size() == 2
                    && saved.stream().anyMatch(test -> "t-old".equals(test.getId())
                    && test.getStatus() == ReadingTestStatus.SUBMITTED)
                    && saved.stream().anyMatch(test -> "t-bad".equals(test.getId())
                    && test.getStatus() == ReadingTestStatus.SUBMITTED);
        }));

        verify(indexOperations).ensureIndex(any());
    }

    @Test
    void run_shouldEnsureIndexEvenWhenNoActiveDataNeedsChanges() throws Exception {
        ReadingTest singleActive = ReadingTest.builder()
                .id("t1")
                .userId("u1")
                .status(ReadingTestStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();

        when(readingTestRepository.findByStatusIn(Set.of(ReadingTestStatus.DRAFT, ReadingTestStatus.IN_PROGRESS)))
                .thenReturn(List.of(singleActive));
        when(mongoTemplate.indexOps(ReadingTest.class)).thenReturn(indexOperations);

        ReadingTestIndexInitializer initializer = new ReadingTestIndexInitializer(readingTestRepository, mongoTemplate);
        initializer.run(new DefaultApplicationArguments(new String[]{}));

        verify(readingTestRepository, never()).saveAll(any());
        verify(indexOperations).ensureIndex(any());
    }
}
