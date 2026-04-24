# AI Job Apply Assistant

Full-stack AI-powered job auto-apply platform built with Spring Boot, Angular, Playwright, and Ollama.

## What’s Included

- Spring Boot 3 backend with JWT auth, encrypted credential storage, JPA entities, scheduler, report generation, rolling logs, and clean controller/service/repository layering.
- Angular frontend with a dashboard, profile setup, live question queue, reports, and logs.
- Playwright-based bot engine with Naukri login/search/apply scaffolding, redirected portal adapters, captcha detection, human-like delays, duplicate prevention hooks, and AI-assisted form filling.
- Local AI integration through Ollama only. No paid AI APIs are used.
- Docker setup for MySQL, Ollama, backend, and frontend.

## Core Backend Packages

```text
backend/src/main/java/com/aijobapplyassistant/
  config/
  controller/
  dto/
  entity/
  repository/
  service/
    ai/
    bot/
    report/
    security/
  exception/
```

## Local Run

### Backend

```bash
cd backend
mvn spring-boot:run
```

Backend runs at `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm start
```

Frontend runs at `http://localhost:4200`.

## Verified Build

- Backend: `mvn test`
- Frontend: `npm run build`

## Default Login

- Admin: `admin@aijobapply.local`
- Password: `Admin@123`

## Required Runtime Services

- Java 21
- Node 20+ or 22+
- MySQL 8 or PostgreSQL
- Ollama with a local model such as `llama3.1`, `mistral`, `phi`, or another open-source model

## Environment

Use `.env.example` as the reference for runtime variables. Important values:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `CREDENTIAL_SECRET`
- `OLLAMA_BASE_URL`
- `OLLAMA_MODEL`
- `PLAYWRIGHT_HEADLESS`

## Docker

```bash
docker compose up --build
```

Services started by compose:

- MySQL on `3306`
- Ollama on `11434`
- Spring Boot backend on `8080`
- Angular frontend via Nginx on `4200`

After Ollama starts, pull a model once if needed:

```bash
docker exec -it ai-job-apply-ollama ollama pull llama3.1
```

## Reports And Logs

- Rolling app logs: `logs/application-current.txt` and `logs/application-yyyy-mm-dd.txt`
- Generated reports: `reports/`
- Stored resumes: `resumes/`
- Manual schema reference: [backend/mysql-schema.sql](/C:/Users/Admin/OneDrive/Documents/New%20project/backend/mysql-schema.sql)

## Notes

- The Playwright automation layer is production-structured and ready for selector tuning, but live third-party portals like Naukri, Workday, Taleo, Lever, and Greenhouse can change markup frequently. Expect some selector refinement against real environments.
- Captcha bypass is not implemented. The app detects captcha and surfaces it as a manual intervention state.
- Redirected portal account creation is supported through a generic adapter pattern with stored encrypted credentials, and can be extended further per portal.
