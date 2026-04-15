# ShelfLife – Personal Bookshelf Spring Boot Application

ShelfLife is a Spring Boot backend for a personal bookshelf / reading list app where users can search books, save titles to shelves, rate books, and keep notes.

## Tech Stack

- **Java 17**
- **Spring Boot 3.x**
- **MongoDB Atlas** via Spring Data MongoDB
- **Spring Security** with Firebase JWT filter scaffold
- **Google Books API** integration
- **Maven** build

## Prerequisites

- Java 17+
- Maven 3.9+
- MongoDB Atlas account (free tier)
- Firebase project (for production JWT validation)
- Google Books API key

## Setup

1. Clone the repository.
2. Update `/home/runner/work/shelflife/shelflife/src/main/resources/application.yml` with:
   - `spring.data.mongodb.uri`
   - `google.books.api.key`
   - Firebase settings
   - Allowed CORS origins
3. (Optional) Enable Firebase JWT verification by setting:
   - `firebase.auth.enabled: true`
   - Firebase Admin SDK credentials initialization in your runtime environment.

## Run Locally

```bash
mvn spring-boot:run
```

Default server URL: `http://localhost:8080`

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/books/search?q={query}` | Proxy search against Google Books API |
| `GET` | `/api/shelves` | Get all books for authenticated user |
| `GET` | `/api/shelves/{shelf}` | Get books for a specific shelf |
| `POST` | `/api/books` | Save a book to a shelf |
| `PUT` | `/api/books/{id}` | Update shelf/rating/notes/dates |
| `DELETE` | `/api/books/{id}` | Remove a saved book |
| `GET` | `/api/books/{id}` | Get a single saved book |

### Authentication

- Search endpoint is public.
- Other `/api/**` endpoints require `Authorization: Bearer <token>`.
- Current filter supports:
  - Placeholder validation mode (`firebase.auth.enabled=false`): token string is used as principal.
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
│   └── SearchController.java
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

## MongoDB Atlas Notes

- Create a free cluster.
- Add your current IP to the network allowlist.
- Create a database user and use its credentials in `spring.data.mongodb.uri`.

## Firebase Notes

- Create a Firebase project and enable Authentication.
- Generate a service account key for backend verification.
- Initialize Firebase Admin SDK in your runtime before enabling strict verification.

## Google Books API Notes

- Create an API key in Google Cloud Console.
- Add it to `google.books.api.key`.

## Deployment Notes (AWS Elastic Beanstalk)

1. Build: `mvn clean package`
2. Deploy generated JAR (`target/*.jar`) to Elastic Beanstalk Java platform.
3. Configure environment variables for MongoDB URI, Google API key, Firebase settings.
4. Update CORS origins to your deployed frontend domain.
