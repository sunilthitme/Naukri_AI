package com.naukri.bot.repository;

import com.naukri.bot.domain.AppliedJob;
import com.naukri.bot.domain.ApplyStatus;
import com.naukri.bot.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface AppliedJobRepository extends JpaRepository<AppliedJob, Long> {
    List<AppliedJob> findByUserOrderByApplyDateTimeDesc(User user, Pageable pageable);

    long countByUser(User user);

    long countByUserAndStatus(User user, ApplyStatus status);

    long countByUserAndApplyDateTimeBetween(User user, Instant start, Instant end);

    boolean existsByUserAndCompanyNameIgnoreCaseAndJobTitleIgnoreCaseAndApplyDateTimeAfter(User user, String companyName, String jobTitle, Instant after);

    @Query("select a from AppliedJob a where a.user = :user and a.applyDateTime between :start and :end order by a.applyDateTime desc")
    List<AppliedJob> findForDateRange(User user, Instant start, Instant end);
}
