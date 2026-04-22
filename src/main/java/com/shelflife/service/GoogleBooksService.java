package com.shelflife.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.shelflife.dto.BookSearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class GoogleBooksService {

    private static final String GOOGLE_BOOKS_BASE_URL = "https://www.googleapis.com/books/v1/volumes";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public GoogleBooksService(RestTemplateBuilder restTemplateBuilder,
                              @Value("${google.books.api.key:}") String apiKey) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiKey = apiKey;
    }

    public List<BookSearchResult> searchBooks(String title, String author, String publisher, String year) {
        String query = buildQuery(title, author, publisher);
        String urlTemplate = GOOGLE_BOOKS_BASE_URL + "?q={query}&maxResults=20";
        JsonNode response;
        Optional<String> maybeApiKey = Optional.ofNullable(apiKey).filter(k -> !k.isBlank());
        if (maybeApiKey.isPresent()) {
            urlTemplate += "&key={apiKey}";
            response = restTemplate.getForObject(urlTemplate, JsonNode.class, query, maybeApiKey.get());
        } else {
            response = restTemplate.getForObject(urlTemplate, JsonNode.class, query);
        }

        JsonNode items = response == null ? null : response.get("items");
        if (items == null || !items.isArray()) {
            return Collections.emptyList();
        }

        List<BookSearchResult> results = new ArrayList<>();
        for (JsonNode item : items) {
            JsonNode volumeInfo = item.path("volumeInfo");
            String publishedDate = volumeInfo.path("publishedDate").asText(null);

            if (year != null && !year.isBlank() && (publishedDate == null || !publishedDate.startsWith(year))) {
                continue;
            }

            results.add(BookSearchResult.builder()
                    .googleBookId(item.path("id").asText(null))
                    .title(volumeInfo.path("title").asText(null))
                    .authors(extractAuthors(volumeInfo.path("authors")))
                    .coverImageUrl(volumeInfo.path("imageLinks").path("thumbnail").asText(null))
                    .publisher(volumeInfo.path("publisher").asText(null))
                    .publishedDate(publishedDate)
                    .build());
        }

        return results;
    }

    private String buildQuery(String title, String author, String publisher) {
        List<String> parts = new ArrayList<>();
        if (title != null && !title.isBlank()) parts.add("intitle:" + title.trim());
        if (author != null && !author.isBlank()) parts.add("inauthor:" + author.trim());
        if (publisher != null && !publisher.isBlank()) parts.add("inpublisher:" + publisher.trim());
        return String.join("+", parts);
    }

    private List<String> extractAuthors(JsonNode authorsNode) {
        if (authorsNode == null || !authorsNode.isArray()) {
            return Collections.emptyList();
        }
        return StreamSupport.stream(authorsNode.spliterator(), false)
                .map(node -> node.asText(null))
                .filter(author -> author != null && !author.isBlank())
                .toList();
    }
}
