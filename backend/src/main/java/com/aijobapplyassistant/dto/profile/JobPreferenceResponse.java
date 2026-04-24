package com.aijobapplyassistant.dto.profile;

public record JobPreferenceResponse(
        Long id,
        String title,
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
