package com.aijobapplyassistant.dto.report;

import java.time.LocalDate;
import java.util.List;

public record DailyReportResponse(
        LocalDate date,
        long totalSearched,
        long totalApplied,
        long totalSkipped,
        long totalFailed,
        long credentialsCreated,
        String pdfPath,
        String excelPath,
        String csvPath,
        List<ReportRowResponse> rows
) {
}
