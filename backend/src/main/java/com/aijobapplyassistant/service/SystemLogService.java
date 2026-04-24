package com.aijobapplyassistant.service;

import com.aijobapplyassistant.dto.question.QuestionResponse;
import com.aijobapplyassistant.entity.SystemLogEntry;
import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.entity.enums.LogLevel;
import com.aijobapplyassistant.repository.SystemLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SystemLogService {

    private static final Logger logger = LoggerFactory.getLogger(SystemLogService.class);
    private final SystemLogRepository systemLogRepository;

    public SystemLogService(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    public void info(User user, String source, String message) {
        logger.info("[{}] {}", source, message);
        persist(user, LogLevel.INFO, source, message, null);
    }

    public void warn(User user, String source, String message, String details) {
        logger.warn("[{}] {}", source, message);
        persist(user, LogLevel.WARN, source, message, details);
    }

    public void error(User user, String source, String message, String details) {
        logger.error("[{}] {}", source, message);
        persist(user, LogLevel.ERROR, source, message, details);
    }

    public List<SystemLogEntry> findRecentLogs(Long userId) {
        return systemLogRepository.findTop200ByUserIdOrderByOccurredAtDesc(userId);
    }

    private void persist(User user, LogLevel level, String source, String message, String details) {
        SystemLogEntry entry = new SystemLogEntry();
        entry.setUser(user);
        entry.setLevel(level);
        entry.setSource(source);
        entry.setMessage(message);
        entry.setDetails(details);
        entry.setOccurredAt(LocalDateTime.now());
        systemLogRepository.save(entry);
    }
}
