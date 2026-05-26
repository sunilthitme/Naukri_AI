package com.naukri.bot.service;

import com.naukri.bot.domain.SchedulerConfig;
import com.naukri.bot.repository.SchedulerConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class SchedulerService {
    private final SchedulerConfigRepository schedulerConfigRepository;
    private final BotOrchestratorService botOrchestratorService;

    @Transactional
    @Scheduled(fixedDelayString = "${app.scheduler.poll-delay-ms:60000}")
    public void triggerDueRuns() {
        for (SchedulerConfig config : schedulerConfigRepository.findByEnabledTrue()) {
            Instant dueAfter = config.getLastTriggeredAt() == null
                    ? Instant.EPOCH
                    : config.getLastTriggeredAt().plus(config.getIntervalMinutes(), ChronoUnit.MINUTES);
            if (Instant.now().isAfter(dueAfter)) {
                config.setLastTriggeredAt(Instant.now());
                schedulerConfigRepository.save(config);
                botOrchestratorService.start(config.getUser());
            }
        }
    }
}
