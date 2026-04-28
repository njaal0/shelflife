package com.shelflife.controller;

import com.shelflife.dto.BookSearchResult;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {SearchController.class, BookController.class, UserController.class})
@Import(com.shelflife.config.SecurityConfig.class)
@ActiveProfiles("local")
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
    void protectedEndpoints_shouldRejectUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/shelves"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/books/any-id"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
