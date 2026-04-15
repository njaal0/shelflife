package com.shelflife.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "book_entries")
public class BookEntry {

    @Id
    private String id;
    private String userId;
    private String googleBookId;
    private String title;
    private List<String> authors;
    private String coverImageUrl;
    private String shelf;
    private Integer rating;
    private String notes;
    private LocalDate startedAt;
    private LocalDate finishedAt;
    private LocalDateTime createdAt;
}
