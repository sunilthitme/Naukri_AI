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
@Table(name = "bot_status")
public class BotStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BotRunStatus status = BotRunStatus.IDLE;

    @Column(nullable = false)
    private boolean running;

    @Column(nullable = false)
    private boolean paused;

    @Column(name = "captcha_detected", nullable = false)
    private boolean captchaDetected;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "last_run_time")
    private Instant lastRunTime;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
