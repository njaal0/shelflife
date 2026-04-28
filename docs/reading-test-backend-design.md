# Reading Test Backend Design (Future Phase)

## Purpose
This document defines the planned backend design for the reading test feature.
It is a design-only artifact for future implementation and does not introduce runtime changes.

## Scope
- Define module boundaries for controller, service, repository, and model layers.
- Define authentication and authorization rules.
- Define linkage between reading tests and current entities (User and BookEntry).
- Define API contracts at a high level.
- Define test and rollout strategy.

## Out of Scope
- No controller/service/model/repository classes are implemented in this phase.
- No database migration scripts are executed in this phase.
- No frontend contracts are finalized beyond draft endpoint shapes.

## Design Goals
- Keep all reading test data user-scoped and private.
- Support test creation, answer submission, and result retrieval.
- Keep compatibility with current auth model (FirebaseAuthFilter + /api/** protection).
- Minimize impact on existing book and user flows.

## Proposed Backend Modules

### Controller Layer
Planned class: ReadingTestController

Planned responsibilities:
- Create a reading test for a specific book.
- Submit answers for an active test.
- Retrieve one test result.
- List authenticated user's tests with optional filters.

Planned route base:
- /api/reading-tests

### Service Layer
Planned class: ReadingTestService

Planned responsibilities:
- Validate test ownership and book ownership.
- Generate test content from available metadata (initially static/question bank; optional AI later).
- Score submissions.
- Persist result snapshots and status transitions.

### Repository Layer
Planned class: ReadingTestRepository

Planned responsibilities:
- User-scoped lookups.
- Book-scoped lookups.
- Status and date-range queries.

### Model Layer
Planned class: ReadingTest

Planned fields:
- id
- userId
- bookEntryId
- googleBookId (optional denormalized lookup key)
- status (DRAFT, IN_PROGRESS, SUBMITTED, SCORED)
- questionSetVersion
- questions (stored prompt/options snapshot)
- answers (submitted answers)
- score (numeric)
- maxScore
- feedbackSummary
- startedAt
- submittedAt
- createdAt
- updatedAt

## Data Ownership and Relationships
- User -> BookEntry already exists.
- ReadingTest should belong to exactly one user and one BookEntry.
- Access checks should always verify userId and bookEntryId together.
- Deleting a user should delete reading tests for that user.
- Deleting a book entry should delete associated reading tests or mark them archived (final behavior to be decided).

## Authentication and Authorization Rules
- All reading test endpoints are protected under /api/**.
- Request identity is derived from Authentication.getName() (current userId pattern).
- No cross-user access is allowed.
- Any test lookup must be user-scoped at repository level.

## Draft API Contracts

### Create Test
- POST /api/reading-tests/books/{bookEntryId}
- Auth required: yes
- Response: 201 with created test metadata and questions
- Error cases:
  - 401 unauthorized
  - 404 book not found for user
  - 409 test already active for book (if one-active-test rule is enabled)

### Submit Answers
- POST /api/reading-tests/{testId}/submit
- Auth required: yes
- Request body: answers list
- Response: 200 with scored result summary
- Error cases:
  - 401 unauthorized
  - 404 test not found for user
  - 409 already submitted
  - 400 invalid answer payload

### Get Test
- GET /api/reading-tests/{testId}
- Auth required: yes
- Response: 200 with test details
- Error cases:
  - 401 unauthorized
  - 404 test not found for user

### List Tests
- GET /api/reading-tests?status=&bookEntryId=&from=&to=
- Auth required: yes
- Response: 200 with filtered list

## Validation Rules (Draft)
- bookEntryId must belong to authenticated user.
- answers size must match expected questions.
- answers must conform to question type constraints.
- submission allowed only when status is IN_PROGRESS.

## Indexing Strategy (Draft)
Recommended indexes for ReadingTest collection:
- userId + createdAt (list view)
- userId + status (filtered list)
- userId + bookEntryId + status (active test checks)
- bookEntryId (cascade/archive operations)

## Error Contract
- Reuse GlobalExceptionHandler and ErrorResponse structure.
- Use ResponseStatusException for request/state violations.
- Keep error codes/messages consistent with existing API style.

## Testing Strategy (Future Phase)
- Unit tests:
  - scoring and state transitions
  - validation rules
- Controller tests:
  - request/response mapping
  - validation and error payloads
- Security integration tests:
  - unauthenticated blocked
  - cross-user access denied
- Repository tests:
  - user-scoped queries and index-backed paths

## Rollout Plan (Future)
1. Introduce model + repository + service with unit tests.
2. Add controller endpoints and controller tests.
3. Add integration tests for auth + ownership boundaries.
4. Enable feature for frontend integration after API contract freeze.

## Open Decisions
- One active test per book vs multiple active drafts.
- Manual scoring only vs auto-scoring plus manual override.
- Hard delete vs archive behavior on book deletion.
- Whether question generation is deterministic by questionSetVersion.
