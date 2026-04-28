package com.shelflife.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "book_entries")
@CompoundIndex(name = "user_isbn_idx", def = "{'userId': 1, 'isbn': 1}", sparse = true)
public class BookEntry {

    @Id
    private String id;
    private String userId;
    private String googleBookId;
    @Indexed(name = "isbn_idx", sparse = true)
    private String isbn;
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
