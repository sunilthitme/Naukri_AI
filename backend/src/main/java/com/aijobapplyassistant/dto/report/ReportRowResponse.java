package com.aijobapplyassistant.dto.report;

import com.aijobapplyassistant.entity.enums.ApplicationStatus;
import java.time.LocalDateTime;

public record ReportRowResponse(
        LocalDateTime appliedDate,
        String company,
        String title,
        String portal,
        ApplicationStatus status,
        String remarks,
        int relevanceScore
) {
}
