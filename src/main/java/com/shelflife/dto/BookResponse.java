package com.shelflife.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API response payload for a single saved book entry.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A saved book entry belonging to the authenticated user.")
public class BookResponse {

    @Schema(description = "Unique book-entry identifier.", example = "64f1a2b3c4d5e6f7a8b9c0d1")
    private String id;

    @Schema(description = "Owner user identifier.", example = "uid-abc123")
    private String userId;

    @Schema(description = "Google Books volume identifier.", example = "NggnmAEACAAJ")
    private String googleBookId;

    @Schema(description = "Normalized ISBN-10 or ISBN-13.", example = "9780134685991")
    private String isbn;

    @Schema(description = "Book title.", example = "Effective Java")
    private String title;

    @Schema(description = "List of author names.")
    private List<String> authors;

    @Schema(description = "URL of the book cover thumbnail.")
    private String coverImageUrl;

    @Schema(description = "Reading shelf the book belongs to.", allowableValues = {"reading", "finished", "want-to-read"})
    private String shelf;

    @Schema(description = "User rating between 1 and 6.", minimum = "1", maximum = "6")
    private Integer rating;

    @Schema(description = "User notes about the book.")
    private String notes;

    @Schema(description = "Date the user started reading (ISO-8601).", example = "2024-01-15")
    private LocalDate startedAt;

    @Schema(description = "Date the user finished reading (ISO-8601).", example = "2024-03-22")
    private LocalDate finishedAt;

    @Schema(description = "Timestamp when the entry was first created.")
    private LocalDateTime createdAt;
}
