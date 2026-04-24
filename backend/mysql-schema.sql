CREATE DATABASE IF NOT EXISTS jobapplydb;
USE jobapplydb;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BIT NOT NULL,
    phone_number VARCHAR(255),
    total_experience INT,
    current_company VARCHAR(255),
    current_salary VARCHAR(255),
    expected_salary VARCHAR(255),
    notice_period VARCHAR(255),
    location_preference VARCHAR(255),
    skills TEXT,
    preferred_keywords TEXT,
    resume_file_name VARCHAR(255),
    resume_file_path VARCHAR(255),
    naukri_username VARCHAR(255),
    encrypted_naukri_password VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS job_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    skills VARCHAR(255),
    experience INT,
    salary_range VARCHAR(255),
    preferred_location VARCHAR(255),
    keywords TEXT,
    posted_today_only BIT NOT NULL,
    easy_apply_first BIT NOT NULL,
    relevant_jobs_only BIT NOT NULL,
    minimum_score INT NOT NULL,
    active BIT NOT NULL,
    CONSTRAINT fk_job_preferences_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS portal_credentials (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    portal_url VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    encrypted_password VARCHAR(1024) NOT NULL,
    portal_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_portal_credentials_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    company VARCHAR(255),
    title VARCHAR(255),
    portal VARCHAR(255),
    portal_url VARCHAR(255),
    job_url VARCHAR(255),
    relevance_score INT NOT NULL,
    status VARCHAR(30) NOT NULL,
    remarks TEXT,
    applied_date DATETIME,
    external_reference_id VARCHAR(255),
    CONSTRAINT fk_applications_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS questions_queue (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    application_id BIGINT,
    question VARCHAR(1000) NOT NULL,
    field_label VARCHAR(255),
    source_portal VARCHAR(255),
    answer VARCHAR(2000),
    status VARCHAR(20) NOT NULL,
    answered_at DATETIME,
    CONSTRAINT fk_questions_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_questions_application FOREIGN KEY (application_id) REFERENCES applications(id)
);

CREATE TABLE IF NOT EXISTS logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT,
    level VARCHAR(20) NOT NULL,
    source VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    details TEXT,
    occurred_at DATETIME NOT NULL,
    CONSTRAINT fk_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS automation_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    started_at DATETIME,
    ended_at DATETIME,
    jobs_searched INT NOT NULL,
    jobs_applied INT NOT NULL,
    jobs_skipped INT NOT NULL,
    jobs_failed INT NOT NULL,
    credentials_created INT NOT NULL,
    summary TEXT,
    CONSTRAINT fk_automation_runs_user FOREIGN KEY (user_id) REFERENCES users(id)
);
