package com.naukri.bot.service;

import com.naukri.bot.domain.BotRunStatus;
import com.naukri.bot.domain.BotStatus;
import com.naukri.bot.domain.User;
import com.naukri.bot.dto.BotDtos.BotStatusResponse;
import com.naukri.bot.repository.BotStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BotStatusService {
    private final BotStatusRepository botStatusRepository;

    @Transactional
    public BotStatus set(User user, BotRunStatus status, boolean running, boolean paused, boolean captchaDetected, String message) {
        BotStatus botStatus = botStatusRepository.findByUser(user).orElseGet(BotStatus::new);
        botStatus.setUser(user);
        botStatus.setStatus(status);
        botStatus.setRunning(running);
        botStatus.setPaused(paused);
        botStatus.setCaptchaDetected(captchaDetected);
        botStatus.setMessage(message);
        botStatus.setUpdatedAt(Instant.now());
        if (status == BotRunStatus.RUNNING || status == BotRunStatus.STOPPED || status == BotRunStatus.FAILED) {
            botStatus.setLastRunTime(Instant.now());
        }
        return botStatusRepository.save(botStatus);
    }

    @Transactional
    public BotStatus activity(User user, String message) {
        BotStatus botStatus = botStatusRepository.findByUser(user).orElseGet(BotStatus::new);
        botStatus.setUser(user);
        if (botStatus.getStatus() == null || botStatus.getStatus() == BotRunStatus.IDLE) {
            botStatus.setStatus(BotRunStatus.RUNNING);
            botStatus.setRunning(true);
        }
        botStatus.setMessage(message);
        botStatus.setUpdatedAt(Instant.now());
        return botStatusRepository.save(botStatus);
    }

    public BotStatus get(User user) {
        return botStatusRepository.findByUser(user).orElseGet(() -> {
            BotStatus status = new BotStatus();
            status.setUser(user);
            status.setStatus(BotRunStatus.IDLE);
            return status;
        });
    }

    public BotStatusResponse response(User user) {
        BotStatus status = get(user);
        return new BotStatusResponse(status.getStatus(), status.isRunning(), status.isPaused(),
                status.isCaptchaDetected(), status.getMessage(), status.getLastRunTime(), status.getUpdatedAt());
    }
}
