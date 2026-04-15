package com.shelflife.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {
    private String googleBookId;
    private String title;
    private List<String> authors;
    private String coverImageUrl;
    private String shelf;
    private Integer rating;
    private String notes;
    private LocalDate startedAt;
    private LocalDate finishedAt;
}
