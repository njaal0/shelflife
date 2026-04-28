# ShelfLife – Personal Bookshelf Spring Boot Application

ShelfLife is a Spring Boot backend for a personal bookshelf and reading planner app. Search for books, organize your reading across three shelves (want to read, reading, have read), plan reading progress (reading tests are a future phase), and maintain your personal reading profile.

## Tech Stack

- **Java 17**
- **Spring Boot 3.x**
- **MongoDB Atlas** via Spring Data MongoDB
- **Spring Security** with Firebase JWT filter scaffold
- **Google Books API** integration
- **Maven** build

## Features

- **Book Search**: Public Google Books search by title, author, publisher, year, or ISBN
- **Reading Shelves**: Organize books across three shelves — "Want to Read", "Reading", and "Have Read"
- **Reading Plans**: Planned for a future phase (not implemented yet)
- **User Profiles**: Authenticated user accounts with profile management
- **Reading Notes**: Add notes and ratings to tracked books

## Future Design Notes

- Reading test backend design (future phase): [docs/reading-test-backend-design.md](docs/reading-test-backend-design.md)

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/books/search` | Public Google Books search (query params: `title`, `author`, `publisher`, `year`, `isbn`) |
| `GET` | `/api/shelves` | Get all books for authenticated user |
| `GET` | `/api/shelves/{shelf}` | Get books for a specific shelf |
| `POST` | `/api/books` | Save a book to a shelf |
| `PUT` | `/api/books/{id}` | Update shelf/rating/notes/dates |
| `DELETE` | `/api/books/{id}` | Remove a saved book |
| `GET` | `/api/books/{id}` | Get a single saved book |
| `GET` | `/api/users/me` | Get authenticated user's profile |
| `PUT` | `/api/users/me` | Update authenticated user's profile |
| `DELETE` | `/api/users/me` | Delete authenticated user's account and books |

### Search Notes

- At least one search parameter is required.
- If `isbn` is provided, the backend uses ISBN-only search mode.
- ISBN accepts ISBN-10 or ISBN-13 formats (hyphens/spaces allowed).

### Authentication

- Search endpoint is public.
- Other `/api/**` endpoints require `Authorization: Bearer <token>`.
- Protected endpoints return `401 Unauthorized` for missing, malformed, empty, or invalid bearer tokens.
- Current filter supports:
   - Local development mode (`spring.profiles.active=local` and `firebase.auth.enabled=false`): token string is used as principal.
   - Firebase validation mode (`firebase.auth.enabled=true`): token validated through Firebase Admin SDK.

## Project Structure

```text
src/main/java/com/shelflife/
├── config/
│   └── SecurityConfig.java
├── filter/
│   └── FirebaseAuthFilter.java
├── controller/
│   ├── BookController.java
│   ├── SearchController.java
│   └── UserController.java
├── model/
│   ├── BookEntry.java
│   └── User.java
├── repository/
│   ├── BookEntryRepository.java
│   └── UserRepository.java
├── service/
│   ├── BookService.java
│   └── GoogleBooksService.java
├── dto/
│   ├── BookSearchResult.java
│   └── BookRequest.java
└── ShelfLifeApplication.java
```
