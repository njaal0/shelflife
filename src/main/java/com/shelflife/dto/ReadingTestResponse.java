package com.shelflife.dto;

import com.shelflife.model.ReadingTestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API response for reading-test operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingTestResponse {
    private String id;
    private ReadingTestStatus status;
    private String promptText;
    private Integer promptWordCount;
    private Integer sampleReadSeconds;
    private Double wordsPerMinute;
    private List<String> selectedBookEntryIds;
    private List<ReadingTestBookPlanResponse> bookPlans;
    private Double totalEstimatedHours;
    private Double totalEstimatedDays;
    private Integer totalEstimatedDurationDays;
    private Double totalEstimatedDurationHours;
    private Integer dailyReadingMinutes;
    private Double totalEstimatedDaysAtDailyReading;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
