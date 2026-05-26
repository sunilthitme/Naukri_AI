package com.naukri.bot.repository;

import com.naukri.bot.domain.ResumeProfile;
import com.naukri.bot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeProfileRepository extends JpaRepository<ResumeProfile, Long> {
    List<ResumeProfile> findByUserAndActiveTrue(User user);
}
