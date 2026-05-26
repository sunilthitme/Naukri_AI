package com.naukri.bot.automation.model;

public enum ApplyStatus {
    SUCCESS,
    FAILED,
    SKIPPED_DUPLICATE,
    ALREADY_APPLIED,
    EXTERNAL_REDIRECT,
    CAPTCHA_DETECTED,
    QUESTION_NEEDS_ANSWER,
    DRY_RUN
}
