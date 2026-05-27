# Test Report

Generated on 2026-05-27.

| Area | Command | Result |
| --- | --- | --- |
| Backend + automation unit tests | `mvn test` | Passed: automation 6/6, backend 4/4 |
| Frontend production build | `npm.cmd run build` | Passed |
| Angular tests | `npm.cmd test` | Passed: 1/1 |
| Flyway migration smoke | backend local profile startup | Passed: V1 applied successfully |
| API E2E smoke | login, credentials, filters, Q&A, mock dry-run apply, dashboard, history, logs | Passed |
| Browser UI smoke | login, credentials, filters, Q&A, test apply, dashboard | Passed |
| Naukri login handling regression | `mvn test` with invalid/captcha/success classifiers | Passed |
| Manual captcha recovery regression | Maven/Angular compile plus local API startup | Passed: `/api/bot/manual-login` route and UI action compiled successfully |

Notes:

- The first Angular test attempt failed because Windows Chrome Headless could not initialize GPU. `frontend/karma.conf.js` now uses a corporate-friendly headless launcher with GPU disabled.
- Live Naukri application flow is implemented in Playwright Java but should be run with `BOT_DRY_RUN=true` first and valid user credentials.
- Browser screenshot: `docs/ui-dashboard-smoke.png`.
- 2026-05-26 update: tightened real Naukri login detection so invalid credentials, captcha, OTP, unchanged login page, and missing login form are surfaced as bot failures with log messages.
- 2026-05-27 update: added manual captcha login recovery. The bot opens a visible browser, waits for user login, and continues automation after login is detected.
