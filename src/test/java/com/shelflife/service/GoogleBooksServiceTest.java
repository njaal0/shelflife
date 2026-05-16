package com.shelflife.service;

import com.shelflife.dto.BookSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleBooksServiceTest {

    @Test
    void searchBooks_shouldUseIsbnQueryAndPreferIsbn13InResponse() {
        GoogleBooksService service = new GoogleBooksService(new RestTemplateBuilder(), "");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        String response = """
                {
                  "items": [
                    {
                      "id": "book-1",
                      "volumeInfo": {
                        "title": "Effective Java",
                        "authors": ["Joshua Bloch"],
                        "publisher": "Addison-Wesley",
                        "publishedDate": "2018-01-06",
                        "industryIdentifiers": [
                          {"type": "ISBN_10", "identifier": "0134685997"},
                          {"type": "ISBN_13", "identifier": "978-0134685991"}
                        ]
                      }
                    }
                  ]
                }
                """;

        server.expect(requestTo(containsString("q=isbn:9780134685991")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        List<BookSearchResult> results = service.searchBooks(
                "Ignored title",
                "Ignored author",
                "Ignored publisher",
                "1999",
                "9780134685991"
        );

        assertEquals(1, results.size());
        assertEquals("book-1", results.get(0).getGoogleBookId());
        assertEquals("9780134685991", results.get(0).getIsbn());
        assertEquals("2018-01-06", results.get(0).getPublishedDate());
        server.verify();
    }

    @Test
    void searchBooks_shouldFallbackToIsbn10WhenIsbn13Missing() {
        GoogleBooksService service = new GoogleBooksService(new RestTemplateBuilder(), "");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        String response = """
                {
                  "items": [
                    {
                      "id": "book-2",
                      "volumeInfo": {
                        "title": "Clean Code",
                        "authors": ["Robert C. Martin"],
                        "publisher": "Prentice Hall",
                        "publishedDate": "2008-08-01",
                        "industryIdentifiers": [
                          {"type": "ISBN_10", "identifier": "0-13-235088-2"}
                        ]
                      }
                    }
                  ]
                }
                """;

        server.expect(requestTo(containsString("q=intitle:Clean%20Code")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        List<BookSearchResult> results = service.searchBooks("Clean Code", null, null, "2008", null);

        assertEquals(1, results.size());
        assertEquals("0132350882", results.get(0).getIsbn());
        server.verify();
    }

    @Test
    void searchBooks_shouldReturn503WhenGoogleBooksApiIsUnreachable() {
        GoogleBooksService service = new GoogleBooksService(new RestTemplateBuilder(), "");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(requestTo(containsString("q=intitle:Some%20Book")))
                .andRespond(withServerError());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.searchBooks("Some Book", null, null, null, null));

        assertEquals(503, ex.getStatusCode().value());
        assertEquals("Book search is temporarily unavailable. Please try again later.", ex.getReason());
        server.verify();
    }

    @Test
    void getPageCount_shouldReturnPageCountWhenApiSucceeds() {
        GoogleBooksService service = new GoogleBooksService(new RestTemplateBuilder(), "");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        String response = """
                {
                  "volumeInfo": {
                    "title": "Clean Code",
                    "pageCount": 464
                  }
                }
                """;

        server.expect(requestTo(containsString("/volumes/vol-abc")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        Integer result = service.getPageCount("vol-abc");

        assertEquals(464, result);
        server.verify();
    }

    @Test
    void getPageCount_shouldReturnNullWhenApiIsUnreachable() {
        GoogleBooksService service = new GoogleBooksService(new RestTemplateBuilder(), "");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        // Server error simulates unreachable API (5xx or network failure mapped by RestTemplate)
        server.expect(requestTo(containsString("/volumes/vol-unreachable")))
                .andRespond(withServerError());

        // Phase 4: must not throw — caller applies fallback estimate instead
        Integer result = service.getPageCount("vol-unreachable");

        assertNull(result);
        server.verify();
    }

    @Test
    void getPageCount_shouldReturnNullWhenPageCountFieldMissingFromResponse() {
        GoogleBooksService service = new GoogleBooksService(new RestTemplateBuilder(), "");
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        // Google Books returns volume info but no pageCount field
        String response = """
                {
                  "volumeInfo": {
                    "title": "No Page Count Book"
                  }
                }
                """;

        server.expect(requestTo(containsString("/volumes/vol-no-pages")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        Integer result = service.getPageCount("vol-no-pages");

        assertNull(result);
        server.verify();
    }
}
