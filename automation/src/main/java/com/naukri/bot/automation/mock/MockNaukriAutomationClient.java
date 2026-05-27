package com.naukri.bot.automation.mock;

import com.naukri.bot.automation.AutomationControl;
import com.naukri.bot.automation.AutomationActivityListener;
import com.naukri.bot.automation.NaukriAutomationClient;
import com.naukri.bot.automation.QuestionAnswerProvider;
import com.naukri.bot.automation.model.ApplyStatus;
import com.naukri.bot.automation.model.AutomationRunRequest;
import com.naukri.bot.automation.model.AutomationRunResult;
import com.naukri.bot.automation.model.ExternalRedirectResult;
import com.naukri.bot.automation.model.JobApplicationResult;
import com.naukri.bot.automation.model.LoginTestResult;

import java.time.Instant;

public class MockNaukriAutomationClient implements NaukriAutomationClient {
    @Override
    public AutomationRunResult run(AutomationRunRequest request,
                                   QuestionAnswerProvider questionAnswerProvider,
                                   AutomationControl control,
                                   AutomationActivityListener activityListener) {
        AutomationRunResult result = new AutomationRunResult();
        activityListener.onActivity("Mock: opening Naukri login page");
        result.getMessages().add("Mock automation login completed.");
        activityListener.onActivity("Mock: searching jobs for " + request.filter().keywords());
        result.getMessages().add("Mock search completed for keywords: " + request.filter().keywords());
        activityListener.onActivity("Mock: checking saved answers for application questions");
        questionAnswerProvider.answerFor("What is your current CTC?")
                .ifPresentOrElse(
                        answer -> result.getMessages().add("Mock question answered with saved answer."),
                        () -> result.getMessages().add("Mock question had no saved answer; dry-run continued."));
        activityListener.onActivity("Mock: recording dry-run application for Example Tech");
        result.getJobResults().add(new JobApplicationResult(
                "Example Tech",
                "Senior Java Spring Boot Engineer",
                Instant.now(),
                request.dryRun() ? ApplyStatus.DRY_RUN : ApplyStatus.SUCCESS,
                "https://www.naukri.com/job-listings-example",
                request.filter().experience(),
                request.filter().salary(),
                request.filter().location(),
                false,
                request.dryRun() ? "Dry run enabled" : null,
                null,
                92.0,
                1));
        activityListener.onActivity("Mock: recording external redirect for Example Global");
        result.getExternalRedirects().add(new ExternalRedirectResult(
                "Example Global",
                "https://careers.example.com/jobs/123",
                Instant.now(),
                ApplyStatus.EXTERNAL_REDIRECT));
        result.finish();
        return result;
    }

    @Override
    public LoginTestResult testLogin(AutomationRunRequest request, AutomationActivityListener activityListener) {
        activityListener.onActivity("Mock: testing Naukri login");
        boolean success = request.naukriEmail() != null && !request.naukriEmail().isBlank()
                && request.naukriPassword() != null && !request.naukriPassword().isBlank();
        return success
                ? LoginTestResult.success("mock://naukri-login")
                : new LoginTestResult(false, com.naukri.bot.automation.model.LoginStatus.INVALID_CREDENTIALS,
                "Mock login requires non-empty Naukri credentials.", "mock://naukri-login", null);
    }
}
