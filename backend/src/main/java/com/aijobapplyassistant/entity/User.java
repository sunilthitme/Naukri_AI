package com.aijobapplyassistant.entity;

import com.aijobapplyassistant.entity.enums.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean active = true;

    private String phoneNumber;
    private Integer totalExperience;
    private String currentCompany;
    private String currentSalary;
    private String expectedSalary;
    private String noticePeriod;
    private String locationPreference;

    @Column(length = 4000)
    private String skills;

    @Column(length = 4000)
    private String preferredKeywords;

    private String resumeFileName;
    private String resumeFilePath;
    private String naukriUsername;

    @Column(length = 512)
    private String encryptedNaukriPassword;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<JobPreference> jobPreferences = new ArrayList<>();

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getTotalExperience() {
        return totalExperience;
    }

    public void setTotalExperience(Integer totalExperience) {
        this.totalExperience = totalExperience;
    }

    public String getCurrentCompany() {
        return currentCompany;
    }

    public void setCurrentCompany(String currentCompany) {
        this.currentCompany = currentCompany;
    }

    public String getCurrentSalary() {
        return currentSalary;
    }

    public void setCurrentSalary(String currentSalary) {
        this.currentSalary = currentSalary;
    }

    public String getExpectedSalary() {
        return expectedSalary;
    }

    public void setExpectedSalary(String expectedSalary) {
        this.expectedSalary = expectedSalary;
    }

    public String getNoticePeriod() {
        return noticePeriod;
    }

    public void setNoticePeriod(String noticePeriod) {
        this.noticePeriod = noticePeriod;
    }

    public String getLocationPreference() {
        return locationPreference;
    }

    public void setLocationPreference(String locationPreference) {
        this.locationPreference = locationPreference;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getPreferredKeywords() {
        return preferredKeywords;
    }

    public void setPreferredKeywords(String preferredKeywords) {
        this.preferredKeywords = preferredKeywords;
    }

    public String getResumeFileName() {
        return resumeFileName;
    }

    public void setResumeFileName(String resumeFileName) {
        this.resumeFileName = resumeFileName;
    }

    public String getResumeFilePath() {
        return resumeFilePath;
    }

    public void setResumeFilePath(String resumeFilePath) {
        this.resumeFilePath = resumeFilePath;
    }

    public String getNaukriUsername() {
        return naukriUsername;
    }

    public void setNaukriUsername(String naukriUsername) {
        this.naukriUsername = naukriUsername;
    }

    public String getEncryptedNaukriPassword() {
        return encryptedNaukriPassword;
    }

    public void setEncryptedNaukriPassword(String encryptedNaukriPassword) {
        this.encryptedNaukriPassword = encryptedNaukriPassword;
    }

    public List<JobPreference> getJobPreferences() {
        return jobPreferences;
    }
}
