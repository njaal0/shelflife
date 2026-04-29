# ShelfLife – Personal Bookshelf Spring Boot Application

ShelfLife is a Spring Boot backend for a personal bookshelf and reading planner app. Search for books, organize your reading across three shelves (want to read, reading, have read), run a reading speed calibration, plan completion durations for selected books, and maintain your personal reading profile.

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
- **Reading Speed Planner**: Start a reading test, submit timed sample reading duration, and estimate total reading time for one or more saved books
- **User Profiles**: Authenticated user accounts with profile management
- **Reading Notes**: Add notes and ratings to tracked books

## Reading Test Notes

- Reading test backend implementation notes: [docs/reading-test-backend-design.md](docs/reading-test-backend-design.md)

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
| `DELETE` | `/api/users/me` | Delete authenticated user's account, books, and reading tests |
| `POST` | `/api/reading-tests/start` | Start a user-scoped reading calibration test |
| `POST` | `/api/reading-tests/{testId}/complete` | Submit sample reading duration and selected book IDs |
| `POST` | `/api/reading-tests/{testId}/daily-plan` | Submit daily reading minutes to compute completion days |
| `GET` | `/api/reading-tests/{testId}` | Get one user-scoped reading test |
| `GET` | `/api/reading-tests` | List user-scoped reading tests (optional filters: `status`, `from`, `to`) |

### Reading Test Flow

1. Start a test (`POST /api/reading-tests/start`) to receive prompt text.
2. Complete the test (`POST /api/reading-tests/{testId}/complete`) with:
   - `sampleReadSeconds`
   - `bookEntryIds` (one or more user-owned saved books)
3. Optionally apply daily plan (`POST /api/reading-tests/{testId}/daily-plan`) with:
   - `dailyReadingMinutes`

The complete/daily-plan responses include:
- Calibrated words per minute
- Per-book estimates (page count, hours, days)
- Aggregate estimates (`totalEstimatedHours`, `totalEstimatedDays`)
- Explicit aggregate duration breakdown (`totalEstimatedDurationDays`, `totalEstimatedDurationHours`)
- Daily-plan completion estimate (`totalEstimatedDaysAtDailyReading`)

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
│   ├── ReadingTestIndexInitializer.java
│   ├── SecurityConfig.java
│   └── UserEmailIndexInitializer.java
├── filter/
│   └── FirebaseAuthFilter.java
├── controller/
│   ├── BookController.java
│   ├── ReadingTestController.java
│   ├── SearchController.java
│   └── UserController.java
├── model/
│   ├── BookEntry.java
│   ├── ReadingTest.java
│   ├── ReadingTestStatus.java
│   └── User.java
├── repository/
│   ├── BookEntryRepository.java
│   ├── ReadingTestRepository.java
│   └── UserRepository.java
├── service/
│   ├── BookService.java
│   ├── GoogleBooksService.java
│   ├── ReadingTestService.java
│   └── UserService.java
├── dto/
│   ├── BookRequest.java
│   ├── BookSearchResult.java
│   ├── ReadingTestBookPlanResponse.java
│   ├── ReadingTestCompletionRequest.java
│   ├── ReadingTestDailyPlanRequest.java
│   └── ReadingTestResponse.java
└── ShelfLifeApplication.java
```
