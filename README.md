# Politica em claro

MVP for reading Brazilian legislative proposals in everyday Portuguese.

## Stack

- Java 21, Spring Boot 3.5, Spring Data JPA and PostgreSQL
- Spring AI with Google Gemini
- React, Vite and Tailwind CSS 4

## Backend

Install JDK 21, Maven and PostgreSQL. Create the database before starting.
Configure these environment variables in the terminal running Maven:

| Variable | Purpose |
| --- | --- |
| `DB_URL` | PostgreSQL JDBC URL, for example `jdbc:postgresql://your-host:5432/politica?sslmode=verify-full` |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `GEMINI_API_KEY` | Gemini Developer API key from Google AI Studio |
| `GEMINI_MODEL` | Optional model override; defaults to `gemini-2.5-flash` |

Keep credentials outside version control. Spring Boot does not load `.env` files automatically.
TLS certificate trust must be configured for the database host when using `sslmode=verify-full`.

```shell
mvn spring-boot:run
```

The startup runner fetches the first page of up to 100 PEC/PL proposals,
attempts a Gemini translation for each one, and saves the results.
Each restart repeats these calls and can consume API quota.
If translation fails, raw data is saved and processing continues.
Database connectivity is required for startup.

The public GET endpoint is `http://localhost:8080/api/propositions`.
It returns all stored proposals ordered by `updatedAt` descending.
Wildcard CORS and `ddl-auto=update` are development settings; restrict CORS
and introduce schema migrations before production.
LLM explanations are drafts and require factual review.

## Frontend

```shell
cd frontend
npm ci
npm run dev
```

Open the URL printed by Vite. The feed requests the backend at
`http://localhost:8080/api/propositions` and handles loading, errors,
empty responses and missing translations.

## Checks

```shell
mvn test
npm --prefix frontend run build
npm --prefix frontend run lint
```

JPA tests use a test-only H2 database; they do not prove live PostgreSQL or Gemini connectivity.