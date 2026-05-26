package com.naukri.bot.controller;

import com.naukri.bot.dto.LogDtos.BotLogResponse;
import com.naukri.bot.service.BotLogService;
import com.naukri.bot.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogsController {
    private final BotLogService botLogService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<BotLogResponse> logs(@RequestParam(defaultValue = "200") int limit) {
        return botLogService.latest(currentUserService.currentUser(), limit);
    }
}
