package com.shelflife.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request payload for creating or updating a saved book entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request payload for creating or updating a saved book entry.")
public class BookRequest {

    @Schema(description = "Google Books volume identifier.", example = "NggnmAEACAAJ")
    private String googleBookId;

    @Schema(description = "ISBN-10 or ISBN-13 (hyphens/spaces allowed).", example = "978-0134685991")
    private String isbn;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    @Schema(description = "Book title.", example = "Effective Java", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "List of author names.")
    private List<String> authors;

    @Size(max = 2048, message = "Cover image URL must not exceed 2048 characters")
    @Schema(description = "URL of the book cover thumbnail.")
    private String coverImageUrl;

    @NotBlank(message = "Shelf is required")
    @Schema(description = "Reading shelf.", allowableValues = {"reading", "finished", "want-to-read"}, requiredMode = Schema.RequiredMode.REQUIRED)
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
