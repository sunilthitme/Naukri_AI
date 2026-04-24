package com.aijobapplyassistant.controller;

import com.aijobapplyassistant.dto.automation.AutomationStatusResponse;
import com.aijobapplyassistant.dto.automation.DashboardSummaryResponse;
import com.aijobapplyassistant.dto.common.ApiResponse;
import com.aijobapplyassistant.service.JobAutomationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/automation")
public class JobAutomationController {

    private final JobAutomationService jobAutomationService;

    public JobAutomationController(JobAutomationService jobAutomationService) {
        this.jobAutomationService = jobAutomationService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<DashboardSummaryResponse> dashboard(@AuthenticationPrincipal String email) {
        return ApiResponse.ok(jobAutomationService.getDashboard(email), "Dashboard loaded");
    }

    @GetMapping("/status")
    public ApiResponse<AutomationStatusResponse> status(@AuthenticationPrincipal String email) {
        return ApiResponse.ok(jobAutomationService.getStatus(email), "Status loaded");
    }

    @PostMapping("/start")
    public ApiResponse<AutomationStatusResponse> start(@AuthenticationPrincipal String email) {
        return ApiResponse.ok(jobAutomationService.startManualRun(email), "Automation started");
    }

    @PostMapping("/stop")
    public ApiResponse<AutomationStatusResponse> stop(@AuthenticationPrincipal String email) {
        return ApiResponse.ok(jobAutomationService.stopManualRun(email), "Stop requested");
    }
}
