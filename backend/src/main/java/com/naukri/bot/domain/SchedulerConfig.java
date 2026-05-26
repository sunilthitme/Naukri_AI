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
@Table(name = "scheduler_config")
public class SchedulerConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "interval_minutes", nullable = false)
    private int intervalMinutes = 60;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
