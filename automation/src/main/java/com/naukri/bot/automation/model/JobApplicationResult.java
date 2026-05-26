package com.naukri.bot.automation.model;

import java.time.Instant;

public record JobApplicationResult(
        String companyName,
        String jobTitle,
        Instant applyDateTime,
        ApplyStatus status,
        String jobUrl,
        String experience,
        String salary,
        String location,
        boolean redirectedExternalSite,
        String failureReason,
        String screenshotPath,
        double matchScore,
        int attempts
) {
}
