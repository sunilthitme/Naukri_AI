package com.aijobapplyassistant.controller;

import com.aijobapplyassistant.dto.common.ApiResponse;
import com.aijobapplyassistant.dto.common.LogEntryResponse;
import com.aijobapplyassistant.entity.SystemLogEntry;
import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.service.SystemLogService;
import com.aijobapplyassistant.service.UserProfileService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final SystemLogService systemLogService;
    private final UserProfileService userProfileService;

    public LogController(SystemLogService systemLogService, UserProfileService userProfileService) {
        this.systemLogService = systemLogService;
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ApiResponse<List<LogEntryResponse>> recentLogs(@AuthenticationPrincipal String email) {
        User user = userProfileService.findByEmail(email);
        List<LogEntryResponse> logs = systemLogService.findRecentLogs(user.getId()).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.ok(logs, "Logs loaded");
    }

    private LogEntryResponse toResponse(SystemLogEntry item) {
        return new LogEntryResponse(item.getId(), item.getLevel(), item.getSource(), item.getMessage(), item.getDetails(), item.getOccurredAt());
    }
}
