# ShelfLife — Frontend Handoff Package

This document is the authoritative reference for the ShelfLife REST API. It is generated from
the live codebase at the end of Phase 5 and reflects all contracts locked in Phases 1–4.

---

## Table of Contents

1. [Base URL & Versioning](#1-base-url--versioning)
2. [Authentication](#2-authentication)
3. [Error Response Contract](#3-error-response-contract)
4. [Pagination Contract](#4-pagination-contract)
5. [Endpoint Matrix](#5-endpoint-matrix)
6. [Request / Response Shapes](#6-request--response-shapes)
   - [Users](#61-users)
   - [Books](#62-books)
   - [Book Search](#63-book-search)
   - [Reading Tests](#64-reading-tests)
7. [Sample Requests & Responses](#7-sample-requests--responses)
   - [User Flow](#user-flow)
   - [Book Shelf Flow](#book-shelf-flow)
   - [Reading Test Flow](#reading-test-flow)
8. [Error Code Reference](#8-error-code-reference)
9. [Environment Variable Reference](#9-environment-variable-reference)
10. [API Versioning Strategy](#10-api-versioning-strategy)

---

## 1. Base URL & Versioning

| Environment  | Base URL                    |
|--------------|-----------------------------|
| Local dev    | `http://localhost:8080/api` |
| Production   | `https://<your-domain>/api` |

There is currently **no version prefix** in the URL path (e.g. no `/v1/`). See
[Section 10](#10-api-versioning-strategy) for the versioning strategy and migration plan
before introducing breaking changes.

---

## 2. Authentication

All endpoints except `GET /api/books/search` require a valid bearer token in the
`Authorization` header:

```
Authorization: Bearer <token>
```

### Production — Firebase ID Tokens

Obtain a Firebase ID token from the Firebase SDK after the user signs in:

```js
const token = await firebase.auth().currentUser.getIdToken();
// Token expires after 1 hour; refresh automatically:
const freshToken = await firebase.auth().currentUser.getIdToken(/* forceRefresh */ true);
```

### Local Development — Any Bearer Token

When the server is started with `--spring.profiles.active=local`, Firebase validation is
disabled. Any non-blank string is accepted as the bearer token and used directly as the
`userId`. Use a fixed string such as `local-dev-user` for deterministic test data.

### Auth Error Responses

| Condition                        | HTTP | Code (body) |
|----------------------------------|------|-------------|
| No `Authorization` header        | 401  | _(empty)_   |
| Malformed or expired token       | 401  | _(empty)_   |
| Token valid but user not in DB   | 404  | `USER_NOT_FOUND` |

---

## 3. Error Response Contract

All error responses share a single envelope:

```json
{
  "code": "BOOK_NOT_FOUND",
  "message": "Book entry not found for the current user",
  "timestamp": "2026-05-16T10:30:00",
  "validationErrors": null
}
```

For `VALIDATION_ERROR` (400), `validationErrors` is a map of field name → message:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "timestamp": "2026-05-16T10:30:00",
  "validationErrors": {
    "title": "Title is required",
    "shelf": "Shelf is required"
  }
}
```

`validationErrors` is `null` for all non-validation errors.

---

## 4. Pagination Contract

Paginated endpoints accept the following query parameters:

| Parameter | Type    | Default | Constraint         |
|-----------|---------|---------|--------------------|
| `page`    | integer | `0`     | `>= 0`             |
| `size`    | integer | `20`    | `>= 1`, `<= 100`  |

All paginated responses share this envelope:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3
}
```

Out-of-range `page` values (beyond available data) return an empty `content` array with
`totalElements` and `totalPages` reflecting the full dataset — not an error.

---

## 5. Endpoint Matrix

### Users

| Method   | Path             | Auth | Request Body        | Success | Description                              |
|----------|------------------|------|---------------------|---------|------------------------------------------|
| `GET`    | `/users/me`      | ✅   | —                   | 200     | Get authenticated user's profile         |
| `PUT`    | `/users/me`      | ✅   | `UserUpdateRequest` | 200     | Update email and/or display name         |
| `DELETE` | `/users/me`      | ✅   | —                   | 204     | Delete account, all books, all tests     |

### Books

| Method   | Path                   | Auth | Request Body        | Success | Description                          |
|----------|------------------------|------|---------------------|---------|--------------------------------------|
| `GET`    | `/shelves`             | ✅   | —                   | 200     | List all saved books (all shelves)   |
| `GET`    | `/shelves/{shelf}`     | ✅   | —                   | 200     | List books on a specific shelf       |
| `POST`   | `/books`               | ✅   | `BookCreateRequest` | 201     | Save a new book                      |
| `GET`    | `/books/{id}`          | ✅   | —                   | 200     | Get a single saved book              |
| `PUT`    | `/books/{id}`          | ✅   | `BookUpdateRequest` | 200     | Partially update a saved book        |
| `DELETE` | `/books/{id}`          | ✅   | —                   | 204     | Remove a saved book                  |

**Path values for `{shelf}`:** `reading`, `finished`, `want-to-read`

### Book Search

| Method | Path             | Auth | Request Body | Success | Description                         |
|--------|------------------|------|--------------|---------|-------------------------------------|
| `GET`  | `/books/search`  | ❌   | —            | 200     | Search Google Books by metadata/ISBN |

### Reading Tests

| Method | Path                            | Auth | Request Body                    | Success | Description                                  |
|--------|---------------------------------|------|---------------------------------|---------|----------------------------------------------|
| `POST` | `/reading-tests/start`          | ✅   | —                               | 201     | Start a new calibration test                 |
| `POST` | `/reading-tests/{testId}/complete`   | ✅   | `ReadingTestCompletionRequest`  | 200     | Submit reading duration + book IDs           |
| `POST` | `/reading-tests/{testId}/daily-plan` | ✅   | `ReadingTestDailyPlanRequest`   | 200     | Apply daily reading minutes                  |
| `GET`  | `/reading-tests/{testId}`       | ✅   | —                               | 200     | Get a single reading test                    |
| `GET`  | `/reading-tests`                | ✅   | —                               | 200     | List reading tests with optional filters     |

**Query parameters for `GET /reading-tests`:**

| Parameter | Type     | Required | Description                                     |
|-----------|----------|----------|-------------------------------------------------|
| `status`  | string   | No       | Filter by status: `DRAFT`, `IN_PROGRESS`, `SUBMITTED`, `SCORED` |
| `from`    | date     | No       | Created-at lower bound, inclusive (ISO-8601: `YYYY-MM-DD`) |
| `to`      | date     | No       | Created-at upper bound, inclusive (ISO-8601: `YYYY-MM-DD`) |
| `page`    | integer  | No       | Zero-based page (default `0`)                  |
| `size`    | integer  | No       | Page size 1–100 (default `20`)                 |

---

## 6. Request / Response Shapes

### 6.1 Users

#### `UserUpdateRequest`

```json
{
  "email": "user@example.com",
  "displayName": "Jane Smith"
}
```

| Field         | Type   | Required | Constraint                         |
|---------------|--------|----------|------------------------------------|
| `email`       | string | No       | Valid email format, max 320 chars  |
| `displayName` | string | No       | Max 100 chars                      |

#### `UserResponse`

```json
{
  "id": "uid-abc123",
  "email": "user@example.com",
  "displayName": "Jane Smith",
  "totalBooks": 14,
  "createdAt": "2026-01-10T09:00:00",
  "updatedAt": "2026-05-16T10:30:00",
  "lastLoginAt": "2026-05-16T10:00:00"
}
```

| Field         | Type     | Nullable | Description                         |
|---------------|----------|----------|-------------------------------------|
| `id`          | string   | No       | Firebase UID / local-dev user ID    |
| `email`       | string   | Yes      | User email                          |
| `displayName` | string   | Yes      | Display name                        |
| `totalBooks`  | integer  | No       | Count of all saved book entries     |
| `createdAt`   | datetime | Yes      | ISO-8601 local datetime             |
| `updatedAt`   | datetime | Yes      | ISO-8601 local datetime             |
| `lastLoginAt` | datetime | Yes      | ISO-8601 local datetime             |

---

### 6.2 Books

#### `BookCreateRequest`

```json
{
  "googleBookId": "NggnmAEACAAJ",
  "isbn": "978-0134685991",
  "title": "Effective Java",
  "authors": ["Joshua Bloch"],
  "coverImageUrl": "https://books.google.com/...",
  "shelf": "want-to-read",
  "rating": null,
  "notes": null,
  "startedAt": null,
  "finishedAt": null
}
```

| Field          | Type     | Required | Constraint                                          |
|----------------|----------|----------|-----------------------------------------------------|
| `title`        | string   | **Yes**  | Non-blank, max 500 chars                            |
| `shelf`        | string   | **Yes**  | `reading`, `finished`, or `want-to-read`            |
| `googleBookId` | string   | No       | Google Books volume ID                              |
| `isbn`         | string   | No       | ISBN-10 or ISBN-13 (hyphens/spaces accepted)        |
| `authors`      | string[] | No       | List of author names                                |
| `coverImageUrl`| string   | No       | Max 2048 chars                                      |
| `rating`       | integer  | No       | 1–6 inclusive                                       |
| `notes`        | string   | No       | Max 5000 chars                                      |
| `startedAt`    | date     | No       | ISO-8601: `YYYY-MM-DD`                              |
| `finishedAt`   | date     | No       | ISO-8601: `YYYY-MM-DD`                              |

#### `BookUpdateRequest`

All fields are optional. Omitted fields are **not changed**. Send `null` to explicitly clear a field.

```json
{
  "shelf": "finished",
  "rating": 5,
  "notes": "A must-read for Java developers.",
  "finishedAt": "2026-05-16"
}
```

| Field       | Type    | Constraint                                    |
|-------------|---------|-----------------------------------------------|
| `isbn`      | string  | ISBN-10 or ISBN-13 (hyphens/spaces accepted)  |
| `shelf`     | string  | `reading`, `finished`, or `want-to-read`      |
| `rating`    | integer | 1–6 inclusive                                 |
| `notes`     | string  | Max 5000 chars                                |
| `startedAt` | date    | ISO-8601: `YYYY-MM-DD`                        |
| `finishedAt`| date    | ISO-8601: `YYYY-MM-DD`                        |

#### `BookResponse`

```json
{
  "id": "64f1a2b3c4d5e6f7a8b9c0d1",
  "userId": "uid-abc123",
  "googleBookId": "NggnmAEACAAJ",
  "isbn": "9780134685991",
  "title": "Effective Java",
  "authors": ["Joshua Bloch"],
  "coverImageUrl": "https://books.google.com/...",
  "shelf": "finished",
  "rating": 5,
  "notes": "A must-read for Java developers.",
  "startedAt": "2026-01-15",
  "finishedAt": "2026-03-22",
  "createdAt": "2026-01-10T09:00:00"
}
```

| Field          | Type     | Nullable |
|----------------|----------|----------|
| `id`           | string   | No       |
| `userId`       | string   | No       |
| `googleBookId` | string   | Yes      |
| `isbn`         | string   | Yes      |
| `title`        | string   | No       |
| `authors`      | string[] | Yes      |
| `coverImageUrl`| string   | Yes      |
| `shelf`        | string   | No       |
| `rating`       | integer  | Yes      |
| `notes`        | string   | Yes      |
| `startedAt`    | date     | Yes      |
| `finishedAt`   | date     | Yes      |
| `createdAt`    | datetime | Yes      |

---

### 6.3 Book Search

**No auth required.** At least one query parameter is required.

| Parameter   | Type   | Description                                              |
|-------------|--------|----------------------------------------------------------|
| `title`     | string | Book title (partial match)                               |
| `author`    | string | Author name (partial match)                              |
| `publisher` | string | Publisher name (partial match)                           |
| `year`      | string | 4-digit publication year                                 |
| `isbn`      | string | ISBN-10 or ISBN-13 (hyphens/spaces accepted). When supplied, activates ISBN-only mode — other parameters are ignored. |

#### `BookSearchResult` (array response)

```json
[
  {
    "googleBookId": "NggnmAEACAAJ",
    "isbn": "9780134685991",
    "title": "Effective Java",
    "authors": ["Joshua Bloch"],
    "coverImageUrl": "https://books.google.com/...",
    "publisher": "Addison-Wesley Professional",
    "publishedDate": "2018-01-06"
  }
]
```

| Field           | Type     | Nullable |
|-----------------|----------|----------|
| `googleBookId`  | string   | Yes      |
| `isbn`          | string   | Yes      |
| `title`         | string   | Yes      |
| `authors`       | string[] | Yes      |
| `coverImageUrl` | string   | Yes      |
| `publisher`     | string   | Yes      |
| `publishedDate` | string   | Yes      |

> `publishedDate` is a raw string from Google Books (e.g. `"2018"`, `"2018-01"`, or `"2018-01-06"`). Do not assume a fixed format.

---

### 6.4 Reading Tests

#### Reading Test Status Lifecycle

```
start → IN_PROGRESS → (complete) → SUBMITTED → (daily-plan) → SCORED
```

Only one `IN_PROGRESS` or `DRAFT` test may exist per user at a time.

#### `ReadingTestCompletionRequest`

```json
{
  "sampleReadSeconds": 75,
  "bookEntryIds": ["64f1a2b3c4d5e6f7a8b9c0d1", "64f1a2b3c4d5e6f7a8b9c0d2"]
}
```

| Field             | Type     | Required | Constraint                          |
|-------------------|----------|----------|-------------------------------------|
| `sampleReadSeconds` | integer | **Yes** | Positive integer                    |
| `bookEntryIds`    | string[] | **Yes**  | Non-empty, no blanks, no duplicates |

#### `ReadingTestDailyPlanRequest`

```json
{
  "dailyReadingMinutes": 45
}
```

| Field                 | Type    | Required | Constraint      |
|-----------------------|---------|----------|-----------------|
| `dailyReadingMinutes` | integer | **Yes**  | Positive integer |

#### `ReadingTestResponse`

```json
{
  "id": "65a1b2c3d4e5f6a7b8c9d0e1",
  "status": "SCORED",
  "promptText": "On a bright morning, Lina stepped out...",
  "promptWordCount": 250,
  "sampleReadSeconds": 75,
  "wordsPerMinute": 200.0,
  "selectedBookEntryIds": ["64f1a2b3c4d5e6f7a8b9c0d1"],
  "bookPlans": [
    {
      "bookEntryId": "64f1a2b3c4d5e6f7a8b9c0d1",
      "title": "Effective Java",
      "pageCount": 464,
      "isPageCountEstimate": false,
      "estimatedHours": 11.6,
      "estimatedDays": 0.48,
      "estimatedDaysAtDailyReading": 15.47
    }
  ],
  "totalEstimatedHours": 11.6,
  "totalEstimatedDays": 0.48,
  "totalEstimatedDurationDays": 0,
  "totalEstimatedDurationHours": 11.6,
  "dailyReadingMinutes": 45,
  "totalEstimatedDaysAtDailyReading": 15.47,
  "startedAt": "2026-05-16T09:00:00",
  "submittedAt": "2026-05-16T09:02:00",
  "createdAt": "2026-05-16T09:00:00",
  "updatedAt": "2026-05-16T09:05:00"
}
```

| Field                           | Type                   | Nullable | Notes                                          |
|---------------------------------|------------------------|----------|------------------------------------------------|
| `id`                            | string                 | No       |                                                |
| `status`                        | string (enum)          | No       | `IN_PROGRESS`, `SUBMITTED`, `SCORED`           |
| `promptText`                    | string                 | Yes      | Present after `start`; the passage to read     |
| `promptWordCount`               | integer                | Yes      |                                                |
| `sampleReadSeconds`             | integer                | Yes      | Present after `complete`                       |
| `wordsPerMinute`                | number                 | Yes      | Calibrated reading speed; present after `complete` |
| `selectedBookEntryIds`          | string[]               | No       | Empty list before `complete`                   |
| `bookPlans`                     | `BookPlanResponse[]`   | No       | Empty list before `complete`                   |
| `totalEstimatedHours`           | number                 | Yes      | Present after `complete`                       |
| `totalEstimatedDays`            | number                 | Yes      | Present after `complete`                       |
| `totalEstimatedDurationDays`    | integer                | Yes      | Whole-day component of total duration          |
| `totalEstimatedDurationHours`   | number                 | Yes      | Remaining hours component of total duration    |
| `dailyReadingMinutes`           | integer                | Yes      | Present after `daily-plan`                     |
| `totalEstimatedDaysAtDailyReading` | number              | Yes      | Present after `daily-plan`                     |
| `startedAt`                     | datetime               | Yes      |                                                |
| `submittedAt`                   | datetime               | Yes      | Present after `complete`                       |
| `createdAt`                     | datetime               | Yes      |                                                |
| `updatedAt`                     | datetime               | Yes      |                                                |

#### `BookPlanResponse` (nested in `bookPlans`)

| Field                       | Type    | Nullable | Notes                                                              |
|-----------------------------|---------|----------|--------------------------------------------------------------------|
| `bookEntryId`               | string  | No       |                                                                    |
| `title`                     | string  | No       |                                                                    |
| `pageCount`                 | integer | No       | From Google Books, or fallback of 300 if unavailable               |
| `isPageCountEstimate`       | boolean | No       | `true` when Google Books had no page count; show estimate indicator |
| `estimatedHours`            | number  | No       | Rounded to 2 decimal places                                        |
| `estimatedDays`             | number  | No       | Rounded to 2 decimal places                                        |
| `estimatedDaysAtDailyReading` | number | Yes     | Present after `daily-plan` is applied                              |

**`isPageCountEstimate` guidance:** When `true`, display a UI indicator such as
`"~11.6 hours (estimated — page count unavailable)"`. The 300-page fallback represents a
typical novel length (260–350 pages). Users can retry after Google Books becomes available.

---

## 7. Sample Requests & Responses

### User Flow

#### Register / first login (automatic on first authenticated request)

The backend creates a user profile on the first authenticated API call. No explicit
registration endpoint is needed.

#### Get profile

```http
GET /api/users/me
Authorization: Bearer <token>
```

```json
{
  "id": "uid-abc123",
  "email": "jane@example.com",
  "displayName": "Jane Smith",
  "totalBooks": 3,
  "createdAt": "2026-01-10T09:00:00",
  "updatedAt": "2026-05-16T10:00:00",
  "lastLoginAt": "2026-05-16T10:00:00"
}
```

---

### Book Shelf Flow

#### 1. Search for a book (no auth needed)

```http
GET /api/books/search?title=Effective+Java&author=Bloch
```

```json
[
  {
    "googleBookId": "NggnmAEACAAJ",
    "isbn": "9780134685991",
    "title": "Effective Java",
    "authors": ["Joshua Bloch"],
    "coverImageUrl": "https://books.google.com/...",
    "publisher": "Addison-Wesley Professional",
    "publishedDate": "2018-01-06"
  }
]
```

#### 2. Save to shelf

```http
POST /api/books
Authorization: Bearer <token>
Content-Type: application/json

{
  "googleBookId": "NggnmAEACAAJ",
  "isbn": "978-0134685991",
  "title": "Effective Java",
  "authors": ["Joshua Bloch"],
  "coverImageUrl": "https://books.google.com/...",
  "shelf": "reading"
}
```

```json
HTTP 201 Created

{
  "id": "64f1a2b3c4d5e6f7a8b9c0d1",
  "userId": "uid-abc123",
  "googleBookId": "NggnmAEACAAJ",
  "isbn": "9780134685991",
  "title": "Effective Java",
  "authors": ["Joshua Bloch"],
  "coverImageUrl": "https://books.google.com/...",
  "shelf": "reading",
  "rating": null,
  "notes": null,
  "startedAt": null,
  "finishedAt": null,
  "createdAt": "2026-05-16T10:00:00"
}
```

#### 3. Mark as finished with rating

```http
PUT /api/books/64f1a2b3c4d5e6f7a8b9c0d1
Authorization: Bearer <token>
Content-Type: application/json

{
  "shelf": "finished",
  "rating": 5,
  "finishedAt": "2026-05-16"
}
```

```json
HTTP 200 OK

{
  "id": "64f1a2b3c4d5e6f7a8b9c0d1",
  "shelf": "finished",
  "rating": 5,
  "finishedAt": "2026-05-16",
  ...
}
```

#### 4. Browse finished shelf

```http
GET /api/shelves/finished?page=0&size=20
Authorization: Bearer <token>
```

```json
{
  "content": [ { ... } ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### Reading Test Flow

#### 1. Start test

```http
POST /api/reading-tests/start
Authorization: Bearer <token>
```

```json
HTTP 201 Created

{
  "id": "65a1b2c3d4e5f6a7b8c9d0e1",
  "status": "IN_PROGRESS",
  "promptText": "On a bright morning, Lina stepped out onto the balcony...",
  "promptWordCount": 250,
  "bookPlans": [],
  "selectedBookEntryIds": [],
  ...
}
```

Display `promptText` to the user. Start a timer when they begin reading.

#### 2. Complete test

```http
POST /api/reading-tests/65a1b2c3d4e5f6a7b8c9d0e1/complete
Authorization: Bearer <token>
Content-Type: application/json

{
  "sampleReadSeconds": 75,
  "bookEntryIds": ["64f1a2b3c4d5e6f7a8b9c0d1"]
}
```

```json
HTTP 200 OK

{
  "id": "65a1b2c3d4e5f6a7b8c9d0e1",
  "status": "SUBMITTED",
  "wordsPerMinute": 200.0,
  "bookPlans": [
    {
      "bookEntryId": "64f1a2b3c4d5e6f7a8b9c0d1",
      "title": "Effective Java",
      "pageCount": 464,
      "isPageCountEstimate": false,
      "estimatedHours": 11.6,
      "estimatedDays": 0.48,
      "estimatedDaysAtDailyReading": null
    }
  ],
  "totalEstimatedHours": 11.6,
  "totalEstimatedDays": 0.48,
  "totalEstimatedDurationDays": 0,
  "totalEstimatedDurationHours": 11.6,
  ...
}
```

Check `isPageCountEstimate` per book plan. If `true`, show an estimate disclaimer.

#### 3. Apply daily reading plan (optional)

```http
POST /api/reading-tests/65a1b2c3d4e5f6a7b8c9d0e1/daily-plan
Authorization: Bearer <token>
Content-Type: application/json

{
  "dailyReadingMinutes": 45
}
```

```json
HTTP 200 OK

{
  "status": "SCORED",
  "dailyReadingMinutes": 45,
  "bookPlans": [
    {
      "estimatedDaysAtDailyReading": 15.47,
      ...
    }
  ],
  "totalEstimatedDaysAtDailyReading": 15.47,
  ...
}
```

---

## 8. Error Code Reference

Use the `code` field in error responses for deterministic frontend handling.

### 400 — Validation / Bad Request

| Code                            | Trigger                                                          |
|---------------------------------|------------------------------------------------------------------|
| `VALIDATION_ERROR`              | Bean validation failed (missing required field, format error)    |
| `INVALID_PAGINATION_PARAMS`     | `page < 0` or `size < 1` or `size > 100`                       |
| `INVALID_SHELF`                 | Shelf name not in `[reading, finished, want-to-read]`            |
| `INVALID_RATING`                | Rating outside 1–6                                               |
| `INVALID_ISBN`                  | ISBN format invalid or normalisation failed                      |
| `INVALID_DATE_RANGE`            | `from` is after `to` in list filters                             |
| `READING_TEST_MISSING_METADATA` | Book has no Google Books ID for page-count lookup               |
| `READING_TEST_MISSING_PAGE_COUNT` | (reserved; currently falls back to estimate, not error)        |

### 404 — Not Found

| Code                   | Trigger                                                      |
|------------------------|--------------------------------------------------------------|
| `USER_NOT_FOUND`       | Authenticated user not in database (deleted or first-login race) |
| `BOOK_NOT_FOUND`       | Book ID not found for the authenticated user                  |
| `READING_TEST_NOT_FOUND` | Reading test ID not found for the authenticated user        |

### 409 — Conflict

| Code                         | Trigger                                                       |
|------------------------------|---------------------------------------------------------------|
| `BOOK_ALREADY_ON_SHELF`      | Same book (by ISBN or googleBookId) already saved             |
| `ACTIVE_READING_TEST_EXISTS` | User already has a `DRAFT` or `IN_PROGRESS` test              |
| `INVALID_READING_TEST_STATE` | Test is not in the state required for the requested operation |
| `DUPLICATE_EMAIL`            | Email address already registered to another user              |

### 503 — Service Unavailable

| Code                      | Trigger                                         |
|---------------------------|-------------------------------------------------|
| `GOOGLE_BOOKS_UNAVAILABLE`| `GET /api/books/search` failed to reach Google Books API |

> **Note:** `POST /api/reading-tests/{id}/complete` does **not** return 503. When Google
> Books is unreachable during a reading test completion, the backend applies a 300-page
> fallback estimate and returns 200 with `isPageCountEstimate: true`.

---

## 9. Environment Variable Reference

### Required in Production

| Variable                     | Example value                               | Description                                      |
|------------------------------|---------------------------------------------|--------------------------------------------------|
| `MONGODB_URI`                | `mongodb+srv://user:pass@cluster.mongodb.net/shelflife` | MongoDB connection string            |
| `FIREBASE_AUTH_ENABLED`      | `true`                                      | Must be `true` in production                     |
| `FIREBASE_PROJECT_ID`        | `shelflife-968cf`                           | Firebase project ID                              |
| `FIREBASE_SERVICE_ACCOUNT_PATH` | `/secrets/firebase-service-account.json` | Path to the Firebase service account JSON file   |
| `APP_CORS_ALLOWED_ORIGINS`   | `https://app.shelflife.example.com`         | Comma-separated list of allowed origins. No wildcards. No localhost. |
| `GOOGLE_BOOKS_API_KEY`       | `AIza...`                                   | Optional but recommended to avoid anonymous quota limits |

### Local Development Overrides

| Variable                     | Default (local profile)          | Description                                    |
|------------------------------|----------------------------------|------------------------------------------------|
| `MONGODB_URI`                | `mongodb://localhost:27017/shelflife` | Override for Atlas or remote dev DB       |
| `APP_CORS_ALLOWED_ORIGINS`   | `http://localhost:3000`          | Override for different frontend dev port        |
| `GOOGLE_BOOKS_API_KEY`       | _(empty — lower quota)_          | Override to use a key in local development      |

### Variables Not Required in Local Dev

The following are managed by the `local` Spring profile and do not need to be set:

- `FIREBASE_AUTH_ENABLED` (forced `false` in local profile)
- `FIREBASE_PROJECT_ID` (set to `local-dev`)
- `FIREBASE_SERVICE_ACCOUNT_PATH`
- `READING_TEST_ACTIVE_INDEX_INITIALIZER_ENABLED`
- `USER_EMAIL_INDEX_INITIALIZER_ENABLED`

---

## 10. API Versioning Strategy

### Current State

The API is **unversioned** (no `/v1/` prefix). This is appropriate while the frontend
and backend are developed in-house and deployed together.

### Policy for Introducing Breaking Changes

A change is **breaking** if it:
- Removes a field from a response body
- Renames a field in a request or response body
- Changes the type of an existing field
- Removes or renames an endpoint
- Changes the semantics of an existing field (e.g. a field that was optional becomes required)

A change is **non-breaking** if it:
- Adds a new optional field to a response body
- Adds a new endpoint
- Adds a new optional query parameter with a safe default
- Adds a new error code for a previously unhandled case

### Migration Plan (when versioning becomes necessary)

1. **Add a `v2` path prefix** for the changed endpoint(s), e.g. `GET /api/v2/shelves`
2. Keep the `v1` path operating unchanged until all frontend consumers have migrated
3. Deprecate `v1` with a response header: `Deprecation: true`
4. Remove `v1` after a confirmed cutover with a defined sunset date

Until a breaking change is needed, no version prefix will be added. Adding fields to
responses (additive changes) is always safe and does not require versioning.
