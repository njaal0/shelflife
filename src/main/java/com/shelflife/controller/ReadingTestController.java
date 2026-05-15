package com.shelflife.controller;

import com.shelflife.dto.ReadingTestCompletionRequest;
import com.shelflife.dto.ReadingTestDailyPlanRequest;
import com.shelflife.dto.ReadingTestResponse;
import com.shelflife.dto.PagedResponse;
import com.shelflife.model.ReadingTestStatus;
import com.shelflife.service.ReadingTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Exposes user-scoped reading-test operations.
 */
@RestController
@RequestMapping("/api/reading-tests")
@RequiredArgsConstructor
@Tag(name = "Reading Tests", description = "Reading speed calibration and book completion planning.")
@SecurityRequirement(name = "bearerAuth")
public class ReadingTestController {

    private final ReadingTestService readingTestService;

    /**
     * Starts a general reading-speed calibration test.
     *
     * @param authentication authenticated user principal
     * @return created reading-speed test with one-page prompt text
     */
    @Operation(summary = "Start a reading test", description = "Creates a new in-progress reading speed calibration test for the authenticated user.")
    @ApiResponse(responseCode = "201", description = "Reading test created")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "An active reading test already exists", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
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
    @Operation(summary = "Complete a reading test", description = "Submits the reading duration and selected book IDs to calculate per-book reading estimates.")
    @ApiResponse(responseCode = "200", description = "Reading test completed with estimates")
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Reading test not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "Test is not in progress", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
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
    @Operation(summary = "Apply daily reading plan", description = "Submits a daily reading commitment in minutes and recalculates estimated completion days for selected books.")
    @ApiResponse(responseCode = "200", description = "Updated reading test with daily estimates")
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Reading test not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "Test has not been completed yet", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
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
    @Operation(summary = "Get a reading test", description = "Returns a single reading test belonging to the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Reading test details")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Reading test not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
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
     * @param page zero-based page index (default 0)
     * @param size maximum results per page (default 20)
     * @param authentication authenticated user principal
     * @return paginated and filtered reading tests
     */
    @Operation(summary = "List reading tests", description = "Returns a paginated list of reading tests for the authenticated user with optional status and date filters.")
    @ApiResponse(responseCode = "200", description = "Paginated reading test list")
    @ApiResponse(responseCode = "400", description = "Invalid date range", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @GetMapping
    public PagedResponse<ReadingTestResponse> listTests(
            @RequestParam(required = false) ReadingTestStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Maximum results per page") @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return readingTestService.listTestsForUser(authentication.getName(), status, from, to, page, size);
    }
}
