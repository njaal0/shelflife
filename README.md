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

### Book Create/Update Contract

- `POST /api/books` (create) requires:
   - `title` (non-blank)
   - `shelf` (`reading`, `finished`, `want-to-read`)
- `PUT /api/books/{id}` (update) is a partial update endpoint:
   - all request fields are optional
   - supported mutable fields: `isbn`, `shelf`, `rating`, `notes`, `startedAt`, `finishedAt`
- `rating` must be between `1` and `6` when provided.

### Error Codes

All API error responses include a `code` field indicating the error type for deterministic frontend handling. Common error codes:

#### Validation Errors (400)
- `VALIDATION_ERROR` – Request body validation failed (e.g., required field missing, invalid format)
- `INVALID_PAGINATION_PARAMS` – Page or size parameter out of valid range (page >= 0, 0 < size <= 100)
- `INVALID_SHELF` – Shelf name not in [`reading`, `finished`, `want-to-read`]
- `INVALID_RATING` – Rating not in range 1–6
- `INVALID_ISBN` – ISBN format invalid or normalization failed
- `INVALID_DATE_RANGE` – Date range filter invalid (e.g., `from` after `to`)

#### Not Found Errors (404)
- `USER_NOT_FOUND` – Authenticated user not found (indicates deleted user or DB issue)
- `BOOK_NOT_FOUND` – Requested book entry not found for authenticated user
- `READING_TEST_NOT_FOUND` – Requested reading test not found for authenticated user

#### Conflict Errors (409)
- `BOOK_ALREADY_ON_SHELF` – Book (by ISBN or Google Books ID) already saved on user's shelf
- `ACTIVE_READING_TEST_EXISTS` – User already has an in-progress reading test
- `INVALID_READING_TEST_STATE` – Reading test is not in expected state (e.g., not yet completed)

#### Service/Upstream Errors (502, 503)
- `GOOGLE_BOOKS_UNAVAILABLE` – Google Books API unreachable; check page counts/estimates may be unavailable

**Error Response Format:**
```json
{
  "code": "INVALID_PAGINATION_PARAMS",
  "message": "Page index must be >= 0, got -1",
  "timestamp": "2026-05-16T10:30:00"
}
```

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
│   ├── BookResponse.java
│   ├── BookSearchResult.java
│   ├── ErrorResponse.java
│   ├── PagedResponse.java
│   ├── ReadingTestBookPlanResponse.java
│   ├── ReadingTestCompletionRequest.java
│   ├── ReadingTestDailyPlanRequest.java
│   ├── ReadingTestResponse.java
│   ├── UserResponse.java
│   └── UserUpdateRequest.java
└── ShelfLifeApplication.java
```

## Local Development Setup

### Prerequisites

- Java 17+
- Maven 3.8+
- MongoDB (local instance or MongoDB Atlas connection string)

### Quickstart

1. **Set the MongoDB connection string** (the only required environment variable in local mode):
   ```bash
   export MONGODB_URI=mongodb://localhost:27017/shelflife
   ```

2. **Start with the `local` profile** to bypass Firebase authentication:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
   The `application-local.yml` profile sets `firebase.auth.enabled=false` and allows CORS from `http://localhost:3000`.

3. The application starts on `http://localhost:8080`.

### Authentication in local mode

When running under the `local` profile (`firebase.auth.enabled=false`), any non-blank bearer token is accepted and used directly as the principal/user-id. This allows easy manual testing without a real Firebase account:

```bash
# Use any string as a bearer token — it becomes the user ID
curl -H "Authorization: Bearer local-test-user" http://localhost:8080/api/users/me
```

| User ID              | Token to pass              |
|----------------------|----------------------------|
| `local-test-user`    | `Bearer local-test-user`   |
| `alice`              | `Bearer alice`             |
| `bob`                | `Bearer bob`               |

### Optional environment variables

| Variable                  | Default                         | Description                              |
|---------------------------|---------------------------------|------------------------------------------|
| `MONGODB_URI`             | `mongodb://localhost:27017/shelflife` | MongoDB connection string           |
| `GOOGLE_BOOKS_API_KEY`    | _(empty — unauthenticated mode)_ | Google Books API key for higher quota   |

### API documentation (Swagger UI)

When the application is running, interactive API docs are available at:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### Running tests

```bash
mvn test
```
