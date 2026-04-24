package com.aijobapplyassistant.dto.profile;

import com.aijobapplyassistant.entity.enums.Role;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        String phoneNumber,
        Integer totalExperience,
        String currentCompany,
        String currentSalary,
        String expectedSalary,
        String noticePeriod,
        String locationPreference,
        String skills,
        String preferredKeywords,
        String naukriUsername,
        String resumeFileName,
        Role role,
        List<JobPreferenceResponse> preferences
) {
}
