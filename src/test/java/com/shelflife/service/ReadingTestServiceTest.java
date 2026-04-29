package com.shelflife.service;

import com.shelflife.dto.ReadingTestResponse;
import com.shelflife.model.BookEntry;
import com.shelflife.model.ReadingTest;
import com.shelflife.model.ReadingTestStatus;
import com.shelflife.repository.ReadingTestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingTestServiceTest {

    @Mock
    private ReadingTestRepository readingTestRepository;

    @Mock
    private BookService bookService;

        @Mock
        private GoogleBooksService googleBooksService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ReadingTestService readingTestService;

    @Test
    void startTest_shouldCreateInProgressCalibration() {
        when(readingTestRepository.existsByUserIdAndStatusIn(any(), any())).thenReturn(false);
        when(readingTestRepository.save(any(ReadingTest.class))).thenAnswer(invocation -> {
            ReadingTest saved = invocation.getArgument(0);
            saved.setId("test-1");
            return saved;
        });

        ReadingTestResponse response = readingTestService.startTest("user-1");

        assertEquals("test-1", response.getId());
        assertEquals(ReadingTestStatus.IN_PROGRESS, response.getStatus());
        assertNotNull(response.getPromptText());
        assertNotNull(response.getPromptWordCount());
        verify(userService).assertUserExists("user-1");
        verify(readingTestRepository).save(any(ReadingTest.class));
    }

    @Test
    void startTest_shouldRejectWhenActiveTestExists() {
        when(readingTestRepository.existsByUserIdAndStatusIn(any(), any())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingTestService.startTest("user-1"));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("An active reading speed test already exists", ex.getReason());
    }

    @Test
    void startTest_shouldRejectWhenConcurrentInsertHitsUniqueIndex() {
        when(readingTestRepository.existsByUserIdAndStatusIn(any(), any())).thenReturn(false);
        when(readingTestRepository.save(any(ReadingTest.class)))
                .thenThrow(new DuplicateKeyException("duplicate key"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingTestService.startTest("user-1"));

        assertEquals(409, ex.getStatusCode().value());
        assertEquals("An active reading speed test already exists", ex.getReason());
    }

    @Test
    void completeTest_shouldCalculateBookDurations() {
        ReadingTest test = ReadingTest.builder()
                .id("test-1")
                .userId("user-1")
                .status(ReadingTestStatus.IN_PROGRESS)
                .promptWordCount(250)
                .createdAt(LocalDateTime.now())
                .build();

        BookEntry book = BookEntry.builder()
                .id("book-1")
                .userId("user-1")
                .googleBookId("g-1")
                .title("Clean Code")
                .build();

        when(readingTestRepository.findByIdAndUserId("test-1", "user-1")).thenReturn(Optional.of(test));
        when(bookService.getBookForUser("book-1", "user-1")).thenReturn(book);
        when(googleBooksService.getPageCount("g-1")).thenReturn(300);
        when(readingTestRepository.save(any(ReadingTest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReadingTestResponse response = readingTestService.completeTest("user-1", "test-1", 75, List.of("book-1"));

        assertEquals(ReadingTestStatus.SUBMITTED, response.getStatus());
        assertEquals(200.0, response.getWordsPerMinute());
        assertEquals(1, response.getBookPlans().size());
        assertNotNull(response.getTotalEstimatedHours());
        assertEquals(0, response.getTotalEstimatedDurationDays());
        assertEquals(7.5, response.getTotalEstimatedDurationHours());
        assertNotNull(response.getSubmittedAt());
    }

    @Test
    void completeTest_shouldMapGoogleBooksClientFailureToBadGateway() {
        ReadingTest test = ReadingTest.builder()
                .id("test-1")
                .userId("user-1")
                .status(ReadingTestStatus.IN_PROGRESS)
                .promptWordCount(250)
                .createdAt(LocalDateTime.now())
                .build();

        BookEntry book = BookEntry.builder()
                .id("book-1")
                .userId("user-1")
                .googleBookId("g-1")
                .title("Network Failure")
                .build();

        when(readingTestRepository.findByIdAndUserId("test-1", "user-1")).thenReturn(Optional.of(test));
        when(bookService.getBookForUser("book-1", "user-1")).thenReturn(book);
        when(googleBooksService.getPageCount("g-1"))
                .thenThrow(new RestClientException("upstream unavailable"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingTestService.completeTest("user-1", "test-1", 75, List.of("book-1")));

        assertEquals(502, ex.getStatusCode().value());
        assertEquals("Google Books page-count lookup failed. Please try again later.", ex.getReason());
    }

    @Test
    void completeTest_shouldRejectWhenPageCountIsUnavailable() {
        ReadingTest test = ReadingTest.builder()
                .id("test-1")
                .userId("user-1")
                .status(ReadingTestStatus.IN_PROGRESS)
                .promptWordCount(250)
                .createdAt(LocalDateTime.now())
                .build();

        BookEntry book = BookEntry.builder()
                .id("book-1")
                .userId("user-1")
                .googleBookId("g-1")
                .title("Unknown Pages")
                .build();

        when(readingTestRepository.findByIdAndUserId("test-1", "user-1")).thenReturn(Optional.of(test));
        when(bookService.getBookForUser("book-1", "user-1")).thenReturn(book);
        when(googleBooksService.getPageCount("g-1")).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> readingTestService.completeTest("user-1", "test-1", 75, List.of("book-1")));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Unable to determine page count for selected book: Unknown Pages", ex.getReason());
    }

    @Test
    void applyDailyPlan_shouldCalculateDaysFromDailyMinutes() {
        ReadingTest test = ReadingTest.builder()
                .id("test-1")
                .userId("user-1")
                .status(ReadingTestStatus.SUBMITTED)
                .bookPlans(List.of(
                        ReadingTest.BookPlanSnapshot.builder()
                                .bookEntryId("book-1")
                                .title("Clean Code")
                                .pageCount(300)
                                .estimatedHours(7.5)
                                .estimatedDays(0.31)
                                .build()
                ))
                .createdAt(LocalDateTime.now())
                .build();

        when(readingTestRepository.findByIdAndUserId("test-1", "user-1")).thenReturn(Optional.of(test));
        when(readingTestRepository.save(any(ReadingTest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReadingTestResponse response = readingTestService.applyDailyPlan("user-1", "test-1", 45);

        assertEquals(ReadingTestStatus.SCORED, response.getStatus());
        assertEquals(45, response.getDailyReadingMinutes());
        assertNotNull(response.getTotalEstimatedDaysAtDailyReading());
    }

    @Test
    void listTestsForUser_shouldApplyDateRangeFilter() {
        ReadingTest oldTest = ReadingTest.builder()
                .id("old")
                .userId("user-1")
                .status(ReadingTestStatus.SCORED)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        ReadingTest recentTest = ReadingTest.builder()
                .id("recent")
                .userId("user-1")
                .status(ReadingTestStatus.SCORED)
                .createdAt(LocalDateTime.of(2026, 3, 1, 10, 0))
                .build();

        when(readingTestRepository.findByUserIdOrderByCreatedAtDesc("user-1"))
                .thenReturn(List.of(recentTest, oldTest));

        List<ReadingTestResponse> responses = readingTestService.listTestsForUser(
                "user-1",
                null,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 4, 1)
        );

        assertEquals(1, responses.size());
        assertEquals("recent", responses.get(0).getId());
    }
}
