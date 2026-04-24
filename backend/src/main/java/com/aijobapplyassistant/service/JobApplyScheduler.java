package com.aijobapplyassistant.service;

import com.aijobapplyassistant.config.ApplicationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobApplyScheduler {

    private final ApplicationProperties properties;
    private final JobAutomationService jobAutomationService;

    public JobApplyScheduler(ApplicationProperties properties, JobAutomationService jobAutomationService) {
        this.properties = properties;
        this.jobAutomationService = jobAutomationService;
    }

    @Scheduled(cron = "${app.scheduler.cron:0 0 9 * * MON-FRI}")
    public void runDailyAutomation() {
        if (properties.getScheduler().isEnabled()) {
            jobAutomationService.startScheduledRuns();
        }
    }
}
