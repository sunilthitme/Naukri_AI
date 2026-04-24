package com.aijobapplyassistant.controller;

import com.aijobapplyassistant.dto.common.ApiResponse;
import com.aijobapplyassistant.dto.report.DailyReportResponse;
import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.service.UserProfileService;
import com.aijobapplyassistant.service.report.ReportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final UserProfileService userProfileService;

    public ReportController(ReportService reportService, UserProfileService userProfileService) {
        this.reportService = reportService;
        this.userProfileService = userProfileService;
    }

    @GetMapping("/daily")
    public ApiResponse<DailyReportResponse> dailyReport(@AuthenticationPrincipal String email,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        User user = userProfileService.findByEmail(email);
        return ApiResponse.ok(reportService.generateDailyReport(user, date == null ? LocalDate.now() : date), "Report generated");
    }
}
