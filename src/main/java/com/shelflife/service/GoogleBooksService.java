package com.shelflife.service;

import com.shelflife.dto.BookSearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class GoogleBooksService {

    private final RestTemplate restTemplate;
    private final String googleBooksBaseUrl;
    private final String apiKey;

    public GoogleBooksService(RestTemplateBuilder restTemplateBuilder,
                              @Value("${google.books.api.base-url:https://www.googleapis.com/books/v1/volumes}") String googleBooksBaseUrl,
                              @Value("${google.books.api.key:}") String apiKey) {
        this.restTemplate = restTemplateBuilder.build();
        this.googleBooksBaseUrl = googleBooksBaseUrl;
        this.apiKey = apiKey;
    }

    public List<BookSearchResult> searchBooks(String query) {
        String url = UriComponentsBuilder
                .fromHttpUrl(googleBooksBaseUrl)
                .queryParam("q", query)
                .queryParamIfPresent("key", apiKey == null || apiKey.isBlank() ? java.util.Optional.empty() : java.util.Optional.of(apiKey))
                .toUriString();

        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null || response.get("items") == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
        List<BookSearchResult> results = new ArrayList<>();

        for (Map<String, Object> item : items) {
            Map<String, Object> volumeInfo = (Map<String, Object>) item.getOrDefault("volumeInfo", Collections.emptyMap());
            Map<String, Object> imageLinks = (Map<String, Object>) volumeInfo.getOrDefault("imageLinks", Collections.emptyMap());
            results.add(BookSearchResult.builder()
                    .googleBookId((String) item.get("id"))
                    .title((String) volumeInfo.get("title"))
                    .authors((List<String>) volumeInfo.getOrDefault("authors", Collections.emptyList()))
                    .coverImageUrl((String) imageLinks.get("thumbnail"))
                    .build());
        }

        return results;
    }
}
