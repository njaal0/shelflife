package com.shelflife.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload to compute completion days from a daily reading commitment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingTestDailyPlanRequest {

    @NotNull(message = "dailyReadingMinutes is required")
    @Positive(message = "dailyReadingMinutes must be greater than 0")
    private Integer dailyReadingMinutes;
}
