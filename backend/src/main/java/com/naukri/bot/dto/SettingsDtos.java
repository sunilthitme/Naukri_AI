package com.naukri.bot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public final class SettingsDtos {
    private SettingsDtos() {
    }

    public record NaukriCredentialsRequest(
            @Email @NotBlank String email,
            @NotBlank String password,
            String resumePath
    ) {
    }

    public record NaukriCredentialsResponse(String email, String resumePath, Instant updatedAt) {
    }

    public record JobFilterRequest(
            String keywords,
            String experience,
            String location,
            String salary,
            String workMode,
            String freshness,
            List<String> preferredCompanies,
            List<String> blacklistedCompanies,
            boolean autoApply,
            boolean externalCareerApply,
            @Min(1) @Max(500) int dailyApplyLimit,
            boolean easyApplyOnly,
            @Min(1) @Max(365) int duplicatePreventionDays
    ) {
    }

    public record JobFilterResponse(
            String keywords,
            String experience,
            String location,
            String salary,
            String workMode,
            String freshness,
            List<String> preferredCompanies,
            List<String> blacklistedCompanies,
            boolean autoApply,
            boolean externalCareerApply,
            int dailyApplyLimit,
            boolean easyApplyOnly,
            int duplicatePreventionDays,
            Instant updatedAt
    ) {
    }

    public record ResumeProfileRequest(@NotBlank String name, @NotBlank String filePath, List<String> keywords, boolean active) {
    }

    public record ResumeProfileResponse(Long id, String name, String filePath, List<String> keywords, boolean active) {
    }

    public record SchedulerConfigRequest(boolean enabled, @Min(5) @Max(1440) int intervalMinutes) {
    }

    public record SchedulerConfigResponse(boolean enabled, int intervalMinutes, Instant lastTriggeredAt) {
    }
}
