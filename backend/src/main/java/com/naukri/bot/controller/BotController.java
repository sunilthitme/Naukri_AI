package com.naukri.bot.controller;

import com.naukri.bot.dto.BotDtos.BotCommandResponse;
import com.naukri.bot.dto.BotDtos.BotStatusResponse;
import com.naukri.bot.service.BotOrchestratorService;
import com.naukri.bot.service.BotStatusService;
import com.naukri.bot.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotController {
    private final BotOrchestratorService botOrchestratorService;
    private final BotStatusService botStatusService;
    private final CurrentUserService currentUserService;

    @GetMapping("/status")
    public BotStatusResponse status() {
        return botStatusService.response(currentUserService.currentUser());
    }

    @PostMapping("/start")
    public BotCommandResponse start() {
        return botOrchestratorService.start(currentUserService.currentUser());
    }

    @PostMapping("/stop")
    public BotCommandResponse stop() {
        return botOrchestratorService.stop(currentUserService.currentUser());
    }

    @PostMapping("/pause")
    public BotCommandResponse pause() {
        return botOrchestratorService.pause(currentUserService.currentUser());
    }

    @PostMapping("/resume")
    public BotCommandResponse resume() {
        return botOrchestratorService.resume(currentUserService.currentUser());
    }

    @PostMapping("/manual-login")
    public BotCommandResponse manualLogin() {
        return botOrchestratorService.manualLoginAndContinue(currentUserService.currentUser());
    }

    @PostMapping("/test-login")
    public BotCommandResponse testLogin() {
        return botOrchestratorService.testLogin(currentUserService.currentUser());
    }

    @PostMapping("/test-apply")
    public BotCommandResponse testApply() {
        return botOrchestratorService.testApply(currentUserService.currentUser());
    }
}
