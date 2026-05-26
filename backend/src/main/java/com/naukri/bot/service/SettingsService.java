package com.naukri.bot.service;

import com.naukri.bot.domain.JobFilter;
import com.naukri.bot.domain.NaukriCredentials;
import com.naukri.bot.domain.ResumeProfile;
import com.naukri.bot.domain.SchedulerConfig;
import com.naukri.bot.domain.User;
import com.naukri.bot.dto.SettingsDtos.JobFilterRequest;
import com.naukri.bot.dto.SettingsDtos.JobFilterResponse;
import com.naukri.bot.dto.SettingsDtos.NaukriCredentialsRequest;
import com.naukri.bot.dto.SettingsDtos.NaukriCredentialsResponse;
import com.naukri.bot.dto.SettingsDtos.ResumeProfileRequest;
import com.naukri.bot.dto.SettingsDtos.ResumeProfileResponse;
import com.naukri.bot.dto.SettingsDtos.SchedulerConfigRequest;
import com.naukri.bot.dto.SettingsDtos.SchedulerConfigResponse;
import com.naukri.bot.repository.JobFilterRepository;
import com.naukri.bot.repository.NaukriCredentialsRepository;
import com.naukri.bot.repository.ResumeProfileRepository;
import com.naukri.bot.repository.SchedulerConfigRepository;
import com.naukri.bot.util.ListTextMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingsService {
    private final NaukriCredentialsRepository credentialsRepository;
    private final JobFilterRepository jobFilterRepository;
    private final ResumeProfileRepository resumeProfileRepository;
    private final SchedulerConfigRepository schedulerConfigRepository;
    private final EncryptionService encryptionService;

    @Transactional
    public NaukriCredentialsResponse saveCredentials(User user, NaukriCredentialsRequest request) {
        NaukriCredentials credentials = credentialsRepository.findByUser(user).orElseGet(NaukriCredentials::new);
        credentials.setUser(user);
        credentials.setEmail(request.email());
        credentials.setEncryptedPassword(encryptionService.encrypt(request.password()));
        credentials.setResumePath(request.resumePath());
        credentials.setUpdatedAt(Instant.now());
        credentialsRepository.save(credentials);
        return new NaukriCredentialsResponse(credentials.getEmail(), credentials.getResumePath(), credentials.getUpdatedAt());
    }

    public NaukriCredentialsResponse getCredentials(User user) {
        return credentialsRepository.findByUser(user)
                .map(credentials -> new NaukriCredentialsResponse(credentials.getEmail(), credentials.getResumePath(), credentials.getUpdatedAt()))
                .orElse(new NaukriCredentialsResponse("", "", null));
    }

    @Transactional
    public JobFilterResponse saveFilter(User user, JobFilterRequest request) {
        JobFilter filter = jobFilterRepository.findByUser(user).orElseGet(JobFilter::new);
        filter.setUser(user);
        filter.setKeywords(request.keywords());
        filter.setExperience(request.experience());
        filter.setLocation(request.location());
        filter.setSalary(request.salary());
        filter.setWorkMode(request.workMode());
        filter.setFreshness(request.freshness());
        filter.setPreferredCompanies(ListTextMapper.join(request.preferredCompanies()));
        filter.setBlacklistedCompanies(ListTextMapper.join(request.blacklistedCompanies()));
        filter.setAutoApply(request.autoApply());
        filter.setExternalCareerApply(request.externalCareerApply());
        filter.setDailyApplyLimit(request.dailyApplyLimit());
        filter.setEasyApplyOnly(request.easyApplyOnly());
        filter.setDuplicatePreventionDays(request.duplicatePreventionDays());
        filter.setUpdatedAt(Instant.now());
        jobFilterRepository.save(filter);
        return toFilterResponse(filter);
    }

    public JobFilterResponse getFilter(User user) {
        return jobFilterRepository.findByUser(user).map(this::toFilterResponse)
                .orElse(new JobFilterResponse("", "", "", "", "", "", List.of(), List.of(), true,
                        false, 20, true, 30, null));
    }

    @Transactional
    public ResumeProfileResponse saveResume(User user, ResumeProfileRequest request) {
        ResumeProfile profile = new ResumeProfile();
        profile.setUser(user);
        profile.setName(request.name());
        profile.setFilePath(request.filePath());
        profile.setKeywords(ListTextMapper.join(request.keywords()));
        profile.setActive(request.active());
        resumeProfileRepository.save(profile);
        return toResumeResponse(profile);
    }

    public List<ResumeProfileResponse> listResumes(User user) {
        return resumeProfileRepository.findByUserAndActiveTrue(user).stream().map(this::toResumeResponse).toList();
    }

    @Transactional
    public SchedulerConfigResponse saveScheduler(User user, SchedulerConfigRequest request) {
        SchedulerConfig config = schedulerConfigRepository.findByUser(user).orElseGet(SchedulerConfig::new);
        config.setUser(user);
        config.setEnabled(request.enabled());
        config.setIntervalMinutes(request.intervalMinutes());
        config.setUpdatedAt(Instant.now());
        schedulerConfigRepository.save(config);
        return toSchedulerResponse(config);
    }

    public SchedulerConfigResponse getScheduler(User user) {
        return schedulerConfigRepository.findByUser(user).map(this::toSchedulerResponse)
                .orElse(new SchedulerConfigResponse(false, 60, null));
    }

    private JobFilterResponse toFilterResponse(JobFilter filter) {
        return new JobFilterResponse(
                filter.getKeywords(),
                filter.getExperience(),
                filter.getLocation(),
                filter.getSalary(),
                filter.getWorkMode(),
                filter.getFreshness(),
                ListTextMapper.split(filter.getPreferredCompanies()),
                ListTextMapper.split(filter.getBlacklistedCompanies()),
                filter.isAutoApply(),
                filter.isExternalCareerApply(),
                filter.getDailyApplyLimit(),
                filter.isEasyApplyOnly(),
                filter.getDuplicatePreventionDays(),
                filter.getUpdatedAt());
    }

    private ResumeProfileResponse toResumeResponse(ResumeProfile profile) {
        return new ResumeProfileResponse(profile.getId(), profile.getName(), profile.getFilePath(),
                ListTextMapper.split(profile.getKeywords()), profile.isActive());
    }

    private SchedulerConfigResponse toSchedulerResponse(SchedulerConfig config) {
        return new SchedulerConfigResponse(config.isEnabled(), config.getIntervalMinutes(), config.getLastTriggeredAt());
    }
}
