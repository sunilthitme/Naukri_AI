package com.naukri.bot.dto;

import com.naukri.bot.domain.BotRunStatus;

import java.time.Instant;

public final class DashboardDtos {
    private DashboardDtos() {
    }

    public record DashboardResponse(
            long totalAppliedJobs,
            long totalFailedJobs,
            long externalRedirectJobs,
            long todaysApplications,
            BotRunStatus botStatus,
            boolean botRunning,
            String botMessage,
            Instant lastRunTime
    ) {
    }
}
