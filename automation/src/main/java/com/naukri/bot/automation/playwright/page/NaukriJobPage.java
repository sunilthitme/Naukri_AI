package com.naukri.bot.automation.playwright.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.naukri.bot.automation.QuestionAnswerProvider;
import com.naukri.bot.automation.model.ApplyStatus;
import com.naukri.bot.automation.model.AutomationRunRequest;
import com.naukri.bot.automation.model.DiscoveredJob;
import com.naukri.bot.automation.model.JobApplicationResult;
import com.naukri.bot.automation.playwright.HumanBehavior;

import java.time.Instant;
import java.util.Locale;

public class NaukriJobPage {
    private final Page page;
    private final HumanBehavior human = new HumanBehavior();

    public NaukriJobPage(Page page) {
        this.page = page;
    }

    public JobApplicationResult apply(DiscoveredJob job,
                                      AutomationRunRequest request,
                                      QuestionAnswerProvider answerProvider,
                                      int attempt) {
        page.navigate(job.jobUrl(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForLoadState();
        human.pause();

        if (captchaDetected()) {
            return result(job, ApplyStatus.CAPTCHA_DETECTED, "Captcha detected", false, attempt);
        }
        String body = page.locator("body").innerText();
        if (contains(body, "already applied", "application sent")) {
            return result(job, ApplyStatus.ALREADY_APPLIED, null, false, attempt);
        }
        if (request.dryRun()) {
            return result(job, ApplyStatus.DRY_RUN, "Dry run enabled", false, attempt);
        }

        Locator applyButton = page.locator("button:has-text('Apply'), a:has-text('Apply'), button:has-text('I am interested')").first();
        if (applyButton.count() == 0) {
            return result(job, ApplyStatus.FAILED, "Apply button not found", false, attempt);
        }

        String beforeUrl = page.url();
        applyButton.click();
        page.waitForLoadState();
        human.pause();
        if (externalRedirect(beforeUrl, page.url())) {
            return result(job, ApplyStatus.EXTERNAL_REDIRECT, null, true, attempt);
        }

        AnswerOutcome answerOutcome = answerQuestions(answerProvider);
        if (answerOutcome == AnswerOutcome.NEEDS_USER_INPUT) {
            return result(job, ApplyStatus.QUESTION_NEEDS_ANSWER, "A question needs a saved answer", false, attempt);
        }

        Locator submitButton = page.locator("button:has-text('Submit'), button:has-text('Send'), button:has-text('Apply')").first();
        if (submitButton.count() > 0) {
            submitButton.click();
            page.waitForLoadState();
            human.pause();
        }
        return result(job, ApplyStatus.SUCCESS, null, false, attempt);
    }

    private AnswerOutcome answerQuestions(QuestionAnswerProvider answerProvider) {
        Locator fields = page.locator("textarea, input[type='text'], input:not([type]), select");
        int count = Math.min(fields.count(), 8);
        for (int i = 0; i < count; i++) {
            Locator field = fields.nth(i);
            String question = inferQuestion(field);
            if (question.isBlank()) {
                continue;
            }
            String answer = answerProvider.answerFor(question).orElse(null);
            if (answer == null || answer.isBlank()) {
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

    private JobApplicationResult result(DiscoveredJob job, ApplyStatus status, String reason, boolean redirect, int attempt) {
        return new JobApplicationResult(job.companyName(), job.jobTitle(), Instant.now(), status, page.url(),
                job.experience(), job.salary(), job.location(), redirect, reason, null, 0.0, attempt);
    }

    private enum AnswerOutcome {
        COMPLETE,
        NEEDS_USER_INPUT
    }
}
