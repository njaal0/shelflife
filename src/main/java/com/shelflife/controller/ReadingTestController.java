package com.shelflife.controller;

import com.shelflife.dto.ReadingTestCompletionRequest;
import com.shelflife.dto.ReadingTestDailyPlanRequest;
import com.shelflife.dto.ReadingTestResponse;
import com.shelflife.model.ReadingTestStatus;
import com.shelflife.service.ReadingTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Exposes user-scoped reading-test operations.
 */
@RestController
@RequestMapping("/api/reading-tests")
@RequiredArgsConstructor
public class ReadingTestController {

    private final ReadingTestService readingTestService;

    /**
     * Starts a general reading-speed calibration test.
     *
     * @param authentication authenticated user principal
     * @return created reading-speed test with one-page prompt text
     */
    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public ReadingTestResponse startTest(Authentication authentication) {
        return readingTestService.startTest(authentication.getName());
    }

    /**
     * Completes calibration by submitting reading duration and selected books.
     *
     * @param testId user-scoped reading test id
     * @param request completion payload
     * @param authentication authenticated user principal
     * @return reading duration estimates across selected books
     */
    @PostMapping("/{testId}/complete")
    public ReadingTestResponse completeTest(@PathVariable String testId,
                                            @Valid @RequestBody ReadingTestCompletionRequest request,
                                            Authentication authentication) {
        return readingTestService.completeTest(
                authentication.getName(),
                testId,
                request.getSampleReadSeconds(),
                request.getBookEntryIds()
        );
    }

    /**
     * Applies a daily reading commitment and calculates completion days.
     *
     * @param testId user-scoped reading test id
     * @param request daily reading plan payload
     * @param authentication authenticated user principal
     * @return updated reading test with daily completion estimates
     */
    @PostMapping("/{testId}/daily-plan")
    public ReadingTestResponse applyDailyPlan(@PathVariable String testId,
                                              @Valid @RequestBody ReadingTestDailyPlanRequest request,
                                              Authentication authentication) {
        return readingTestService.applyDailyPlan(
                authentication.getName(),
                testId,
                request.getDailyReadingMinutes()
        );
    }

    /**
     * Returns one reading test belonging to the authenticated user.
     *
     * @param testId user-scoped test id
     * @param authentication authenticated user principal
     * @return reading test details
     */
    @GetMapping("/{testId}")
    public ReadingTestResponse getTest(@PathVariable String testId,
                                       Authentication authentication) {
        return readingTestService.getTestForUser(testId, authentication.getName());
    }

    /**
     * Lists reading tests for the authenticated user with optional filters.
     *
     * @param status optional status filter
     * @param from optional created-date lower bound (inclusive)
     * @param to optional created-date upper bound (inclusive)
     * @param authentication authenticated user principal
     * @return filtered reading tests
     */
    @GetMapping
    public List<ReadingTestResponse> listTests(
            @RequestParam(required = false) ReadingTestStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        return readingTestService.listTestsForUser(authentication.getName(), status, from, to);
    }
}
