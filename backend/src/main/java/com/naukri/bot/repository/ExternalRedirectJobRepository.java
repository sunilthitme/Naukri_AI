package com.naukri.bot.repository;

import com.naukri.bot.domain.ExternalRedirectJob;
import com.naukri.bot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalRedirectJobRepository extends JpaRepository<ExternalRedirectJob, Long> {
    long countByUser(User user);
}
