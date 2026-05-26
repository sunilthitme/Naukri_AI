package com.naukri.bot.repository;

import com.naukri.bot.domain.BotStatus;
import com.naukri.bot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BotStatusRepository extends JpaRepository<BotStatus, Long> {
    Optional<BotStatus> findByUser(User user);
}
