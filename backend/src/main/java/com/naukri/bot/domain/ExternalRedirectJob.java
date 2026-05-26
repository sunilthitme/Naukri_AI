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
@Table(name = "external_redirect_jobs")
public class ExternalRedirectJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "redirect_url", nullable = false, columnDefinition = "TEXT")
    private String redirectUrl;

    @Column(name = "redirect_date_time", nullable = false)
    private Instant redirectDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplyStatus status;

    @Column(name = "csv_file_name")
    private String csvFileName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
