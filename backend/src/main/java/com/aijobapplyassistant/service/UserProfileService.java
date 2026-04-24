package com.aijobapplyassistant.service;

import com.aijobapplyassistant.dto.profile.JobPreferenceRequest;
import com.aijobapplyassistant.dto.profile.JobPreferenceResponse;
import com.aijobapplyassistant.dto.profile.UserProfileRequest;
import com.aijobapplyassistant.dto.profile.UserProfileResponse;
import com.aijobapplyassistant.entity.JobPreference;
import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.exception.ApiException;
import com.aijobapplyassistant.repository.JobPreferenceRepository;
import com.aijobapplyassistant.repository.UserRepository;
import com.aijobapplyassistant.service.security.CredentialCipherService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final JobPreferenceRepository jobPreferenceRepository;
    private final CredentialCipherService credentialCipherService;

    public UserProfileService(UserRepository userRepository, JobPreferenceRepository jobPreferenceRepository,
            CredentialCipherService credentialCipherService) {
        this.userRepository = userRepository;
        this.jobPreferenceRepository = jobPreferenceRepository;
        this.credentialCipherService = credentialCipherService;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, UserProfileRequest request) {
        User user = findByEmail(email);
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setTotalExperience(request.totalExperience());
        user.setCurrentCompany(request.currentCompany());
        user.setCurrentSalary(request.currentSalary());
        user.setExpectedSalary(request.expectedSalary());
        user.setNoticePeriod(request.noticePeriod());
        user.setLocationPreference(request.locationPreference());
        user.setSkills(request.skills());
        user.setPreferredKeywords(request.preferredKeywords());
        user.setNaukriUsername(request.naukriUsername());

        if (request.naukriPassword() != null && !request.naukriPassword().isBlank()) {
            user.setEncryptedNaukriPassword(credentialCipherService.encrypt(request.naukriPassword()));
        }

        user.getJobPreferences().clear();
        List<JobPreference> preferences = new ArrayList<>();
        if (request.preferences() != null) {
            for (JobPreferenceRequest item : request.preferences()) {
                JobPreference preference = new JobPreference();
                preference.setUser(user);
                preference.setTitle(item.title());
                preference.setSkills(item.skills());
                preference.setExperience(item.experience());
                preference.setSalaryRange(item.salaryRange());
                preference.setPreferredLocation(item.preferredLocation());
                preference.setKeywords(item.keywords());
                preference.setPostedTodayOnly(item.postedTodayOnly());
                preference.setEasyApplyFirst(item.easyApplyFirst());
                preference.setRelevantJobsOnly(item.relevantJobsOnly());
                preference.setMinimumScore(item.minimumScore());
                preference.setActive(item.active());
                preferences.add(preference);
            }
        }
        user.getJobPreferences().addAll(preferences);
        userRepository.save(user);
        return toResponse(user);
    }

    public UserProfileResponse getProfile(String email) {
        return toResponse(findByEmail(email));
    }

    @Transactional
    public void updateResumeMetadata(String email, String fileName, String storedPath) {
        User user = findByEmail(email);
        user.setResumeFileName(fileName);
        user.setResumeFilePath(storedPath);
        userRepository.save(user);
    }

    public UserProfileResponse toResponse(User user) {
        List<JobPreferenceResponse> preferences = user.getJobPreferences().stream()
                .map(this::toPreferenceResponse)
                .toList();
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getTotalExperience(),
                user.getCurrentCompany(),
                user.getCurrentSalary(),
                user.getExpectedSalary(),
                user.getNoticePeriod(),
                user.getLocationPreference(),
                user.getSkills(),
                user.getPreferredKeywords(),
                user.getNaukriUsername(),
                user.getResumeFileName(),
                user.getRole(),
                preferences
        );
    }

    public JobPreferenceResponse toPreferenceResponse(JobPreference preference) {
        return new JobPreferenceResponse(
                preference.getId(),
                preference.getTitle(),
                preference.getSkills(),
                preference.getExperience(),
                preference.getSalaryRange(),
                preference.getPreferredLocation(),
                preference.getKeywords(),
                preference.isPostedTodayOnly(),
                preference.isEasyApplyFirst(),
                preference.isRelevantJobsOnly(),
                preference.getMinimumScore(),
                preference.isActive()
        );
    }
}
