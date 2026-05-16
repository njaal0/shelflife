package com.shelflife.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.shelflife.dto.BookSearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 * Integrates with Google Books API and maps external volume responses into local DTOs.
 */
@Service
public class GoogleBooksService {

    private static final String GOOGLE_BOOKS_BASE_URL = "https://www.googleapis.com/books/v1/volumes";
    private static final Pattern NON_ISBN_CHARACTERS = Pattern.compile("[^0-9Xx]");

    private final RestTemplate restTemplate;
    private final String apiKey;

    public GoogleBooksService(RestTemplateBuilder restTemplateBuilder,
                              @Value("${google.books.api.key:}") String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        this.restTemplate = restTemplateBuilder.requestFactory(() -> factory).build();
        this.apiKey = apiKey;
    }

    /**
     * Searches Google Books by metadata fields or ISBN.
     *
     * <p>When {@code isbn} is provided, the query is executed as ISBN-only mode and year
     * filtering is skipped.
     *
     * @param title optional title filter
     * @param author optional author filter
     * @param publisher optional publisher filter
     * @param year optional publication year filter
     * @param isbn optional normalized ISBN value
     * @return mapped search results
     * @throws ResponseStatusException with 503 when the Google Books API is unreachable
     */
    public List<BookSearchResult> searchBooks(String title,
                                              String author,
                                              String publisher,
                                              String year,
                                              String isbn) {
        String query = buildQuery(title, author, publisher, isbn);
        String urlTemplate = GOOGLE_BOOKS_BASE_URL + "?q={query}&maxResults=20";
        Optional<String> maybeApiKey = Optional.ofNullable(apiKey).filter(k -> !k.isBlank());

        JsonNode response;
        try {
            if (maybeApiKey.isPresent()) {
                urlTemplate += "&key={apiKey}";
                response = restTemplate.getForObject(urlTemplate, JsonNode.class, query, maybeApiKey.get());
            } else {
                response = restTemplate.getForObject(urlTemplate, JsonNode.class, query);
            }
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Book search is temporarily unavailable. Please try again later.", ex);
        }

        JsonNode items = response == null ? null : response.get("items");
        if (items == null || !items.isArray()) {
            return Collections.emptyList();
        }

        List<BookSearchResult> results = new ArrayList<>();
        boolean filterByYear = year != null && !year.isBlank() && (isbn == null || isbn.isBlank());
        for (JsonNode item : items) {
            JsonNode volumeInfo = item.path("volumeInfo");
            String publishedDate = volumeInfo.path("publishedDate").asText(null);

            if (filterByYear && (publishedDate == null || !publishedDate.startsWith(year))) {
                continue;
            }

            results.add(BookSearchResult.builder()
                    .googleBookId(item.path("id").asText(null))
                    .isbn(extractIsbn(volumeInfo.path("industryIdentifiers")))
                    .title(volumeInfo.path("title").asText(null))
                    .authors(extractAuthors(volumeInfo.path("authors")))
                    .coverImageUrl(volumeInfo.path("imageLinks").path("thumbnail").asText(null))
                    .publisher(volumeInfo.path("publisher").asText(null))
                    .publishedDate(publishedDate)
                    .build());
        }

        return results;
    }

    /**
     * Fetches the page count for a Google Books volume id with graceful fallback on API outages.
     *
     * <p>This method implements resilience to Google Books API unavailability:
     * <ul>
     *   <li>Returns null if googleBookId is blank or null (cannot proceed)</li>
     *   <li>Returns null if the API returns no pageCount field (book metadata unavailable)</li>
     *   <li>Returns null if the API is unreachable (timeout, 502/503, network error)
     *       without throwing, allowing caller to apply fallback estimates</li>
     * </ul>
     *
     * @param googleBookId Google volume id
     * @return page count when available, otherwise null (includes null on API unavailability)
     */
    public Integer getPageCount(String googleBookId) {
        if (googleBookId == null || googleBookId.isBlank()) {
            return null;
        }

        String urlTemplate = GOOGLE_BOOKS_BASE_URL + "/{googleBookId}";
        JsonNode response;
        Optional<String> maybeApiKey = Optional.ofNullable(apiKey).filter(k -> !k.isBlank());
        try {
            if (maybeApiKey.isPresent()) {
                urlTemplate += "?key={apiKey}";
                response = restTemplate.getForObject(urlTemplate, JsonNode.class, googleBookId, maybeApiKey.get());
            } else {
                response = restTemplate.getForObject(urlTemplate, JsonNode.class, googleBookId);
            }
        } catch (RestClientException ex) {
            // API is unavailable or unreachable (timeout, 502/503, network error)
            // Return null to allow caller to apply fallback estimates instead of failing hard
            return null;
        }

        JsonNode pageCountNode = response == null ? null : response.path("volumeInfo").path("pageCount");
        if (pageCountNode == null || !pageCountNode.isNumber()) {
            return null;
        }

        int pageCount = pageCountNode.asInt();
        return pageCount > 0 ? pageCount : null;
    }

    private String buildQuery(String title, String author, String publisher, String isbn) {
        if (isbn != null && !isbn.isBlank()) {
            return "isbn:" + isbn.trim();
        }

        List<String> parts = new ArrayList<>();
        if (title != null && !title.isBlank()) parts.add("intitle:" + title.trim());
        if (author != null && !author.isBlank()) parts.add("inauthor:" + author.trim());
        if (publisher != null && !publisher.isBlank()) parts.add("inpublisher:" + publisher.trim());
        return String.join("+", parts);
    }

    private String extractIsbn(JsonNode identifiersNode) {
        if (identifiersNode == null || !identifiersNode.isArray()) {
            return null;
        }

        String isbn10 = null;
        for (JsonNode identifierNode : identifiersNode) {
            String type = identifierNode.path("type").asText("");
            String identifier = normalizeIdentifier(identifierNode.path("identifier").asText(null));
            if (identifier == null) {
                continue;
            }

            if ("ISBN_13".equalsIgnoreCase(type)) {
                return identifier;
            }

            if ("ISBN_10".equalsIgnoreCase(type) && isbn10 == null) {
                isbn10 = identifier;
            }
        }

        return isbn10;
    }

    private String normalizeIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }

        String cleaned = NON_ISBN_CHARACTERS.matcher(identifier).replaceAll("").toUpperCase();
        if (cleaned.length() == 10 || cleaned.length() == 13) {
            return cleaned;
        }

        return null;
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
