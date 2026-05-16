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

## Documentation

- Frontend handoff package (endpoint matrix, shapes, samples, error codes, env vars): [docs/frontend-handoff.md](docs/frontend-handoff.md)
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

**Fallback Page Count Estimates:**

When Google Books API is unavailable or doesn't have page count data for a book, ShelfLife applies a fallback estimate of **300 pages** (typical novel length). The backend returns `isPageCountEstimate: true` in the response for each book plan:

```json
{
  "bookPlanSnapshots": [
    {
      "bookEntryId": "61abcdef...",
      "title": "The Great Gatsby",
      "pageCount": 300,
      "isPageCountEstimate": true,
      "estimatedHours": 16.67,
      "estimatedDays": 0.69,
      "estimatedDaysAtDailyReading": 27.78
    }
  ]
}
```

Your frontend should:
- Display a visual indicator (e.g., "~16.67 hours estimated based on typical book length") when `isPageCountEstimate` is true
- Show actual page count when `isPageCountEstimate` is false
- Allow users to manually override the page count if the estimate seems inaccurate
- Refresh the plan later when Google Books becomes available to get the actual page count

**When Google Books API is Unavailable (reading test complete):**

`POST /api/reading-tests/{testId}/complete` always returns HTTP **200** regardless of Google Books availability. When the API is unreachable or lacks page count data for a book, the backend applies the 300-page fallback estimate automatically and marks the affected books with `isPageCountEstimate: true`.

Your frontend should:
- Check `isPageCountEstimate` per book plan and show a UI indicator, e.g.: "~16.67 hours (estimated — page count unavailable)"
- Inform users they can retry the reading test later once Google Books is restored, to get precise page counts

**Handling Google Books API Outages (book search):**

`GET /api/search` **does** fail hard when Google Books is unreachable — it returns HTTP **503** with error code `GOOGLE_BOOKS_UNAVAILABLE`. Implement exponential backoff retry (1s, 2s, 4s delays) for search requests, and show a user-facing message like "Book search is temporarily unavailable. Please try again shortly."

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

#### Service/Upstream Errors (503)
- `GOOGLE_BOOKS_UNAVAILABLE` – Google Books API unreachable during book search (`GET /api/search`). Reading test completion is unaffected — page count fallback estimates are applied automatically.

**Error Response Format:**
```json
{
  "code": "INVALID_PAGINATION_PARAMS",
  "message": "Page index must be >= 0, got -1",
  "timestamp": "2026-05-16T10:30:00"
}
```

### Authentication

ShelfLife supports two authentication modes depending on deployment environment:

#### Production Mode: Firebase Authentication
- **Enabled by**: `firebase.auth.enabled=true` (default outside local profile)
- **Token source**: Firebase ID tokens from your frontend
- **Validation**: Tokens are verified against Firebase Admin SDK using the service account
- **Token format**: ID token (JWT) obtained from Firebase Authentication

**How frontend obtains a token:**
1. User signs in via Firebase Authentication SDK in frontend
2. Frontend calls `getIdToken()` on the authenticated user:
   ```javascript
   const user = auth.currentUser;
   const token = await user.getIdToken();
   ```
3. Frontend includes token in Authorization header:
   ```javascript
   headers: {
     'Authorization': `Bearer ${token}`
   }
   ```

**Token lifecycle:**
- ID tokens expire after **1 hour** (default Firebase setting)
- Frontend should refresh token before expiration:
  ```javascript
  // Firebase SDK handles automatic refresh
  const refreshedToken = await user.getIdToken(true); // Force refresh
  ```
- Expired tokens result in `401 Unauthorized` response from backend

#### Local Development Mode: Any Bearer Token
- **Enabled by**: `spring.profiles.active=local` with `firebase.auth.enabled=false` (configured in `application-local.yml`)
- **Token source**: Any non-blank string becomes the user ID
- **Validation**: Disabled — useful for manual testing and frontend development
- **Example**: `Bearer alice` creates a user with ID `alice`

**Usage in local mode:**
```bash
# Any string after "Bearer " becomes the user ID
curl -H "Authorization: Bearer alice" http://localhost:8080/api/users/me
```

#### Protected Endpoints

All `/api/**` endpoints except `GET /api/books/search` require valid authentication:
- Missing Authorization header → `401 Unauthorized` with code `UNAUTHORIZED`
- Invalid/expired token → `401 Unauthorized` with code `UNAUTHORIZED`
- Malformed header → `401 Unauthorized` with code `UNAUTHORIZED`

## Production Deployment Configuration

### Required Environment Variables

The following environment variables must be explicitly set outside the `local` profile:

| Variable                            | Description                                                           | Example                           |
|-------------------------------------|-----------------------------------------------------------------------|-----------------------------------|
| `MONGODB_URI`                       | MongoDB Atlas connection string (required)                            | `mongodb+srv://user:pwd@cluster.mongodb.net/shelflife?...` |
| `FIREBASE_AUTH_ENABLED`             | Enable Firebase token validation (must be `true`)                     | `true`                            |
| `FIREBASE_PROJECT_ID`               | Firebase project identifier (required)                                | `shelflife-968cf`                 |
| `FIREBASE_SERVICE_ACCOUNT_PATH`     | Path to Firebase service account JSON file (required)                 | `file:/etc/secrets/firebase-sa.json` |
| `GOOGLE_BOOKS_API_KEY`              | Google Books API key for authenticated requests (recommended)        | `AIza...`                         |
| `APP_CORS_ALLOWED_ORIGINS`          | Comma-separated list of allowed frontend origins (required)          | `https://myapp.com,https://staging.myapp.com` |

### CORS Configuration

**CORS (Cross-Origin Resource Sharing)** is required for frontend applications to communicate with the backend.

#### Local Development
Configured in `application-local.yml` to allow localhost development:
```yaml
app:
  cors:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:3001
      - http://127.0.0.1:3000
```

#### Production Deployment
Set `APP_CORS_ALLOWED_ORIGINS` to exact frontend URLs. Examples:
- Single origin: `https://shelflife.example.com`
- Multiple origins: `https://shelflife.example.com,https://staging.shelflife.example.com`

**Important CORS Rules:**
- Wildcards (`*`) are **not allowed** when credentials are enabled (security requirement)
- Localhost origins are **not allowed** outside local profile (security requirement)
- Origins must be exact matches (scheme + domain + port)
- Trailing slashes are normalized (both `https://example.com` and `https://example.com/` match)

**CORS Validation Errors:**
If CORS origins are misconfigured, the application fails to start with error:
```
IllegalStateException: app.cors.allowed-origins must contain at least one origin
```

### Startup Validation

The application validates configuration at startup and fails fast if issues are detected:

| Condition | Error Message |
|-----------|---------------|
| Outside `local` profile with `firebase.auth.enabled=false` | `firebase.auth.enabled must be true outside local profile` |
| Missing `FIREBASE_PROJECT_ID` | `firebase.auth.project-id must be configured outside local profile` |
| Missing `FIREBASE_SERVICE_ACCOUNT_PATH` | `firebase.auth.service-account-path must be configured outside local profile` |
| Missing `APP_CORS_ALLOWED_ORIGINS` | `app.cors.allowed-origins must contain at least one origin` |
| Wildcard in CORS origins | `app.cors.allowed-origins must not contain wildcard '*'` |
| Localhost in CORS origins | `app.cors.allowed-origins must not contain localhost origins outside local profile` |
| Mixed `local` + non-local profiles | `The local profile must not be combined with non-local profiles` |

**Pre-deployment checklist:**
- [ ] `MONGODB_URI` set to production MongoDB Atlas cluster
- [ ] `FIREBASE_AUTH_ENABLED=true`
- [ ] `FIREBASE_PROJECT_ID` matches Firebase console project
- [ ] `FIREBASE_SERVICE_ACCOUNT_PATH` points to service account JSON (not committed to repo)
- [ ] `GOOGLE_BOOKS_API_KEY` set (if higher quota needed)
- [ ] `APP_CORS_ALLOWED_ORIGINS` set to production frontend URL(s) only
- [ ] No `spring.profiles.active=local` in production environment
- [ ] Application starts without IllegalStateException errors



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

1. **Clone and navigate to project:**
   ```bash
   cd shelflife
   ```

2. **Ensure MongoDB is running** (local instance or configure `MONGODB_URI`):
   ```bash
   # If using Docker:
   docker run -d -p 27017:27017 mongo:latest
   
   # Or MongoDB Atlas:
   export MONGODB_URI="mongodb+srv://username:password@cluster.mongodb.net/shelflife?..."
   ```

3. **Start with the local profile** to bypass Firebase authentication:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
   - Automatically loads `application-local.yml`
   - Sets `firebase.auth.enabled=false`
   - Allows CORS from localhost (3000, 3001)
   - Any bearer token is accepted

4. Verify startup:
   ```bash
   curl http://localhost:8080/api/books/search?title=Dune
   # Should return search results without requiring auth
   ```

5. Test authenticated endpoint with any bearer token:
   ```bash
   curl -H "Authorization: Bearer local-dev-user" http://localhost:8080/api/users/me
   # Should create/return user with ID "local-dev-user"
   ```

### Local Development User IDs

When running in local mode, use any string as a bearer token to create test users:

| User ID                 | Usage                                              | Token                         |
|-------------------------|----|------------------------------------------------------------|
| `local-dev-user`        | General local testing                               | `Bearer local-dev-user`       |
| `alice`                 | Test user 1 (for comparing users/books)             | `Bearer alice`                |
| `bob`                   | Test user 2                                         | `Bearer bob`                  |
| `test-invalid-isbn`     | Test ISBN validation errors                         | `Bearer test-invalid-isbn`    |
| `test-google-books-key` | Test Google Books API (requires `GOOGLE_BOOKS_API_KEY` env var) | `Bearer test-google-books-key` |

### Optional Environment Variables

| Variable                  | Default                         | Description                                               |
|---------------------------|---------------------------------|-----------------------------------------------------------|
| `MONGODB_URI`             | `mongodb://localhost:27017/shelflife` | MongoDB connection string (or MongoDB Atlas)    |
| `GOOGLE_BOOKS_API_KEY`    | _(empty — unauthenticated mode)_ | Google Books API key for higher API quota (optional)    |

**Note:** In local mode, `GOOGLE_BOOKS_API_KEY` is optional. Without it, the backend uses unauthenticated Google Books API calls (lower quota). For development with many search requests, obtain a free API key from [Google Cloud Console](https://console.cloud.google.com/).

### API Documentation (Swagger UI)

When the application is running, interactive API documentation is available at:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

Use the Swagger UI to test endpoints directly without curl:
1. Navigate to `http://localhost:8080/swagger-ui.html`
2. For authenticated endpoints, click **Authorize** and enter bearer token
3. Fill request parameters and execute

### Running Tests

```bash
mvn test
```

Run specific test class:
```bash
mvn test -Dtest=BookControllerTest
```

Run with coverage report:
```bash
mvn test jacoco:report
# Coverage report: target/site/jacoco/index.html
```

### Troubleshooting Local Setup

**Port 8080 already in use:**
```bash
# Run on different port
mvn spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments="--server.port=8888"
```

**MongoDB connection refused:**
```bash
# Verify MongoDB is running
# If local: check docker container or mongod process
# If Atlas: verify MONGODB_URI connection string

# Test connection
mongo "mongodb://localhost:27017/shelflife"
```

**Firebase service account file not found:**
- Only relevant in production; local mode bypasses Firebase
- Ensure `firebase.auth.enabled=false` in active profile

**CORS errors from frontend:**
- Verify frontend origin is in CORS allowed-origins (e.g., `http://localhost:3000`)
- If using different frontend port, add to `application-local.yml`

