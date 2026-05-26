package com.naukri.bot.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job_filters")
public class JobFilter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    private String experience;
    private String location;
    private String salary;

    @Column(name = "work_mode")
    private String workMode;

    private String freshness;

    @Column(name = "preferred_companies", columnDefinition = "TEXT")
    private String preferredCompanies;

    @Column(name = "blacklisted_companies", columnDefinition = "TEXT")
    private String blacklistedCompanies;

    @Column(name = "auto_apply", nullable = false)
    private boolean autoApply = true;

    @Column(name = "external_career_apply", nullable = false)
    private boolean externalCareerApply;

    @Column(name = "daily_apply_limit", nullable = false)
    private int dailyApplyLimit = 20;

    @Column(name = "easy_apply_only", nullable = false)
    private boolean easyApplyOnly = true;

    @Column(name = "duplicate_prevention_days", nullable = false)
    private int duplicatePreventionDays = 30;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
