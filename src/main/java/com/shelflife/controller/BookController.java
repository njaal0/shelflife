package com.shelflife.controller;

import com.shelflife.dto.BookCreateRequest;
import com.shelflife.dto.BookResponse;
import com.shelflife.dto.BookUpdateRequest;
import com.shelflife.dto.PagedResponse;
import com.shelflife.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Save, browse, and manage books on reading shelves.")
@SecurityRequirement(name = "bearerAuth")
public class BookController {

    private final BookService bookService;

    @Operation(summary = "List all saved books", description = "Returns a paginated list of all books saved by the authenticated user across all shelves.")
    @ApiResponse(responseCode = "200", description = "Paginated book list")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @GetMapping("/shelves")
    public PagedResponse<BookResponse> getAllShelves(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Maximum results per page") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return bookService.getBooksForUser(getUserId(authentication), page, size);
    }

    @Operation(summary = "List books on a shelf", description = "Returns a paginated list of books on the specified shelf for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Paginated shelf list")
    @ApiResponse(responseCode = "400", description = "Unknown shelf name", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @GetMapping("/shelves/{shelf}")
    public PagedResponse<BookResponse> getShelf(
            @PathVariable String shelf,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Maximum results per page") @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        return bookService.getBooksForUserByShelf(getUserId(authentication), shelf, page, size);
    }

    @Operation(summary = "Save a book", description = "Adds a new book to the authenticated user's shelf. Title and shelf are required.")
    @ApiResponse(responseCode = "201", description = "Book created")
    @ApiResponse(responseCode = "400", description = "Validation error or invalid shelf/rating/ISBN", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "409", description = "Book already on shelf", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse createBook(@Valid @RequestBody BookCreateRequest request, Authentication authentication) {
        return bookService.createBook(getUserId(authentication), request);
    }

    @Operation(summary = "Update a saved book", description = "Applies partial updates (shelf, rating, notes, dates, ISBN) to a saved book entry. All request fields are optional.")
    @ApiResponse(responseCode = "200", description = "Book updated")
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Book not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @PutMapping("/books/{id}")
    public BookResponse updateBook(@PathVariable String id,
                                   @Valid @RequestBody BookUpdateRequest request,
                                   Authentication authentication) {
        return bookService.updateBook(id, getUserId(authentication), request);
    }

    @Operation(summary = "Delete a saved book", description = "Removes a saved book entry from the authenticated user's shelf.")
    @ApiResponse(responseCode = "204", description = "Book deleted")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Book not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @DeleteMapping("/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable String id, Authentication authentication) {
        bookService.deleteBook(id, getUserId(authentication));
    }

    @Operation(summary = "Get a saved book", description = "Returns a single saved book entry belonging to the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Book entry")
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @ApiResponse(responseCode = "404", description = "Book not found", content = @Content(schema = @Schema(ref = "#/components/schemas/ErrorResponse")))
    @GetMapping("/books/{id}")
    public BookResponse getBook(@PathVariable String id, Authentication authentication) {
        return bookService.getBookForUser(id, getUserId(authentication));
    }

    private String getUserId(Authentication authentication) {
        return authentication.getName();
    }
}
