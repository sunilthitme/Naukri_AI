package com.naukri.bot.dto;

import com.naukri.bot.domain.LogLevel;

import java.time.Instant;

public final class LogDtos {
    private LogDtos() {
    }

    public record BotLogResponse(Long id, LogLevel level, String message, Instant createdAt) {
    }
}
