# Database Schema

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
