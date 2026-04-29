package com.shelflife.service;

import com.shelflife.dto.ReadingTestBookPlanResponse;
import com.shelflife.dto.ReadingTestResponse;
import com.shelflife.model.BookEntry;
import com.shelflife.model.ReadingTest;
import com.shelflife.model.ReadingTestStatus;
import com.shelflife.repository.ReadingTestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles timed reading-speed calibration and user-scoped duration planning.
 */
@Service
@RequiredArgsConstructor
public class ReadingTestService {

    private static final int WORDS_PER_PAGE = 300;
    private static final String ACTIVE_TEST_EXISTS_MESSAGE = "An active reading speed test already exists";
    private static final String PROMPT_TEXT = "On a bright morning, Lina stepped out onto the balcony with a notebook, "
        + "a pencil, and a cup of tea. The city below was waking in layers: one bakery lifted its shutters, "
        + "a cyclist rang a bell at the corner, and a bus sighed as it stopped to collect passengers. "
        + "She decided to practice reading with focus, not speed, so she began by noticing each sentence "
        + "as a small idea that connected to the next. When she reached a difficult paragraph, she paused, "
        + "re-read it, and wrote one short summary line in the margin. The summary did not need to be perfect; "
        + "it only needed to capture meaning in her own words. After a few pages, she noticed that understanding "
        + "came from rhythm: read, reflect, and continue. She kept distractions away by placing her phone in "
        + "another room and setting a timer. By the time the tea cooled, she had finished a full passage and "
        + "could explain the main argument clearly. What surprised her most was not how much she had read, "
        + "but how calm she felt while reading with intention.";
    private static final int PROMPT_WORD_COUNT = countWords(PROMPT_TEXT);
    private static final Set<ReadingTestStatus> ACTIVE_STATUSES = Set.of(
            ReadingTestStatus.DRAFT,
            ReadingTestStatus.IN_PROGRESS
    );

    private final ReadingTestRepository readingTestRepository;
    private final BookService bookService;
    private final GoogleBooksService googleBooksService;
    private final UserService userService;

    /**
     * Starts a new in-progress reading-speed calibration test.
     *
     * @param userId authenticated user id
     * @return created reading test
     */
    public ReadingTestResponse startTest(String userId) {
        userService.assertUserExists(userId);

    if (readingTestRepository.existsByUserIdAndStatusIn(userId, ACTIVE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ACTIVE_TEST_EXISTS_MESSAGE);
        }

        LocalDateTime now = LocalDateTime.now();
        ReadingTest test = ReadingTest.builder()
                .userId(userId)
                .status(ReadingTestStatus.IN_PROGRESS)
        .promptText(PROMPT_TEXT)
        .promptWordCount(PROMPT_WORD_COUNT)
                .startedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            return toResponse(readingTestRepository.save(test));
        } catch (DuplicateKeyException ignored) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ACTIVE_TEST_EXISTS_MESSAGE);
        }
    }

    /**
         * Completes calibration and calculates per-book reading estimates.
     *
     * @param userId authenticated user id
     * @param testId user-scoped test id
         * @param sampleReadSeconds elapsed seconds to read the prompt text
         * @param bookEntryIds selected user-scoped book ids
         * @return reading estimate output
     */
        public ReadingTestResponse completeTest(String userId,
                            String testId,
                            Integer sampleReadSeconds,
                            List<String> bookEntryIds) {
        userService.assertUserExists(userId);
        ReadingTest test = getTestForUserInternal(testId, userId);

        if (test.getStatus() != ReadingTestStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Only in-progress tests can be completed");
        }

        if (sampleReadSeconds == null || sampleReadSeconds <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "sampleReadSeconds must be greater than 0");
        }

        if (bookEntryIds == null || bookEntryIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "bookEntryIds must contain at least one item");
        }

        if (test.getPromptWordCount() == null || test.getPromptWordCount() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Reading test prompt metadata is missing");
        }

        Set<String> uniqueBookIds = new HashSet<>();
        for (String bookEntryId : bookEntryIds) {
            if (bookEntryId == null || bookEntryId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "bookEntryIds must not contain blank values");
            }
            if (!uniqueBookIds.add(bookEntryId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "bookEntryIds must not contain duplicate values");
            }
        }

        double wordsPerMinute = roundToTwoDecimals(
            (test.getPromptWordCount() * 60.0) / sampleReadSeconds
        );
        List<ReadingTest.BookPlanSnapshot> bookPlans = buildBookPlans(userId, bookEntryIds, wordsPerMinute, null);
        double totalEstimatedHours = roundToTwoDecimals(bookPlans.stream()
            .mapToDouble(plan -> plan.getEstimatedHours() == null ? 0.0 : plan.getEstimatedHours())
            .sum());
        double totalEstimatedDays = roundToTwoDecimals(totalEstimatedHours / 24.0);

        LocalDateTime now = LocalDateTime.now();
        test.setSampleReadSeconds(sampleReadSeconds);
        test.setWordsPerMinute(wordsPerMinute);
        test.setSelectedBookEntryIds(bookEntryIds);
        test.setBookPlans(bookPlans);
        test.setTotalEstimatedHours(totalEstimatedHours);
        test.setTotalEstimatedDays(totalEstimatedDays);
        test.setDailyReadingMinutes(null);
        test.setTotalEstimatedDaysAtDailyReading(null);
        test.setStatus(ReadingTestStatus.SUBMITTED);
        test.setSubmittedAt(now);
        test.setUpdatedAt(now);

        return toResponse(readingTestRepository.save(test));
    }

        /**
         * Applies a daily reading commitment and calculates completion days.
         *
         * @param userId authenticated user id
         * @param testId user-scoped reading test id
         * @param dailyReadingMinutes daily reading commitment in minutes
         * @return updated reading estimate output
         */
        public ReadingTestResponse applyDailyPlan(String userId, String testId, Integer dailyReadingMinutes) {
        userService.assertUserExists(userId);
        ReadingTest test = getTestForUserInternal(testId, userId);

        if (test.getStatus() != ReadingTestStatus.SUBMITTED && test.getStatus() != ReadingTestStatus.SCORED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Reading test must be completed before daily planning");
        }

        if (dailyReadingMinutes == null || dailyReadingMinutes <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "dailyReadingMinutes must be greater than 0");
        }

        if (test.getBookPlans() == null || test.getBookPlans().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Reading test has no calculated book estimates");
        }

        List<ReadingTest.BookPlanSnapshot> updatedBookPlans = test.getBookPlans().stream()
            .map(plan -> {
                double estimatedDaysAtDailyReading = roundToTwoDecimals(
                    (plan.getEstimatedHours() * 60.0) / dailyReadingMinutes
                );
                plan.setEstimatedDaysAtDailyReading(estimatedDaysAtDailyReading);
                return plan;
            })
            .toList();

        double totalEstimatedDaysAtDailyReading = roundToTwoDecimals(updatedBookPlans.stream()
            .mapToDouble(plan -> plan.getEstimatedDaysAtDailyReading() == null ? 0.0 : plan.getEstimatedDaysAtDailyReading())
            .sum());

        test.setBookPlans(updatedBookPlans);
        test.setDailyReadingMinutes(dailyReadingMinutes);
        test.setTotalEstimatedDaysAtDailyReading(totalEstimatedDaysAtDailyReading);
        test.setStatus(ReadingTestStatus.SCORED);
        test.setUpdatedAt(LocalDateTime.now());

        return toResponse(readingTestRepository.save(test));
        }

    /**
     * Gets one reading test for an authenticated user.
     *
     * @param testId test id
     * @param userId authenticated user id
     * @return reading test response
     */
    public ReadingTestResponse getTestForUser(String testId, String userId) {
        userService.assertUserExists(userId);
        return toResponse(getTestForUserInternal(testId, userId));
    }

    /**
     * Lists user-scoped reading tests with optional filters.
     *
     * @param userId authenticated user id
     * @param status optional status filter
     * @param from optional created-date lower bound (inclusive)
     * @param to optional created-date upper bound (inclusive)
     * @return matching reading tests
     */
    public List<ReadingTestResponse> listTestsForUser(
            String userId,
            ReadingTestStatus status,
            LocalDate from,
            LocalDate to
    ) {
        userService.assertUserExists(userId);

        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "from must be on or before to");
        }

        List<ReadingTest> tests = fetchTests(userId, status);
        return tests.stream()
                .filter(test -> matchesDateRange(test, from, to))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Deletes all reading tests for a user.
     *
     * @param userId authenticated user id
     */
    public void deleteTestsForUser(String userId) {
        readingTestRepository.deleteByUserId(userId);
    }

    private ReadingTest getTestForUserInternal(String testId, String userId) {
        return readingTestRepository.findByIdAndUserId(testId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reading test not found"));
    }

    private List<ReadingTest> fetchTests(String userId, ReadingTestStatus status) {
        if (status != null) {
            return readingTestRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        }

        return readingTestRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private boolean matchesDateRange(ReadingTest test, LocalDate from, LocalDate to) {
        if (test.getCreatedAt() == null) {
            return from == null && to == null;
        }

        LocalDate createdDate = test.getCreatedAt().toLocalDate();
        if (from != null && createdDate.isBefore(from)) {
            return false;
        }
        if (to != null && createdDate.isAfter(to)) {
            return false;
        }
        return true;
    }

        private List<ReadingTest.BookPlanSnapshot> buildBookPlans(String userId,
                                      List<String> bookEntryIds,
                                      double wordsPerMinute,
                                      Integer dailyReadingMinutes) {
        return bookEntryIds.stream()
            .map(bookEntryId -> buildBookPlan(userId, bookEntryId, wordsPerMinute, dailyReadingMinutes))
            .toList();
        }

        private ReadingTest.BookPlanSnapshot buildBookPlan(String userId,
                                   String bookEntryId,
                                   double wordsPerMinute,
                                   Integer dailyReadingMinutes) {
        BookEntry bookEntry = bookService.getBookForUser(bookEntryId, userId);
        if (bookEntry.getGoogleBookId() == null || bookEntry.getGoogleBookId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Selected book has no Google volume id for page-count lookup");
        }

        Integer pageCount;
        try {
            pageCount = googleBooksService.getPageCount(bookEntry.getGoogleBookId());
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Google Books page-count lookup failed. Please try again later.", ex);
        }

        if (pageCount == null || pageCount <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unable to determine page count for selected book: " + safeTitle(bookEntry));
        }

        double estimatedHours = roundToTwoDecimals((pageCount * WORDS_PER_PAGE) / wordsPerMinute / 60.0);
        double estimatedDays = roundToTwoDecimals(estimatedHours / 24.0);
        Double estimatedDaysAtDailyReading = null;
        if (dailyReadingMinutes != null && dailyReadingMinutes > 0) {
            estimatedDaysAtDailyReading = roundToTwoDecimals((estimatedHours * 60.0) / dailyReadingMinutes);
        }

        return ReadingTest.BookPlanSnapshot.builder()
            .bookEntryId(bookEntry.getId())
            .title(safeTitle(bookEntry))
            .pageCount(pageCount)
            .estimatedHours(estimatedHours)
            .estimatedDays(estimatedDays)
            .estimatedDaysAtDailyReading(estimatedDaysAtDailyReading)
            .build();
        }

        private String safeTitle(BookEntry bookEntry) {
        if (bookEntry.getTitle() == null || bookEntry.getTitle().isBlank()) {
            return "Unknown title";
        }
        return bookEntry.getTitle();
        }

        private static int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
        }

        private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
        }

    private ReadingTestResponse toResponse(ReadingTest test) {
        DurationBreakdown totalDuration = toDurationBreakdown(test.getTotalEstimatedHours());

        return ReadingTestResponse.builder()
                .id(test.getId())
                .status(test.getStatus())
            .promptText(test.getPromptText())
            .promptWordCount(test.getPromptWordCount())
            .sampleReadSeconds(test.getSampleReadSeconds())
            .wordsPerMinute(test.getWordsPerMinute())
            .selectedBookEntryIds(test.getSelectedBookEntryIds() == null ? List.of() : test.getSelectedBookEntryIds())
            .bookPlans(test.getBookPlans() == null ? List.of() : test.getBookPlans().stream()
                .map(plan -> ReadingTestBookPlanResponse.builder()
                    .bookEntryId(plan.getBookEntryId())
                    .title(plan.getTitle())
                    .pageCount(plan.getPageCount())
                    .estimatedHours(plan.getEstimatedHours())
                    .estimatedDays(plan.getEstimatedDays())
                    .estimatedDaysAtDailyReading(plan.getEstimatedDaysAtDailyReading())
                    .build())
                .toList())
            .totalEstimatedHours(test.getTotalEstimatedHours())
            .totalEstimatedDays(test.getTotalEstimatedDays())
            .totalEstimatedDurationDays(totalDuration == null ? null : totalDuration.days())
            .totalEstimatedDurationHours(totalDuration == null ? null : totalDuration.hours())
            .dailyReadingMinutes(test.getDailyReadingMinutes())
            .totalEstimatedDaysAtDailyReading(test.getTotalEstimatedDaysAtDailyReading())
                .startedAt(test.getStartedAt())
                .submittedAt(test.getSubmittedAt())
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .build();
    }

    private DurationBreakdown toDurationBreakdown(Double totalEstimatedHours) {
        if (totalEstimatedHours == null || totalEstimatedHours < 0) {
            return null;
        }

        int days = (int) Math.floor(totalEstimatedHours / 24.0);
        double hours = roundToTwoDecimals(totalEstimatedHours - (days * 24.0));
        if (hours >= 24.0) {
            days += 1;
            hours = 0.0;
        }

        return new DurationBreakdown(days, hours);
    }

    private record DurationBreakdown(int days, double hours) {
    }
}
