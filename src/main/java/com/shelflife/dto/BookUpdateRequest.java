package com.shelflife.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request payload for partially updating a saved book entry.
 * <p>
 * All fields are optional. To update a field, include it in the request payload with a non-null value.
 * To leave a field unchanged, either omit it from the payload or set it to null.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for partially updating a saved book entry.")
public class BookUpdateRequest {

    @Schema(description = "ISBN-10 or ISBN-13 (hyphens/spaces allowed).", example = "978-0132350884")
    private String isbn;

    @Schema(description = "Reading shelf.", allowableValues = {"reading", "finished", "want-to-read"})
    private String shelf;

    @Min(value = 1, message = "Rating must be between 1 and 6")
    @Max(value = 6, message = "Rating must be between 1 and 6")
    @Schema(description = "User rating between 1 and 6.", minimum = "1", maximum = "6")
    private Integer rating;

    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    @Schema(description = "User notes about the book.")
    private String notes;

    @Schema(description = "Date the user started reading (ISO-8601).", example = "2024-01-15")
    private LocalDate startedAt;

    @Schema(description = "Date the user finished reading (ISO-8601).", example = "2024-03-22")
    private LocalDate finishedAt;
}