package com.aijobapplyassistant.service;

import com.aijobapplyassistant.dto.automation.AutomationStatusResponse;
import com.aijobapplyassistant.dto.automation.DashboardSummaryResponse;
import com.aijobapplyassistant.dto.report.ReportRowResponse;
import com.aijobapplyassistant.entity.AutomationRun;
import com.aijobapplyassistant.entity.JobApplication;
import com.aijobapplyassistant.entity.JobPreference;
import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.entity.enums.ApplicationStatus;
import com.aijobapplyassistant.entity.enums.AutomationRunStatus;
import com.aijobapplyassistant.exception.ApiException;
import com.aijobapplyassistant.repository.AutomationRunRepository;
import com.aijobapplyassistant.repository.JobApplicationRepository;
import com.aijobapplyassistant.repository.UserRepository;
import com.aijobapplyassistant.service.bot.BotRunResult;
import com.aijobapplyassistant.service.bot.JobProcessingResult;
import com.aijobapplyassistant.service.bot.PlaywrightJobBotEngine;
import com.aijobapplyassistant.service.report.ReportService;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class JobAutomationService {

    private final UserProfileService userProfileService;
    private final UserRepository userRepository;
    private final AutomationRunRepository automationRunRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final PlaywrightJobBotEngine playwrightJobBotEngine;
    private final SystemLogService systemLogService;
    private final QuestionQueueService questionQueueService;
    private final ReportService reportService;
    private final NotificationService notificationService;
    private final com.aijobapplyassistant.service.ai.ResumeParsingService resumeParsingService;
    private final com.aijobapplyassistant.service.security.CredentialCipherService credentialCipherService;
    private final Executor automationExecutor;
    private final Map<Long, AtomicBoolean> stopSignals = new ConcurrentHashMap<>();

    public JobAutomationService(UserProfileService userProfileService, UserRepository userRepository,
            AutomationRunRepository automationRunRepository, JobApplicationRepository jobApplicationRepository,
            PlaywrightJobBotEngine playwrightJobBotEngine, SystemLogService systemLogService,
            QuestionQueueService questionQueueService, ReportService reportService,
            NotificationService notificationService,
            com.aijobapplyassistant.service.ai.ResumeParsingService resumeParsingService,
            com.aijobapplyassistant.service.security.CredentialCipherService credentialCipherService,
            @Qualifier("automationExecutor") Executor automationExecutor) {
        this.userProfileService = userProfileService;
        this.userRepository = userRepository;
        this.automationRunRepository = automationRunRepository;
        this.jobApplicationRepository = jobApplicationRepository;
        this.playwrightJobBotEngine = playwrightJobBotEngine;
        this.systemLogService = systemLogService;
        this.questionQueueService = questionQueueService;
        this.reportService = reportService;
        this.notificationService = notificationService;
        this.resumeParsingService = resumeParsingService;
        this.credentialCipherService = credentialCipherService;
        this.automationExecutor = automationExecutor;
    }

    public AutomationStatusResponse startManualRun(String email) {
        User user = userProfileService.findByEmail(email);
        validateUserSetup(user);
        AutomationRun latest = automationRunRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);
        if (latest != null && latest.getStatus() == AutomationRunStatus.RUNNING) {
            return toStatus(latest);
        }

        AutomationRun run = new AutomationRun();
        run.setUser(user);
        run.setStatus(AutomationRunStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run.setSummary("Automation started");
        automationRunRepository.save(run);

        stopSignals.put(user.getId(), new AtomicBoolean(false));
        automationExecutor.execute(() -> executeRun(user.getId(), run.getId()));
        return toStatus(run);
    }

    public AutomationStatusResponse stopManualRun(String email) {
        User user = userProfileService.findByEmail(email);
        AtomicBoolean stopSignal = stopSignals.computeIfAbsent(user.getId(), key -> new AtomicBoolean(false));
        stopSignal.set(true);
        AutomationRun latest = automationRunRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No automation run found"));
        latest.setSummary("Stop requested");
        automationRunRepository.save(latest);
        return toStatus(latest);
    }

    public AutomationStatusResponse getStatus(String email) {
        User user = userProfileService.findByEmail(email);
        AutomationRun latest = automationRunRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .orElseGet(() -> {
                    AutomationRun run = new AutomationRun();
                    run.setUser(user);
                    run.setStatus(AutomationRunStatus.IDLE);
                    run.setSummary("No run started yet");
                    return run;
                });
        return toStatus(latest);
    }

    public DashboardSummaryResponse getDashboard(String email) {
        User user = userProfileService.findByEmail(email);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);
        List<ReportRowResponse> recentApplications = jobApplicationRepository.findTop20ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(item -> new ReportRowResponse(item.getAppliedDate(), item.getCompany(), item.getTitle(), item.getPortal(),
                        item.getStatus(), item.getRemarks(), item.getRelevanceScore()))
                .toList();

        return new DashboardSummaryResponse(
                getStatus(email),
                jobApplicationRepository.countByUserIdAndStatusAndAppliedDateBetween(user.getId(), ApplicationStatus.APPLIED, start, end),
                jobApplicationRepository.countByUserIdAndStatusAndAppliedDateBetween(user.getId(), ApplicationStatus.FAILED, start, end),
                jobApplicationRepository.countByUserIdAndStatusAndAppliedDateBetween(user.getId(), ApplicationStatus.SKIPPED, start, end),
                questionQueueService.countOpenQuestions(user.getId()),
                recentApplications
        );
    }

    public void startScheduledRuns() {
        for (User user : userRepository.findByActiveTrue()) {
            if (user.getResumeFilePath() != null && !user.getJobPreferences().isEmpty()) {
                startManualRun(user.getEmail());
            }
        }
    }

    private void executeRun(Long userId, Long runId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        AutomationRun run = automationRunRepository.findById(runId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Run not found"));
        AtomicBoolean stopSignal = stopSignals.computeIfAbsent(userId, key -> new AtomicBoolean(false));

        try {
            String resumeText = resumeParsingService.extractText(Path.of(user.getResumeFilePath()));
            String naukriPassword = credentialCipherService.decrypt(user.getEncryptedNaukriPassword());
            BotRunResult botRunResult = playwrightJobBotEngine.execute(user, activePreferences(user.getJobPreferences()), resumeText, user.getResumeFilePath(), stopSignal, naukriPassword);

            persistResults(user, botRunResult.results());
            run.setJobsSearched(botRunResult.searched());
            run.setJobsApplied(botRunResult.applied());
            run.setJobsSkipped(botRunResult.skipped());
            run.setJobsFailed(botRunResult.failed());
            run.setCredentialsCreated(botRunResult.credentialsCreated());
            run.setEndedAt(LocalDateTime.now());
            run.setStatus(stopSignal.get() ? AutomationRunStatus.STOPPED :
                    questionQueueService.countOpenQuestions(user.getId()) > 0 ? AutomationRunStatus.WAITING_FOR_USER : AutomationRunStatus.COMPLETED);
            run.setSummary(botRunResult.summary());
            automationRunRepository.save(run);

            var report = reportService.generateDailyReport(user, LocalDate.now());
            notificationService.notifyReport(user, report);
            systemLogService.info(user, "JobAutomationService", "Automation run finished with status " + run.getStatus());
        } catch (Exception exception) {
            run.setStatus(AutomationRunStatus.FAILED);
            run.setEndedAt(LocalDateTime.now());
            run.setSummary(exception.getMessage());
            automationRunRepository.save(run);
            systemLogService.error(user, "JobAutomationService", "Automation run failed", exception.getMessage());
        } finally {
            stopSignals.remove(userId);
        }
    }

    private void persistResults(User user, List<JobProcessingResult> results) {
        for (JobProcessingResult result : results) {
            if (result.listing().jobUrl() != null && jobApplicationRepository.findByUserIdAndJobUrl(user.getId(), result.listing().jobUrl()).isPresent()) {
                continue;
            }

            JobApplication application = new JobApplication();
            application.setUser(user);
            application.setCompany(result.listing().company());
            application.setTitle(result.listing().title());
            application.setPortal(result.listing().portal());
            application.setPortalUrl(result.listing().externalUrl());
            application.setJobUrl(result.listing().jobUrl());
            application.setRelevanceScore(result.relevanceScore());
            application.setStatus(result.status());
            application.setRemarks(result.remarks());
            application.setAppliedDate(LocalDateTime.now());
            jobApplicationRepository.save(application);
        }
    }

    private List<JobPreference> activePreferences(List<JobPreference> preferences) {
        return preferences.stream().filter(JobPreference::isActive).toList();
    }

    private void validateUserSetup(User user) {
        if (user.getResumeFilePath() == null || user.getResumeFilePath().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Resume upload is required before applying");
        }
        if (user.getNaukriUsername() == null || user.getEncryptedNaukriPassword() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Naukri credentials are required before applying");
        }
        if (activePreferences(user.getJobPreferences()).isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At least one active job preference is required");
        }
    }

    private AutomationStatusResponse toStatus(AutomationRun run) {
        return new AutomationStatusResponse(
                run.getId(),
                run.getStatus(),
                run.getStartedAt(),
                run.getEndedAt(),
                run.getJobsSearched(),
                run.getJobsApplied(),
                run.getJobsSkipped(),
                run.getJobsFailed(),
                run.getCredentialsCreated(),
                run.getSummary()
        );
    }
}
