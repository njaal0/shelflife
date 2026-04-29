package com.shelflife.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload used to complete calibration and generate reading estimates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingTestCompletionRequest {

    @NotNull(message = "sampleReadSeconds is required")
    @Positive(message = "sampleReadSeconds must be greater than 0")
    private Integer sampleReadSeconds;

    @NotEmpty(message = "bookEntryIds must contain at least one item")
    private List<@NotBlank(message = "bookEntryIds must not contain blank values") String> bookEntryIds;
}
