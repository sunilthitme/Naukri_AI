package com.aijobapplyassistant.entity;

import com.aijobapplyassistant.entity.enums.AutomationRunStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "automation_runs")
public class AutomationRun extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AutomationRunStatus status = AutomationRunStatus.IDLE;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private int jobsSearched;
    private int jobsApplied;
    private int jobsSkipped;
    private int jobsFailed;
    private int credentialsCreated;

    @Column(length = 4000)
    private String summary;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public AutomationRunStatus getStatus() {
        return status;
    }

    public void setStatus(AutomationRunStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public int getJobsSearched() {
        return jobsSearched;
    }

    public void setJobsSearched(int jobsSearched) {
        this.jobsSearched = jobsSearched;
    }

    public int getJobsApplied() {
        return jobsApplied;
    }

    public void setJobsApplied(int jobsApplied) {
        this.jobsApplied = jobsApplied;
    }

    public int getJobsSkipped() {
        return jobsSkipped;
    }

    public void setJobsSkipped(int jobsSkipped) {
        this.jobsSkipped = jobsSkipped;
    }

    public int getJobsFailed() {
        return jobsFailed;
    }

    public void setJobsFailed(int jobsFailed) {
        this.jobsFailed = jobsFailed;
    }

    public int getCredentialsCreated() {
        return credentialsCreated;
    }

    public void setCredentialsCreated(int credentialsCreated) {
        this.credentialsCreated = credentialsCreated;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
