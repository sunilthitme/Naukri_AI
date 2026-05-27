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
- H2 is used by default for local runs; PostgreSQL 16+ is optional for production
- Chrome/Chromium for Angular tests and Playwright automation

## Environment

Copy `.env.example` to `.env` and replace every secret value.

Important variables:

- `APP_JWT_SECRET`: at least 32 characters
- `APP_ENCRYPTION_KEY`: at least 32 characters; first 32 bytes are used for AES
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` if overriding the default H2 database
- `APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_PASSWORD`
- `BOT_DRY_RUN=true` for safe local testing
- `BOT_MOCK=true` only for local smoke tests that should not contact Naukri
- `BOT_MANUAL_LOGIN_TIMEOUT_SECONDS` for the manual captcha login window, default `600`
- `BOT_QUESTION_ANSWER_TIMEOUT_SECONDS` for the live question answer popup window, default `600`
- `OPENAI_BASE_URL`, `OPENAI_API_KEY`, `OPENAI_MODEL` for compatible LLM scoring

## Run With Docker

```powershell
docker compose --env-file .env up --build
```

Frontend: `http://localhost:4200`

Backend: `http://localhost:8080`

## Run Locally On Windows

Backend with the default file-backed H2 database:

```powershell
cd backend
mvn clean spring-boot:run
```

Default local login:

- Email: `admin@example.com`
- Password: `ChangeMe123!`

If your PowerShell session still has old PostgreSQL variables, clear them before starting:

```powershell
Remove-Item Env:SPRING_DATASOURCE_URL,Env:SPRING_DATASOURCE_USERNAME,Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
```

Local smoke profile with in-memory H2 and mock automation:

```powershell
mvn -pl backend -am spring-boot:run -Dspring-boot.run.profiles=local
```

The default profile stores H2 files under `storage/localdb/`. If Flyway reports a failed local H2 migration from an interrupted run, stop the backend and delete the ignored `storage/localdb/*.db` files, then start again. The `local` profile uses an in-memory H2 database, Flyway migrations, a bootstrap admin, dry-run automation, and mock automation.

Production PostgreSQL override:

```powershell
$env:APP_JWT_SECRET="replace-with-at-least-32-characters"
$env:APP_ENCRYPTION_KEY="replace-with-32-byte-aes-key-text"
$env:APP_BOOTSTRAP_ADMIN_EMAIL="admin@example.com"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD="ChangeMe123!"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/naukri_bot"
$env:SPRING_DATASOURCE_USERNAME="naukri_bot"
$env:SPRING_DATASOURCE_PASSWORD="change-me"
cd backend
mvn clean spring-boot:run
```

Frontend:

```powershell
cd frontend
npm.cmd install
npm.cmd start
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

## Manual Captcha Login Recovery

When Naukri shows captcha, OTP, or a human verification page, the bot marks the run as `CAPTCHA_REQUIRED` and the Bot Control page shows `Login Manually & Continue`. Click it to open a visible Playwright browser on the backend machine, complete the Naukri login manually, and the automation will continue from the pending search/apply flow after login is detected.

The manual wait time is controlled by `BOT_MANUAL_LOGIN_TIMEOUT_SECONDS`. The default is 10 minutes.

## Live Apply Question Handling

During an apply flow, saved Q&A entries are matched and filled automatically. If Naukri asks a new question, the automation pauses on that job and the Angular shell shows an `Answer required` popup. Enter the answer and click `Save Answer & Continue`; the answer is stored permanently and Playwright continues filling the same Naukri form.

The wait time is controlled by `BOT_QUESTION_ANSWER_TIMEOUT_SECONDS`. Keep `BOT_DRY_RUN=false` when you want real applications submitted; with dry-run enabled the bot opens jobs and records dry-run rows without clicking apply.

## API Collection

Import `docs/naukri-ai-job-apply-bot.postman_collection.json` into Postman or compatible API tools.

## Corporate Proxy

See `docs/corporate-proxy-guide.md`.
