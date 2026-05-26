package com.naukri.bot.dto;

import com.naukri.bot.domain.ApplyStatus;

import java.time.Instant;

public final class HistoryDtos {
    private HistoryDtos() {
    }

    public record AppliedJobResponse(
            Long id,
            String companyName,
            String jobTitle,
            Instant applyDate,
            ApplyStatus status,
            boolean redirectedExternalSite,
            String csvFileName,
            String failureReason,
            String jobUrl,
            Double matchScore
    ) {
    }
}
