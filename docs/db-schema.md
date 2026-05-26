# Database Schema

The default local database is file-backed H2 under `storage/localdb/`. Production can still use PostgreSQL by overriding `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`.

Flyway migration `backend/src/main/resources/db/migration/V1__init.sql` creates:

- `users`
- `naukri_credentials`
- `job_filters`
- `applied_jobs`
- `external_redirect_jobs`
- `question_answers`
- `bot_logs`
- `bot_status`
- `scheduler_config`
- `resume_profiles`

All API writes go through Spring Data JPA repositories with parameterized queries to avoid SQL injection.
