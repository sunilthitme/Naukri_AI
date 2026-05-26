package com.naukri.bot.repository;

import com.naukri.bot.domain.SchedulerConfig;
import com.naukri.bot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchedulerConfigRepository extends JpaRepository<SchedulerConfig, Long> {
    Optional<SchedulerConfig> findByUser(User user);

    List<SchedulerConfig> findByEnabledTrue();
}
