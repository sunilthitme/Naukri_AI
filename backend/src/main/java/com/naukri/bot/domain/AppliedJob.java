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
@Table(name = "applied_jobs")
public class AppliedJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "apply_date_time", nullable = false)
    private Instant applyDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplyStatus status;

    @Column(name = "job_url", columnDefinition = "TEXT")
    private String jobUrl;

    private String experience;
    private String salary;
    private String location;

    @Column(name = "redirected_external_site", nullable = false)
    private boolean redirectedExternalSite;

    @Column(name = "csv_file_name")
    private String csvFileName;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "match_score")
    private Double matchScore;

    @Column(name = "screenshot_path", columnDefinition = "TEXT")
    private String screenshotPath;

    @Column(nullable = false)
    private int attempts = 1;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
