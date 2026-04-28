package com.shelflife.controller;

import com.shelflife.dto.BookSearchResult;
import com.shelflife.model.BookEntry;
import com.shelflife.service.BookService;
import com.shelflife.service.GoogleBooksService;
import com.shelflife.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SearchController.class, BookController.class, UserController.class})
@Import(com.shelflife.config.SecurityConfig.class)
@ActiveProfiles("local")
@TestPropertySource(properties = "firebase.auth.enabled=false")
class ApiAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleBooksService googleBooksService;

    @MockBean
    private BookService bookService;

    @MockBean
    private UserService userService;

    @Test
    void search_shouldAllowUnauthenticatedRequestWithValidIsbn() throws Exception {
        when(googleBooksService.searchBooks(null, null, null, null, "9780134685991"))
                .thenReturn(List.of(BookSearchResult.builder().googleBookId("g-1").build()));

        mockMvc.perform(get("/api/books/search")
                        .param("isbn", "978-0134685991"))
                .andExpect(status().isOk());
    }

    @Test
    void search_shouldReturnBadRequestWhenNoParametersProvided() throws Exception {
        mockMvc.perform(get("/api/books/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_shouldReturnBadRequestForInvalidIsbn() throws Exception {
        mockMvc.perform(get("/api/books/search")
                        .param("isbn", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoints_shouldRejectMissingAuthorizationAcrossHttpMethods() throws Exception {
        assertUnauthorized(get("/api/shelves"), "Missing Authorization header");
        assertUnauthorized(get("/api/shelves/reading"), "Missing Authorization header");
        assertUnauthorized(get("/api/books/any-id"), "Missing Authorization header");
        assertUnauthorized(post("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"), "Missing Authorization header");
        assertUnauthorized(put("/api/books/any-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"), "Missing Authorization header");
        assertUnauthorized(delete("/api/books/any-id"), "Missing Authorization header");
        assertUnauthorized(get("/api/users/me"), "Missing Authorization header");
        assertUnauthorized(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"), "Missing Authorization header");
        assertUnauthorized(delete("/api/users/me"), "Missing Authorization header");
    }

    @Test
    void protectedEndpoints_shouldRejectMalformedAuthorizationHeader() throws Exception {
        assertUnauthorized(get("/api/shelves")
                .header("Authorization", "Token not-a-bearer"), "Invalid Authorization header");
    }

    @Test
    void protectedEndpoints_shouldRejectEmptyBearerToken() throws Exception {
        assertUnauthorized(get("/api/shelves")
                .header("Authorization", "Bearer   "), "Missing bearer token");
    }

    @Test
    void protectedEndpoints_shouldAllowAuthenticatedRequestInLocalMode() throws Exception {
        when(bookService.getBooksForUser("user-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/shelves")
                        .header("Authorization", "Bearer user-1"))
                .andExpect(status().isOk());

        verify(userService).ensureUserExists("user-1", null, null);
        verify(bookService).getBooksForUser("user-1");
    }

    @Test
    void bookEndpoint_shouldEnforceUserScopedAccess() throws Exception {
        BookEntry ownerBook = BookEntry.builder()
                .id("book-1")
                .userId("owner-user")
                .title("Owned Book")
                .shelf("reading")
                .build();

        when(bookService.getBookForUser("book-1", "owner-user")).thenReturn(ownerBook);
        when(bookService.getBookForUser("book-1", "other-user"))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Book entry not found"));

        mockMvc.perform(get("/api/books/book-1")
                        .header("Authorization", "Bearer owner-user"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/books/book-1")
                        .header("Authorization", "Bearer other-user"))
                .andExpect(status().isNotFound());

        verify(bookService).getBookForUser("book-1", "owner-user");
        verify(bookService).getBookForUser("book-1", "other-user");
    }

    @Test
    void bookUpdateEndpoint_shouldEnforceUserScopedAccess() throws Exception {
        BookEntry updatedBook = BookEntry.builder()
                .id("book-1")
                .userId("owner-user")
                .title("Updated Book")
                .shelf("finished")
                .build();

        when(bookService.updateBook(eq("book-1"), eq("owner-user"), any())).thenReturn(updatedBook);
        when(bookService.updateBook(eq("book-1"), eq("other-user"), any()))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Book entry not found"));

        mockMvc.perform(put("/api/books/book-1")
                        .header("Authorization", "Bearer owner-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shelf\":\"finished\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/books/book-1")
                        .header("Authorization", "Bearer other-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shelf\":\"finished\"}"))
                .andExpect(status().isNotFound());

        verify(bookService).updateBook(eq("book-1"), eq("owner-user"), any());
        verify(bookService).updateBook(eq("book-1"), eq("other-user"), any());
    }

    @Test
    void bookDeleteEndpoint_shouldEnforceUserScopedAccess() throws Exception {
        doThrow(new ResponseStatusException(NOT_FOUND, "Book entry not found"))
                .when(bookService).deleteBook("book-1", "other-user");

        mockMvc.perform(delete("/api/books/book-1")
                        .header("Authorization", "Bearer owner-user"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/books/book-1")
                        .header("Authorization", "Bearer other-user"))
                .andExpect(status().isNotFound());

        verify(bookService).deleteBook("book-1", "owner-user");
        verify(bookService).deleteBook("book-1", "other-user");
    }

    private void assertUnauthorized(MockHttpServletRequestBuilder requestBuilder,
                                    String expectedMessage) throws Exception {
        mockMvc.perform(requestBuilder)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }
}
