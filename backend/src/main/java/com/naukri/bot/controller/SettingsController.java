package com.naukri.bot.controller;

import com.naukri.bot.dto.SettingsDtos.JobFilterRequest;
import com.naukri.bot.dto.SettingsDtos.JobFilterResponse;
import com.naukri.bot.dto.SettingsDtos.NaukriCredentialsRequest;
import com.naukri.bot.dto.SettingsDtos.NaukriCredentialsResponse;
import com.naukri.bot.dto.SettingsDtos.ResumeProfileRequest;
import com.naukri.bot.dto.SettingsDtos.ResumeProfileResponse;
import com.naukri.bot.dto.SettingsDtos.SchedulerConfigRequest;
import com.naukri.bot.dto.SettingsDtos.SchedulerConfigResponse;
import com.naukri.bot.service.CurrentUserService;
import com.naukri.bot.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {
    private final SettingsService settingsService;
    private final CurrentUserService currentUserService;

    @GetMapping("/credentials")
    public NaukriCredentialsResponse getCredentials() {
        return settingsService.getCredentials(currentUserService.currentUser());
    }

    @PostMapping("/credentials")
    public NaukriCredentialsResponse saveCredentials(@Valid @RequestBody NaukriCredentialsRequest request) {
        return settingsService.saveCredentials(currentUserService.currentUser(), request);
    }

    @GetMapping("/filters")
    public JobFilterResponse getFilter() {
        return settingsService.getFilter(currentUserService.currentUser());
    }

    @PostMapping("/filters")
    public JobFilterResponse saveFilter(@Valid @RequestBody JobFilterRequest request) {
        return settingsService.saveFilter(currentUserService.currentUser(), request);
    }

    @GetMapping("/resumes")
    public List<ResumeProfileResponse> resumes() {
        return settingsService.listResumes(currentUserService.currentUser());
    }

    @PostMapping("/resumes")
    public ResumeProfileResponse saveResume(@Valid @RequestBody ResumeProfileRequest request) {
        return settingsService.saveResume(currentUserService.currentUser(), request);
    }

    @GetMapping("/scheduler")
    public SchedulerConfigResponse scheduler() {
        return settingsService.getScheduler(currentUserService.currentUser());
    }

    @PostMapping("/scheduler")
    public SchedulerConfigResponse saveScheduler(@Valid @RequestBody SchedulerConfigRequest request) {
        return settingsService.saveScheduler(currentUserService.currentUser(), request);
    }
}
