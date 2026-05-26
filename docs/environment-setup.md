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

## Playwright

The Docker backend image includes Playwright dependencies. On a local Windows machine, install the browser once if needed:

```powershell
mvn -pl automation exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

## Safe Automation Mode

Keep dry run enabled until the account, filters, resume paths, proxy, and compliance review are complete:

```env
BOT_DRY_RUN=true
```
