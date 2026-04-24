package com.aijobapplyassistant.controller;

import com.aijobapplyassistant.dto.common.ApiResponse;
import com.aijobapplyassistant.dto.profile.ResumeUploadResponse;
import com.aijobapplyassistant.dto.profile.UserProfileRequest;
import com.aijobapplyassistant.dto.profile.UserProfileResponse;
import com.aijobapplyassistant.service.ResumeStorageService;
import com.aijobapplyassistant.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserProfileService userProfileService;
    private final ResumeStorageService resumeStorageService;

    public ProfileController(UserProfileService userProfileService, ResumeStorageService resumeStorageService) {
        this.userProfileService = userProfileService;
        this.resumeStorageService = resumeStorageService;
    }

    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile(@AuthenticationPrincipal String email) {
        return ApiResponse.ok(userProfileService.getProfile(email), "Profile loaded");
    }

    @PutMapping
    public ApiResponse<UserProfileResponse> updateProfile(@AuthenticationPrincipal String email, @Valid @RequestBody UserProfileRequest request) {
        return ApiResponse.ok(userProfileService.updateProfile(email, request), "Profile updated");
    }

    @PostMapping("/resume")
    public ApiResponse<ResumeUploadResponse> uploadResume(@AuthenticationPrincipal String email, @RequestParam("file") MultipartFile file) {
        ResumeUploadResponse response = resumeStorageService.store(file);
        userProfileService.updateResumeMetadata(email, response.fileName(), response.storedPath());
        return ApiResponse.ok(response, "Resume uploaded");
    }
}
