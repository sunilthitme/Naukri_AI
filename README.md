# Naukri AI Job Apply Bot

Production-ready full-stack application for managing a Naukri job-application automation workflow with Angular Material, Spring Boot, PostgreSQL, Playwright Java, JWT auth, AES credential encryption, CSV exports, scheduling, and notification hooks.

## What Is Included

- `frontend/` Angular 18 standalone app with Material UI pages for login, dashboard, credentials, filters, Q&A, history, bot control, and live logs.
- `backend/` Spring Boot 3 API with layered controllers, services, DTOs, repositories, security, Flyway migrations, scheduler, reports, and CSV handling.
- `automation/` Playwright Java module using Page Object Model for Naukri login/search/apply flow.
- `storage/` CSV, screenshots, and report output folders.
- `docs/` architecture, database schema, API collection, proxy guide, setup notes, and test report.

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 22+ with `npm.cmd` on Windows
- PostgreSQL 16+ for production
- Chrome/Chromium for Angular tests and Playwright automation

## Environment

Copy `.env.example` to `.env` and replace every secret value.

Important variables:

- `APP_JWT_SECRET`: at least 32 characters
- `APP_ENCRYPTION_KEY`: at least 32 characters; first 32 bytes are used for AES
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_PASSWORD`
- `BOT_DRY_RUN=true` for safe local testing
- `BOT_MOCK=true` only for local smoke tests that should not contact Naukri
- `OPENAI_BASE_URL`, `OPENAI_API_KEY`, `OPENAI_MODEL` for compatible LLM scoring

## Run With Docker

```powershell
docker compose --env-file .env up --build
```

Frontend: `http://localhost:4200`

Backend: `http://localhost:8080`

## Run Locally On Windows

Backend with PostgreSQL:

```powershell
$env:APP_JWT_SECRET="replace-with-at-least-32-characters"
$env:APP_ENCRYPTION_KEY="replace-with-32-byte-aes-key-text"
$env:APP_BOOTSTRAP_ADMIN_EMAIL="admin@example.com"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD="ChangeMe123!"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/naukri_bot"
$env:SPRING_DATASOURCE_USERNAME="naukri_bot"
$env:SPRING_DATASOURCE_PASSWORD="change-me"
mvn -pl backend -am spring-boot:run
```

You can also run from inside the backend folder:

```powershell
cd backend
mvn clean spring-boot:run
```

Local smoke profile without PostgreSQL:

```powershell
mvn -pl backend -am spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile uses an in-memory H2 database, Flyway migrations, a bootstrap admin, dry-run automation, and mock automation.

Frontend:

```powershell
cd frontend
npm.cmd install
npm.cmd start -- --proxy-config proxy.conf.json
```

## Build And Test

```powershell
mvn test
cd frontend
npm.cmd run build
npm.cmd test
```

## Security Notes

- Naukri passwords are encrypted with AES-GCM before persistence.
- Application login uses JWT with Spring Security.
- APIs are role-aware and protected by bearer token.
- DB access is through Spring Data JPA parameterized queries.
- Captcha is detected and pauses automation; this app does not bypass captcha.
- Keep `BOT_DRY_RUN=true` until credentials, filters, and compliance approvals are validated.
- Use `BOT_MOCK=true` only for automated local smoke tests; leave it false for real Playwright automation.

## API Collection

Import `docs/naukri-ai-job-apply-bot.postman_collection.json` into Postman or compatible API tools.

## Corporate Proxy

See `docs/corporate-proxy-guide.md`.
