package com.aijobapplyassistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "job_preferences")
public class JobPreference extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    private String skills;
    private Integer experience;
    private String salaryRange;
    private String preferredLocation;

    @Column(length = 4000)
    private String keywords;

    private boolean postedTodayOnly = true;
    private boolean easyApplyFirst = true;
    private boolean relevantJobsOnly = true;
    private int minimumScore = 70;
    private boolean active = true;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public String getSalaryRange() {
        return salaryRange;
    }

    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
    }

    public String getPreferredLocation() {
        return preferredLocation;
    }

    public void setPreferredLocation(String preferredLocation) {
        this.preferredLocation = preferredLocation;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public boolean isPostedTodayOnly() {
        return postedTodayOnly;
    }

    public void setPostedTodayOnly(boolean postedTodayOnly) {
        this.postedTodayOnly = postedTodayOnly;
    }

    public boolean isEasyApplyFirst() {
        return easyApplyFirst;
    }

    public void setEasyApplyFirst(boolean easyApplyFirst) {
        this.easyApplyFirst = easyApplyFirst;
    }

    public boolean isRelevantJobsOnly() {
        return relevantJobsOnly;
    }

    public void setRelevantJobsOnly(boolean relevantJobsOnly) {
        this.relevantJobsOnly = relevantJobsOnly;
    }

    public int getMinimumScore() {
        return minimumScore;
    }

    public void setMinimumScore(int minimumScore) {
        this.minimumScore = minimumScore;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
