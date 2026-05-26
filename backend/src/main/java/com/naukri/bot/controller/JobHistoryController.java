package com.naukri.bot.controller;

import com.naukri.bot.dto.HistoryDtos.AppliedJobResponse;
import com.naukri.bot.service.CurrentUserService;
import com.naukri.bot.service.JobHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobHistoryController {
    private final JobHistoryService jobHistoryService;
    private final CurrentUserService currentUserService;

    @GetMapping("/history")
    public List<AppliedJobResponse> history(@RequestParam(defaultValue = "100") int limit) {
        return jobHistoryService.history(currentUserService.currentUser(), limit);
    }
}
