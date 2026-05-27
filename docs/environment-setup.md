# Environment Setup Guide

## Local H2 Database

The default backend profile uses a file-backed H2 database, so local Windows runs do not require PostgreSQL:

```powershell
cd backend
mvn clean spring-boot:run
```

Database files are stored under `storage/localdb/`. If Flyway reports a failed local H2 migration from an interrupted run, stop the backend and delete the ignored `storage/localdb/*.db` files, then start again.

If your shell still has old PostgreSQL variables, clear them before starting:

```powershell
Remove-Item Env:SPRING_DATASOURCE_URL,Env:SPRING_DATASOURCE_USERNAME,Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
```

## Production PostgreSQL

Create the database and user:

```sql
CREATE DATABASE naukri_bot;
CREATE USER naukri_bot WITH ENCRYPTED PASSWORD 'change-me';
GRANT ALL PRIVILEGES ON DATABASE naukri_bot TO naukri_bot;
```

Spring Boot runs Flyway migrations automatically on startup.

## Windows Execution Policy

If PowerShell blocks npm scripts, use `npm.cmd`:

```powershell
npm.cmd install
npm.cmd run build
```

## Local CORS

The backend accepts local UI origins through `APP_CORS_ALLOWED_ORIGIN_PATTERNS`. The default allows `localhost`, `127.0.0.1`, IPv6 localhost, and private LAN dev URLs on any port:

```env
APP_CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:*,http://127.0.0.1:*,http://[::1]:*,http://192.168.*.*:*,http://10.*.*.*:*,http://172.*.*.*:*
```

## Playwright

The Docker backend image includes Playwright dependencies. On a local Windows machine, install the browser once if needed:

```powershell
mvn -pl automation exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

## Manual Captcha Login

Naukri captcha and OTP prompts must be completed by the user. When the bot reports `CAPTCHA_REQUIRED`, use the Bot Control page action `Login Manually & Continue`. The backend opens Chromium in visible mode and waits for login completion before continuing the pending automation run.

```env
BOT_MANUAL_LOGIN_TIMEOUT_SECONDS=600
```

## Live Question Popup

When a Naukri apply popup asks a question that is not already saved in Q&A, the backend waits and the Angular app shows an answer dialog. Submit the answer in the dialog to save it and continue the same apply flow.

```env
BOT_QUESTION_ANSWER_TIMEOUT_SECONDS=600
```

## Safe Automation Mode

Keep dry run enabled until the account, filters, resume paths, proxy, and compliance review are complete:

```env
BOT_DRY_RUN=true
```
