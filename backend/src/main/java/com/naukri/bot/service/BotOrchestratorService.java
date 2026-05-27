package com.naukri.bot.service;

import com.naukri.bot.automation.AutomationControl;
import com.naukri.bot.automation.AutomationActivityListener;
import com.naukri.bot.automation.NaukriAutomationClient;
import com.naukri.bot.automation.model.AutomationJobFilter;
import com.naukri.bot.automation.model.AutomationRunRequest;
import com.naukri.bot.automation.model.AutomationRunResult;
import com.naukri.bot.automation.model.JobApplicationResult;
import com.naukri.bot.automation.model.LoginStatus;
import com.naukri.bot.automation.model.LoginTestResult;
import com.naukri.bot.automation.model.ProxySettings;
import com.naukri.bot.config.AppProperties;
import com.naukri.bot.domain.AppliedJob;
import com.naukri.bot.domain.ApplyStatus;
import com.naukri.bot.domain.BotRunStatus;
import com.naukri.bot.domain.ExternalRedirectJob;
import com.naukri.bot.domain.JobFilter;
import com.naukri.bot.domain.NaukriCredentials;
import com.naukri.bot.domain.User;
import com.naukri.bot.dto.BotDtos.BotCommandResponse;
import com.naukri.bot.exception.AppException;
import com.naukri.bot.repository.AppliedJobRepository;
import com.naukri.bot.repository.ExternalRedirectJobRepository;
import com.naukri.bot.repository.JobFilterRepository;
import com.naukri.bot.repository.NaukriCredentialsRepository;
import com.naukri.bot.repository.UserRepository;
import com.naukri.bot.util.ListTextMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BotOrchestratorService {
    private static final Logger log = LoggerFactory.getLogger(BotOrchestratorService.class);

    private final NaukriAutomationClient automationClient;
    private final TaskExecutor botTaskExecutor;
    private final AppProperties properties;
    private final NaukriCredentialsRepository credentialsRepository;
    private final UserRepository userRepository;
    private final JobFilterRepository jobFilterRepository;
    private final AppliedJobRepository appliedJobRepository;
    private final ExternalRedirectJobRepository externalRedirectJobRepository;
    private final EncryptionService encryptionService;
    private final AIAnswerService aiAnswerService;
    private final CsvService csvService;
    private final SmartMatchService smartMatchService;
    private final ResumeSelectionService resumeSelectionService;
    private final BotStatusService botStatusService;
    private final BotLogService botLogService;
    private final BotRuntimeState runtimeState;
    private final NotificationService notificationService;
    private final PendingQuestionService pendingQuestionService;

    public BotCommandResponse start(User user) {
        botLogService.clear(user);
        String validationFailure = validateStartRequest(user);
        if (validationFailure != null) {
            botStatusService.set(user, BotRunStatus.FAILED, false, false, false, validationFailure);
            botLogService.error(user, validationFailure);
            return new BotCommandResponse(validationFailure, BotRunStatus.FAILED);
        }
        botStatusService.set(user, BotRunStatus.RUNNING, true, false, false, "Automation started");
        botLogService.info(user, "Automation queued");
        runtimeState.start(user.getId());
        try {
            botTaskExecutor.execute(() -> runAutomation(user.getId()));
        } catch (Exception exception) {
            String message = "Unable to queue automation: " + friendlyMessage(exception);
            botStatusService.set(user, BotRunStatus.FAILED, false, false, false, message);
            botLogService.error(user, message);
            return new BotCommandResponse(message, BotRunStatus.FAILED);
        }
        return new BotCommandResponse("Bot started", BotRunStatus.RUNNING);
    }

    public BotCommandResponse manualLoginAndContinue(User user) {
        String validationFailure = validateStartRequest(user);
        if (validationFailure != null) {
            botStatusService.set(user, BotRunStatus.FAILED, false, false, false, validationFailure);
            botLogService.error(user, validationFailure);
            return new BotCommandResponse(validationFailure, BotRunStatus.FAILED);
        }
        String message = "Opening visible Naukri browser for manual login. Complete captcha/login there; automation will continue automatically.";
        botStatusService.set(user, BotRunStatus.RUNNING, true, false, false, message);
        botLogService.info(user, message);
        runtimeState.start(user.getId());
        try {
            botTaskExecutor.execute(() -> runAutomation(user.getId(), true));
        } catch (Exception exception) {
            String failure = "Unable to queue manual login recovery: " + friendlyMessage(exception);
            botStatusService.set(user, BotRunStatus.FAILED, false, false, false, failure);
            botLogService.error(user, failure);
            return new BotCommandResponse(failure, BotRunStatus.FAILED);
        }
        return new BotCommandResponse(message, BotRunStatus.RUNNING);
    }

    public BotCommandResponse stop(User user) {
        runtimeState.stop(user.getId());
        pendingQuestionService.cancel(user);
        botStatusService.set(user, BotRunStatus.STOPPED, false, false, false, "Stop requested");
        botLogService.warn(user, "Stop requested");
        return new BotCommandResponse("Stop requested", BotRunStatus.STOPPED);
    }

    public BotCommandResponse pause(User user) {
        runtimeState.pause(user.getId());
        botStatusService.set(user, BotRunStatus.PAUSED, true, true, false, "Paused");
        botLogService.warn(user, "Automation paused");
        return new BotCommandResponse("Bot paused", BotRunStatus.PAUSED);
    }

    public BotCommandResponse resume(User user) {
        runtimeState.resume(user.getId());
        botStatusService.set(user, BotRunStatus.RUNNING, true, false, false, "Resumed");
        botLogService.info(user, "Automation resumed");
        return new BotCommandResponse("Bot resumed", BotRunStatus.RUNNING);
    }

    public BotCommandResponse testLogin(User user) {
        LoginTestResult result = automationClient.testLogin(request(user, true), activityListener(user));
        if (result.success()) {
            botStatusService.set(user, BotRunStatus.IDLE, false, false, false, result.message());
            botLogService.info(user, result.message());
            return new BotCommandResponse(result.message(), BotRunStatus.IDLE);
        }
        boolean captcha = result.status() == LoginStatus.CAPTCHA_DETECTED;
        BotRunStatus status = captcha ? BotRunStatus.CAPTCHA_REQUIRED : BotRunStatus.FAILED;
        botStatusService.set(user, status, false, captcha, captcha, result.message());
        botLogService.error(user, result.message());
        return new BotCommandResponse(result.message(), status);
    }

    public BotCommandResponse testApply(User user) {
        String message = "Test apply running in dry-run mode. Complete manual Naukri verification if a browser opens.";
        botStatusService.set(user, BotRunStatus.RUNNING, true, false, false, message);
        botLogService.info(user, message);
        AutomationRunRequest request = request(user, true, true);
        AutomationRunResult result = automationClient.run(request, question -> answerForQuestion(user, question),
                new Control(user.getId()), activityListener(user));
        persistResult(user, jobFilterRepository.findByUser(user).orElseThrow(), result);
        if (result.isCaptchaDetected()) {
            String failure = "Test apply stopped because Naukri requested captcha or account verification. No live applications were submitted.";
            botStatusService.set(user, BotRunStatus.FAILED, false, false, false, failure);
            return new BotCommandResponse(failure, BotRunStatus.FAILED);
        }
        if (result.isLoginFailed()) {
            botStatusService.set(user, BotRunStatus.FAILED, false, false, false, result.getFailureReason());
            return new BotCommandResponse(result.getFailureReason(), BotRunStatus.FAILED);
        }
        botStatusService.set(user, BotRunStatus.IDLE, false, false, false, "Test apply completed in dry-run mode");
        return new BotCommandResponse("Test apply completed in dry-run mode", BotRunStatus.IDLE);
    }

    public void runAutomation(Long userId) {
        runAutomation(userId, false);
    }

    public void runAutomation(Long userId, boolean manualLoginOnCaptcha) {
        User user = null;
        try {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "User not found"));
            User automationUser = user;
            JobFilter filter = jobFilterRepository.findByUser(user)
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Job filters are not configured"));
            AutomationRunResult result = automationClient.run(request(user, properties.bot().dryRun(), manualLoginOnCaptcha),
                    question -> answerForQuestion(automationUser, question),
                    new Control(user.getId()),
                    activityListener(automationUser));
            persistResult(user, filter, result);
            if (result.isCaptchaDetected()) {
                botStatusService.set(user, BotRunStatus.CAPTCHA_REQUIRED, false, true, true, "Captcha detected. Manual login required.");
                botLogService.warn(user, "Captcha detected. Automation paused.");
                return;
            }
            if (result.isLoginFailed()) {
                String message = failureMessage(result.getFailureReason());
                botStatusService.set(user, BotRunStatus.FAILED, false, false, false, message);
                botLogService.error(user, message);
                return;
            }
            botStatusService.set(user, BotRunStatus.STOPPED, false, false, false, "Automation completed");
            notificationService.dailySummary("Naukri bot run completed", "Processed " + result.getJobResults().size() + " jobs.");
        } catch (Exception exception) {
            String message = "Automation failed: " + friendlyMessage(exception);
            if (user == null) {
                log.error(message, exception);
                return;
            }
            botStatusService.set(user, BotRunStatus.FAILED, false, false, false, message);
            botLogService.error(user, message);
        }
    }

    private String validateStartRequest(User user) {
        try {
            request(user, properties.bot().dryRun());
            return null;
        } catch (Exception exception) {
            return "Cannot start bot: " + friendlyMessage(exception);
        }
    }

    private AutomationRunRequest request(User user, boolean dryRun) {
        return request(user, dryRun, false);
    }

    private AutomationRunRequest request(User user, boolean dryRun, boolean manualLoginOnCaptcha) {
        NaukriCredentials credentials = credentialsRepository.findByUser(user)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Naukri credentials are not configured"));
        JobFilter filter = jobFilterRepository.findByUser(user)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Job filters are not configured"));
        List<String> resumes = resumeSelectionService.selectResumePaths(user, filter.getKeywords());
        if (resumes.isEmpty() && credentials.getResumePath() != null && !credentials.getResumePath().isBlank()) {
            resumes = List.of(credentials.getResumePath());
        }
        return new AutomationRunRequest(
                credentials.getEmail(),
                encryptionService.decrypt(credentials.getEncryptedPassword()),
                new AutomationJobFilter(filter.getKeywords(), filter.getExperience(), filter.getLocation(), filter.getSalary(),
                        filter.getWorkMode(), filter.getFreshness(), ListTextMapper.split(filter.getPreferredCompanies()),
                        ListTextMapper.split(filter.getBlacklistedCompanies()), filter.isAutoApply(), filter.isExternalCareerApply(),
                        filter.getDailyApplyLimit(), filter.isEasyApplyOnly(), filter.getDuplicatePreventionDays()),
                resumes,
                Path.of(properties.bot().storageDir()),
                manualLoginOnCaptcha ? false : properties.bot().headless(),
                dryRun,
                manualLoginOnCaptcha,
                properties.bot().manualLoginTimeoutSeconds(),
                new ProxySettings(properties.bot().proxyHost(), properties.bot().proxyPort(), properties.bot().proxyUsername(), properties.bot().proxyPassword()),
                properties.bot().maxRetries());
    }

    private Optional<String> answerForQuestion(User user, String question) {
        Optional<String> savedAnswer = aiAnswerService.answerFor(user, question);
        if (savedAnswer.isPresent()) {
            botLogService.info(user, "Auto-answered application question: " + question);
            return savedAnswer;
        }
        String message = "Waiting for your answer to application question: " + question;
        botStatusService.activity(user, message);
        botLogService.warn(user, message);
        int timeoutSeconds = Math.max(30, properties.bot().questionAnswerTimeoutSeconds());
        Optional<String> answer = pendingQuestionService.waitForAnswer(user, question, Duration.ofSeconds(timeoutSeconds));
        if (answer.isEmpty()) {
            botLogService.warn(user, "No answer received before timeout for question: " + question);
        }
        return answer;
    }

    private void persistResult(User user, JobFilter filter, AutomationRunResult result) {
        for (String message : result.getMessages()) {
            botLogService.info(user, message);
        }
        for (JobApplicationResult jobResult : result.getJobResults()) {
            if (isDuplicate(user, filter, jobResult)) {
                botLogService.warn(user, "Skipped duplicate: " + jobResult.companyName() + " - " + jobResult.jobTitle());
                continue;
            }
            AppliedJob job = new AppliedJob();
            job.setUser(user);
            job.setCompanyName(defaultText(jobResult.companyName(), "Unknown Company"));
            job.setJobTitle(defaultText(jobResult.jobTitle(), "Unknown Job"));
            job.setApplyDateTime(jobResult.applyDateTime());
            job.setStatus(ApplyStatus.valueOf(jobResult.status().name()));
            job.setJobUrl(jobResult.jobUrl());
            job.setExperience(jobResult.experience());
            job.setSalary(jobResult.salary());
            job.setLocation(jobResult.location());
            job.setRedirectedExternalSite(jobResult.redirectedExternalSite());
            job.setFailureReason(jobResult.failureReason());
            job.setScreenshotPath(jobResult.screenshotPath());
            job.setAttempts(jobResult.attempts());
            job.setMatchScore(smartMatchService.score(filter, job.getJobTitle(), job.getCompanyName(), ""));
            job.setCsvFileName(csvService.writeAppliedJob(job));
            appliedJobRepository.save(job);
            botLogService.info(user, "Recorded job: " + job.getCompanyName() + " - " + job.getJobTitle() + " [" + job.getStatus() + "]");
        }
        result.getExternalRedirects().forEach(redirect -> {
            ExternalRedirectJob external = new ExternalRedirectJob();
            external.setUser(user);
            external.setCompanyName(defaultText(redirect.companyName(), "Unknown Company"));
            external.setRedirectUrl(redirect.redirectUrl());
            external.setRedirectDateTime(redirect.redirectDateTime());
            external.setStatus(ApplyStatus.valueOf(redirect.status().name()));
            external.setCsvFileName(csvService.writeExternalRedirect(external));
            externalRedirectJobRepository.save(external);
        });
    }

    private boolean isDuplicate(User user, JobFilter filter, JobApplicationResult jobResult) {
        Instant after = Instant.now().minus(filter.getDuplicatePreventionDays(), ChronoUnit.DAYS);
        return appliedJobRepository.existsByUserAndCompanyNameIgnoreCaseAndJobTitleIgnoreCaseAndStatusInAndApplyDateTimeAfter(
                user,
                defaultText(jobResult.companyName(), "Unknown Company"),
                defaultText(jobResult.jobTitle(), "Unknown Job"),
                List.of(ApplyStatus.SUCCESS, ApplyStatus.ALREADY_APPLIED),
                after);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String failureMessage(String message) {
        return message == null || message.isBlank() ? "Automation failed. Check logs for details." : message;
    }

    private AutomationActivityListener activityListener(User user) {
        return message -> {
            if (message == null || message.isBlank()) {
                return;
            }
            try {
                botStatusService.activity(user, message);
                botLogService.info(user, "Activity: " + message);
            } catch (Exception exception) {
                log.warn("Unable to save automation activity", exception);
            }
        };
    }

    private String friendlyMessage(Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = exception.getMessage();
        String rootMessage = root.getMessage();
        if ((message != null && message.contains("Unable to decrypt value"))
                || (rootMessage != null && rootMessage.contains("Tag mismatch"))) {
            return "Unable to decrypt saved Naukri password. Re-save Naukri credentials using the current encryption key.";
        }
        if (message == null || message.isBlank()) {
            message = rootMessage;
        }
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private class Control implements AutomationControl {
        private final Long userId;

        private Control(Long userId) {
            this.userId = userId;
        }

        @Override
        public boolean shouldStop() {
            return runtimeState.shouldStop(userId);
        }

        @Override
        public boolean isPaused() {
            return runtimeState.isPaused(userId);
        }
    }
}
