package com.naukri.bot.automation.playwright.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.naukri.bot.automation.AutomationActivityListener;
import com.naukri.bot.automation.QuestionAnswerProvider;
import com.naukri.bot.automation.model.ApplyStatus;
import com.naukri.bot.automation.model.AutomationRunRequest;
import com.naukri.bot.automation.model.DiscoveredJob;
import com.naukri.bot.automation.model.JobApplicationResult;
import com.naukri.bot.automation.playwright.HumanBehavior;

import java.time.Instant;
import java.util.Locale;

public class NaukriJobPage {
    private static final int MAX_APPLY_STEPS = 6;

    private final Page page;
    private final AutomationActivityListener activityListener;
    private final HumanBehavior human = new HumanBehavior();

    public NaukriJobPage(Page page) {
        this(page, AutomationActivityListener.NOOP);
    }

    public NaukriJobPage(Page page, AutomationActivityListener activityListener) {
        this.page = page;
        this.activityListener = activityListener;
    }

    public JobApplicationResult apply(DiscoveredJob job,
                                      AutomationRunRequest request,
                                      QuestionAnswerProvider answerProvider,
                                      int attempt) {
        activity("Opening job: " + safe(job.jobTitle()) + " at " + safe(job.companyName()) + " (attempt " + attempt + ")");
        page.navigate(job.jobUrl(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        activity("Waiting for job detail page to load");
        page.waitForLoadState();
        human.pause();

        if (captchaDetected()) {
            activity("Captcha detected on job page");
            return result(job, ApplyStatus.CAPTCHA_DETECTED, "Captcha detected", false, attempt);
        }
        activity("Checking application state for: " + safe(job.jobTitle()));
        String body = page.locator("body").innerText();
        if (contains(body, "already applied", "application sent")) {
            activity("Already applied: " + safe(job.jobTitle()) + " at " + safe(job.companyName()));
            return result(job, ApplyStatus.ALREADY_APPLIED, null, false, attempt);
        }
        if (request.dryRun()) {
            activity("Dry run enabled. Skipping actual apply for: " + safe(job.jobTitle()));
            return result(job, ApplyStatus.DRY_RUN, "Dry run enabled", false, attempt);
        }

        activity("Looking for apply button");
        Locator applyButton = page.locator("button:has-text('Apply'), a:has-text('Apply'), button:has-text('I am interested')").first();
        if (applyButton.count() == 0) {
            activity("Apply button not found for: " + safe(job.jobTitle()));
            return result(job, ApplyStatus.FAILED, "Apply button not found", false, attempt);
        }

        String beforeUrl = page.url();
        activity("Clicking apply for: " + safe(job.jobTitle()));
        applyButton.click();
        page.waitForLoadState();
        human.pause();
        if (externalRedirect(beforeUrl, page.url())) {
            activity("External career site redirect detected for: " + safe(job.companyName()));
            return result(job, ApplyStatus.EXTERNAL_REDIRECT, null, true, attempt);
        }

        for (int step = 1; step <= MAX_APPLY_STEPS; step++) {
            activity("Checking application step " + step + " for: " + safe(job.jobTitle()));
            AnswerOutcome answerOutcome = answerQuestions(answerProvider);
            if (answerOutcome == AnswerOutcome.NEEDS_USER_INPUT) {
                activity("Application question still needs an answer for: " + safe(job.jobTitle()));
                return result(job, ApplyStatus.QUESTION_NEEDS_ANSWER, "A question needs a saved answer", false, attempt);
            }
            if (applicationComplete()) {
                activity("Application success detected for: " + safe(job.jobTitle()));
                return result(job, ApplyStatus.SUCCESS, null, false, attempt);
            }
            Locator submitButton = firstVisible("button:has-text('Submit'), button:has-text('Send'), button:has-text('Apply'), "
                    + "button:has-text('Continue'), button:has-text('Next')");
            if (submitButton == null) {
                break;
            }
            String beforeStepUrl = page.url();
            activity("Submitting application step " + step + " for: " + safe(job.jobTitle()));
            submitButton.click();
            page.waitForLoadState();
            human.pause();
            if (externalRedirect(beforeStepUrl, page.url())) {
                activity("External career site redirect detected for: " + safe(job.companyName()));
                return result(job, ApplyStatus.EXTERNAL_REDIRECT, null, true, attempt);
            }
        }
        activity("Application completed for: " + safe(job.jobTitle()) + " at " + safe(job.companyName()));
        return result(job, ApplyStatus.SUCCESS, null, false, attempt);
    }

    private AnswerOutcome answerQuestions(QuestionAnswerProvider answerProvider) {
        Locator fields = page.locator("textarea, input[type='text'], input[type='number'], input[type='tel'], input:not([type]), select");
        int count = Math.min(fields.count(), 8);
        for (int i = 0; i < count; i++) {
            Locator field = fields.nth(i);
            if (!visible(field)) {
                continue;
            }
            String question = inferQuestion(field);
            if (question.isBlank()) {
                continue;
            }
            activity("Answering application question: " + question);
            String answer = answerProvider.answerFor(question).orElse(null);
            if (answer == null || answer.isBlank()) {
                activity("No saved answer found for question: " + question);
                return AnswerOutcome.NEEDS_USER_INPUT;
            }
            String tagName = field.evaluate("el => el.tagName.toLowerCase()").toString();
            if ("select".equals(tagName)) {
                field.selectOption(answer);
            } else {
                human.type(field, answer);
            }
        }
        return AnswerOutcome.COMPLETE;
    }

    private Locator firstVisible(String selector) {
        Locator locator = page.locator(selector);
        int count = Math.min(locator.count(), 10);
        for (int i = 0; i < count; i++) {
            Locator candidate = locator.nth(i);
            if (visible(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean applicationComplete() {
        String body = page.locator("body").innerText();
        return contains(body, "application sent", "successfully applied", "applied successfully", "application submitted");
    }

    private String inferQuestion(Locator field) {
        try {
            String aria = field.getAttribute("aria-label");
            if (aria != null && !aria.isBlank()) {
                return aria;
            }
            String id = field.getAttribute("id");
            if (id != null && !id.isBlank()) {
                Locator label = page.locator("label[for='" + id + "']").first();
                if (label.count() > 0) {
                    return label.innerText();
                }
            }
            return field.locator("xpath=ancestor::*[self::div or self::li][1]").innerText();
        } catch (Exception exception) {
            return "";
        }
    }

    private boolean captchaDetected() {
        String body = page.locator("body").innerText().toLowerCase(Locale.ROOT);
        return body.contains("captcha") || body.contains("verify you are human") || page.url().toLowerCase(Locale.ROOT).contains("captcha");
    }

    private boolean externalRedirect(String beforeUrl, String afterUrl) {
        return afterUrl != null
                && !afterUrl.isBlank()
                && !afterUrl.contains("naukri.com")
                && !afterUrl.equals(beforeUrl);
    }

    private boolean contains(String source, String... values) {
        String normalized = source == null ? "" : source.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (normalized.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean visible(Locator locator) {
        try {
            return locator.isVisible();
        } catch (Exception exception) {
            return false;
        }
    }

    private JobApplicationResult result(DiscoveredJob job, ApplyStatus status, String reason, boolean redirect, int attempt) {
        return new JobApplicationResult(job.companyName(), job.jobTitle(), Instant.now(), status, page.url(),
                job.experience(), job.salary(), job.location(), redirect, reason, null, 0.0, attempt);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private void activity(String message) {
        activityListener.onActivity(message);
    }

    private enum AnswerOutcome {
        COMPLETE,
        NEEDS_USER_INPUT
    }
}
