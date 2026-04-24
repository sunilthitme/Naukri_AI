package com.aijobapplyassistant.repository;

import com.aijobapplyassistant.entity.JobApplication;
import com.aijobapplyassistant.entity.enums.ApplicationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    List<JobApplication> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    List<JobApplication> findByUserIdAndAppliedDateBetweenOrderByAppliedDateDesc(Long userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndStatusAndAppliedDateBetween(Long userId, ApplicationStatus status, LocalDateTime start, LocalDateTime end);

    Optional<JobApplication> findByUserIdAndJobUrl(Long userId, String jobUrl);
}
