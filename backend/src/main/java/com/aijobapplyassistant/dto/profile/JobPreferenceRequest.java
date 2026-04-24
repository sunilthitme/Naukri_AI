package com.aijobapplyassistant.dto.profile;

import jakarta.validation.constraints.NotBlank;

public record JobPreferenceRequest(
        Long id,
        @NotBlank String title,
        String skills,
        Integer experience,
        String salaryRange,
        String preferredLocation,
        String keywords,
        boolean postedTodayOnly,
        boolean easyApplyFirst,
        boolean relevantJobsOnly,
        int minimumScore,
        boolean active
) {
}
