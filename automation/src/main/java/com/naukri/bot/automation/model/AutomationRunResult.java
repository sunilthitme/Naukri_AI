package com.naukri.bot.automation.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AutomationRunResult {
    private final Instant startedAt = Instant.now();
    private Instant finishedAt;
    private boolean captchaDetected;
    private boolean loginFailed;
    private String failureReason;
    private final List<JobApplicationResult> jobResults = new ArrayList<>();
    private final List<ExternalRedirectResult> externalRedirects = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void finish() {
        this.finishedAt = Instant.now();
    }

    public boolean isCaptchaDetected() {
        return captchaDetected;
    }

    public void setCaptchaDetected(boolean captchaDetected) {
        this.captchaDetected = captchaDetected;
    }

    public boolean isLoginFailed() {
        return loginFailed;
    }

    public void setLoginFailed(boolean loginFailed) {
        this.loginFailed = loginFailed;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public List<JobApplicationResult> getJobResults() {
        return jobResults;
    }

    public List<ExternalRedirectResult> getExternalRedirects() {
        return externalRedirects;
    }

    public List<String> getMessages() {
        return messages;
    }
}
