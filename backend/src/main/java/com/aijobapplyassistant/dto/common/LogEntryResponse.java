package com.aijobapplyassistant.dto.common;

import com.aijobapplyassistant.entity.enums.LogLevel;
import java.time.LocalDateTime;

public record LogEntryResponse(
        Long id,
        LogLevel level,
        String source,
        String message,
        String details,
        LocalDateTime occurredAt
) {
}
