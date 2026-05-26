package com.naukri.bot.repository;

import com.naukri.bot.domain.BotLog;
import com.naukri.bot.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BotLogRepository extends JpaRepository<BotLog, Long> {
    List<BotLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
