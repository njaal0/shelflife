package com.shelflife.controller;

import com.shelflife.dto.BookSearchResult;
import com.shelflife.service.GoogleBooksService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchControllerTest {

    @Mock
    private GoogleBooksService googleBooksService;

    @InjectMocks
    private SearchController searchController;

    @Test
    void searchBooks_shouldRejectWhenNoSearchParametersProvided() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> searchController.searchBooks(null, null, null, null, null));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("At least one search parameter is required: title, author, publisher, year, isbn", ex.getReason());
    }

    @Test
    void searchBooks_shouldRejectInvalidIsbnFormat() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> searchController.searchBooks(null, null, null, null, "123"));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Invalid isbn format. Use ISBN-10 or ISBN-13.", ex.getReason());
    }

    @Test
    void searchBooks_shouldUseIsbnOnlyModeWhenIsbnProvided() {
        List<BookSearchResult> expected = List.of(BookSearchResult.builder().googleBookId("g-1").build());
        when(googleBooksService.searchBooks(null, null, null, null, "9780134685991"))
                .thenReturn(expected);

        List<BookSearchResult> actual = searchController.searchBooks(
                "Some title",
                "Some author",
                "Some publisher",
                "1999",
                "978-0134685991"
        );

        assertEquals(expected, actual);
        verify(googleBooksService).searchBooks(null, null, null, null, "9780134685991");
        verifyNoMoreInteractions(googleBooksService);
    }

    @Test
    void searchBooks_shouldUseStandardModeWhenIsbnMissing() {
        List<BookSearchResult> expected = List.of(BookSearchResult.builder().googleBookId("g-2").build());
        when(googleBooksService.searchBooks("Clean Code", "Martin", null, "2008", null))
                .thenReturn(expected);

        List<BookSearchResult> actual = searchController.searchBooks("Clean Code", "Martin", null, "2008", null);

        assertEquals(expected, actual);
        verify(googleBooksService).searchBooks("Clean Code", "Martin", null, "2008", null);
        verifyNoMoreInteractions(googleBooksService);
    }
}
