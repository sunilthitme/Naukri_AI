# Environment Setup Guide

## PostgreSQL

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
