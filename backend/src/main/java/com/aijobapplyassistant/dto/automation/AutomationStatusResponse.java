package com.aijobapplyassistant.dto.automation;

import com.aijobapplyassistant.entity.enums.AutomationRunStatus;
import java.time.LocalDateTime;

public record AutomationStatusResponse(
        Long runId,
        AutomationRunStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        int jobsSearched,
        int jobsApplied,
        int jobsSkipped,
        int jobsFailed,
        int credentialsCreated,
        String summary
) {
}
