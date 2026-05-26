package com.naukri.bot.dto;

import com.naukri.bot.domain.BotRunStatus;

import java.time.Instant;

public final class BotDtos {
    private BotDtos() {
    }

    public record BotCommandResponse(String message, BotRunStatus status) {
    }

    public record BotStatusResponse(
            BotRunStatus status,
            boolean running,
            boolean paused,
            boolean captchaDetected,
            String message,
            Instant lastRunTime,
            Instant updatedAt
    ) {
    }
}
