package com.shelflife.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One book duration estimate generated from the user's calibrated reading speed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingTestBookPlanResponse {
    private String bookEntryId;
    private String title;
    private Integer pageCount;
    private Double estimatedHours;
    private Double estimatedDays;
    private Double estimatedDaysAtDailyReading;
}
