# Reading Test Backend Design and Implementation Notes

## Purpose
This document describes the implemented reading test backend architecture and API behavior.
It also records current constraints and follow-up considerations for future iterations.

## Implemented Scope
- Controller, service, repository, and model layers for user-scoped reading tests.
- Timed reading-speed calibration flow.
- Per-book duration estimation using Google Books page count metadata.
- Daily reading plan estimation.
- Ownership and authentication enforcement using existing auth model.
- Startup index initialization for active-test uniqueness safety.

## Design Goals
- Keep all reading test data private and scoped to the authenticated user.
- Ensure only one active calibration test exists per user at a time.
- Provide deterministic and explainable estimates from submitted timing data.
- Preserve compatibility with current `/api/**` security behavior.

## Implemented Backend Modules

### Controller Layer
Class: `ReadingTestController`

Route base:
- `/api/reading-tests`

Endpoints:
- `POST /api/reading-tests/start`
- `POST /api/reading-tests/{testId}/complete`
- `POST /api/reading-tests/{testId}/daily-plan`
- `GET /api/reading-tests/{testId}`
- `GET /api/reading-tests`

### Service Layer
Class: `ReadingTestService`

Responsibilities:
- Start in-progress tests with prompt text and prompt word count.
- Validate user ownership and test state transitions.
- Compute words-per-minute from prompt word count and `sampleReadSeconds`.
- Build per-book duration estimates from page count metadata.
- Compute aggregate days/hours estimates and daily-plan projections.
- Map upstream page-count lookup failures to controlled API responses.

### Repository Layer
Class: `ReadingTestRepository`

Responsibilities:
- User-scoped lookups by id and list views.
- Status-filtered list queries.
- Active-test existence checks by status set.
- User-scoped delete for account cleanup.

### Model Layer
Class: `ReadingTest`

Stored fields:
- `id`
- `userId`
- `status` (`DRAFT`, `IN_PROGRESS`, `SUBMITTED`, `SCORED`)
- `promptText`
- `promptWordCount`
- `sampleReadSeconds`
- `wordsPerMinute`
- `selectedBookEntryIds`
- `bookPlans[]`
- `totalEstimatedHours`
- `totalEstimatedDays`
- `dailyReadingMinutes`
- `totalEstimatedDaysAtDailyReading`
- `startedAt`
- `submittedAt`
- `createdAt`
- `updatedAt`

`bookPlans[]` snapshot fields:
- `bookEntryId`
- `title`
- `pageCount`
- `estimatedHours`
- `estimatedDays`
- `estimatedDaysAtDailyReading`

## Data Ownership and Relationships
- Reading tests are user-scoped documents.
- Selected books are represented by `selectedBookEntryIds` and snapshot data in `bookPlans`.
- Ownership checks occur through user-scoped repository queries and `BookService.getBookForUser(...)`.
- Deleting a user deletes user-owned reading tests (`UserService.deleteAccount(...)`).

## Authentication and Authorization Rules
- All reading-test routes are under `/api/**` and require authentication.
- Request identity is derived from `Authentication.getName()`.
- Cross-user access is denied by user-scoped lookups.

## Implemented API Contracts

### Start Test
- `POST /api/reading-tests/start`
- Auth required: yes
- Response: `201 Created` with prompt metadata and test id
- Error cases:
  - `401` unauthorized
  - `404` user not found
  - `409` active test already exists

### Complete Test
- `POST /api/reading-tests/{testId}/complete`
- Auth required: yes
- Request body:
  - `sampleReadSeconds` (positive integer)
  - `bookEntryIds` (non-empty, non-blank list)
- Response: `200 OK` with per-book and aggregate estimates
- Error cases:
  - `400` invalid payload or missing page count metadata
  - `401` unauthorized
  - `404` user/test/book not found for owner
  - `409` invalid test state
  - `502` upstream Google Books lookup failure

### Apply Daily Plan
- `POST /api/reading-tests/{testId}/daily-plan`
- Auth required: yes
- Request body:
  - `dailyReadingMinutes` (positive integer)
- Response: `200 OK` with daily-plan completion estimate
- Error cases:
  - `400` invalid payload
  - `401` unauthorized
  - `404` user/test not found
  - `409` invalid test state

### Get Test
- `GET /api/reading-tests/{testId}`
- Auth required: yes
- Response: `200 OK` with user-scoped test details
- Error cases:
  - `401` unauthorized
  - `404` user/test not found

### List Tests
- `GET /api/reading-tests?status=&from=&to=`
- Auth required: yes
- Response: `200 OK` with filtered user-scoped tests
- Error cases:
  - `400` invalid date range
  - `401` unauthorized
  - `404` user not found

## Validation Rules
- `sampleReadSeconds` must be greater than `0`.
- `bookEntryIds` must be non-empty and contain unique non-blank values.
- `dailyReadingMinutes` must be greater than `0`.
- Complete is allowed only from `IN_PROGRESS`.
- Daily plan is allowed only from `SUBMITTED` or `SCORED`.

## Indexing and Rollout Strategy
Collection indexes:
- `userId + createdAt`
- `userId + status + createdAt`

Startup initializer (`ReadingTestIndexInitializer`) additionally ensures:
- Legacy malformed or duplicate active records are deactivated safely.
- Unique partial index on `userId` for active statuses (`DRAFT`, `IN_PROGRESS`).

This prevents race conditions for active-test creation and supports safe rollout in existing environments.

## Error Contract
- Uses `GlobalExceptionHandler` and existing `ErrorResponse` format.
- Uses `ResponseStatusException` for request, validation, and state violations.
- Returns controlled message for upstream Google Books lookup failure.

## Testing Coverage
- Service tests for state transitions, estimation, and error mapping.
- Controller tests for request validation and endpoint wiring.
- Security integration tests for unauthorized access and user-scoped access boundaries.
- Index initializer tests for dedupe and index enforcement behavior.

## Follow-Up Considerations
- Add explicit OpenAPI/Swagger documentation for request/response examples.
- Consider adding endpoint-level contract tests for full serialized error payloads on `502` paths.
