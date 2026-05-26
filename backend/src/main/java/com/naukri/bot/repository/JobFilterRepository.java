package com.naukri.bot.repository;

import com.naukri.bot.domain.JobFilter;
import com.naukri.bot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobFilterRepository extends JpaRepository<JobFilter, Long> {
    Optional<JobFilter> findByUser(User user);
}
