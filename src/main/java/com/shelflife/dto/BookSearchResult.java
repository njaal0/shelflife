package com.shelflife.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookSearchResult {
    private String googleBookId;
    private String isbn;
    private String title;
    private List<String> authors;
    private String coverImageUrl;
    private String publisher;
    private String publishedDate;
}
