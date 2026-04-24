package com.aijobapplyassistant.dto.automation;

import com.aijobapplyassistant.dto.report.ReportRowResponse;
import java.util.List;

public record DashboardSummaryResponse(
        AutomationStatusResponse latestRun,
        long totalAppliedToday,
        long totalFailedToday,
        long totalSkippedToday,
        int openQuestions,
        List<ReportRowResponse> recentApplications
) {
}
