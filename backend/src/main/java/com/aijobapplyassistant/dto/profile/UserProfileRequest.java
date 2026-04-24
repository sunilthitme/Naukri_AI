package com.aijobapplyassistant.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UserProfileRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email,
        @NotBlank String phoneNumber,
        Integer totalExperience,
        String currentCompany,
        String currentSalary,
        String expectedSalary,
        String noticePeriod,
        String locationPreference,
        String skills,
        String preferredKeywords,
        String naukriUsername,
        String naukriPassword,
        List<JobPreferenceRequest> preferences
) {
}
