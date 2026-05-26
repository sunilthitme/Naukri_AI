CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE naukri_credentials (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    encrypted_password TEXT NOT NULL,
    resume_path TEXT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_naukri_credentials_user UNIQUE(user_id)
);

CREATE TABLE job_filters (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    keywords TEXT,
    experience VARCHAR(100),
    location VARCHAR(255),
    salary VARCHAR(100),
    work_mode VARCHAR(100),
    freshness VARCHAR(100),
    preferred_companies TEXT,
    blacklisted_companies TEXT,
    auto_apply BOOLEAN NOT NULL DEFAULT TRUE,
    external_career_apply BOOLEAN NOT NULL DEFAULT FALSE,
    daily_apply_limit INTEGER NOT NULL DEFAULT 20,
    easy_apply_only BOOLEAN NOT NULL DEFAULT TRUE,
    duplicate_prevention_days INTEGER NOT NULL DEFAULT 30,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_job_filters_user UNIQUE(user_id)
);

CREATE TABLE applied_jobs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(255) NOT NULL,
    job_title VARCHAR(500) NOT NULL,
    apply_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(80) NOT NULL,
    job_url TEXT,
    experience VARCHAR(255),
    salary VARCHAR(255),
    location VARCHAR(255),
    redirected_external_site BOOLEAN NOT NULL DEFAULT FALSE,
    csv_file_name VARCHAR(255),
    failure_reason TEXT,
    match_score DOUBLE PRECISION,
    screenshot_path TEXT,
    attempts INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_applied_jobs_user_apply_date ON applied_jobs(user_id, apply_date_time);
CREATE INDEX idx_applied_jobs_duplicate ON applied_jobs(user_id, company_name, job_title, apply_date_time);

CREATE TABLE external_redirect_jobs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_name VARCHAR(255) NOT NULL,
    redirect_url TEXT NOT NULL,
    redirect_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(80) NOT NULL,
    csv_file_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE question_answers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    normalized_question TEXT NOT NULL,
    answer TEXT NOT NULL,
    confidence_score DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    usage_count INTEGER NOT NULL DEFAULT 0,
    last_used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_question_answers_user ON question_answers(user_id);

CREATE TABLE bot_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    level VARCHAR(30) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_bot_logs_created_at ON bot_logs(created_at DESC);

CREATE TABLE bot_status (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    running BOOLEAN NOT NULL DEFAULT FALSE,
    paused BOOLEAN NOT NULL DEFAULT FALSE,
    captcha_detected BOOLEAN NOT NULL DEFAULT FALSE,
    message TEXT,
    last_run_time TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_bot_status_user UNIQUE(user_id)
);

CREATE TABLE scheduler_config (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    interval_minutes INTEGER NOT NULL DEFAULT 60,
    last_triggered_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_scheduler_config_user UNIQUE(user_id)
);

CREATE TABLE resume_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    keywords TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
