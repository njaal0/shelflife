package com.shelflife.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic page wrapper returned from paginated list endpoints.
 *
 * @param <T> element type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A paginated response wrapper.")
public class PagedResponse<T> {

    @Schema(description = "Elements on the current page.")
    private List<T> content;

    @Schema(description = "Zero-based current page index.", example = "0")
    private int page;

    @Schema(description = "Maximum number of elements per page.", example = "20")
    private int size;

    @Schema(description = "Total number of matching elements across all pages.", example = "42")
    private long totalElements;

    @Schema(description = "Total number of pages.", example = "3")
    private int totalPages;
}
