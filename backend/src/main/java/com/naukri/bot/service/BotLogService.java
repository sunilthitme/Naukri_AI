package com.naukri.bot.service;

import com.naukri.bot.domain.BotLog;
import com.naukri.bot.domain.LogLevel;
import com.naukri.bot.domain.User;
import com.naukri.bot.dto.LogDtos.BotLogResponse;
import com.naukri.bot.repository.BotLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BotLogService {
    private final BotLogRepository botLogRepository;

    public void info(User user, String message) {
        save(user, LogLevel.INFO, message);
    }

    public void warn(User user, String message) {
        save(user, LogLevel.WARN, message);
    }

    public void error(User user, String message) {
        save(user, LogLevel.ERROR, message);
    }

    public List<BotLogResponse> latest(User user, int limit) {
        return botLogRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, Math.min(limit, 500))).stream()
                .map(log -> new BotLogResponse(log.getId(), log.getLevel(), log.getMessage(), log.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void clear(User user) {
        botLogRepository.deleteByUser(user);
    }

    private void save(User user, LogLevel level, String message) {
        BotLog log = new BotLog();
        log.setUser(user);
        log.setLevel(level);
        log.setMessage(message);
        botLogRepository.save(log);
    }
}
