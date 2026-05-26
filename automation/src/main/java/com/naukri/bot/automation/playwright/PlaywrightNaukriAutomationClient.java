package com.naukri.bot.automation.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Proxy;
import com.naukri.bot.automation.AutomationControl;
import com.naukri.bot.automation.NaukriAutomationClient;
import com.naukri.bot.automation.QuestionAnswerProvider;
import com.naukri.bot.automation.model.ApplyStatus;
import com.naukri.bot.automation.model.AutomationRunRequest;
import com.naukri.bot.automation.model.AutomationRunResult;
import com.naukri.bot.automation.model.DiscoveredJob;
import com.naukri.bot.automation.model.ExternalRedirectResult;
import com.naukri.bot.automation.model.JobApplicationResult;
import com.naukri.bot.automation.playwright.page.NaukriJobPage;
import com.naukri.bot.automation.playwright.page.NaukriLoginPage;
import com.naukri.bot.automation.playwright.page.NaukriSearchPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public class PlaywrightNaukriAutomationClient implements NaukriAutomationClient {
    private static final Logger log = LoggerFactory.getLogger(PlaywrightNaukriAutomationClient.class);

    @Override
    public AutomationRunResult run(AutomationRunRequest request, QuestionAnswerProvider questionAnswerProvider, AutomationControl control) {
        AutomationRunResult result = new AutomationRunResult();
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(request.headless());
            if (request.proxy() != null && request.proxy().enabled()) {
                Proxy proxy = new Proxy(request.proxy().server());
                if (request.proxy().username() != null && !request.proxy().username().isBlank()) {
                    proxy.setUsername(request.proxy().username());
                    proxy.setPassword(request.proxy().password());
                }
                launchOptions.setProxy(proxy);
            }

            Browser browser = playwright.chromium().launch(launchOptions);
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1366, 768)
                    .setIgnoreHTTPSErrors(true));
            Page page = context.newPage();

            NaukriLoginPage loginPage = new NaukriLoginPage(page);
            loginPage.login(request.naukriEmail(), request.naukriPassword());
            if (loginPage.captchaDetected()) {
                result.setCaptchaDetected(true);
                result.getMessages().add("Captcha detected during login. Automation paused for manual action.");
                result.finish();
                return result;
            }

            NaukriSearchPage searchPage = new NaukriSearchPage(page);
            List<DiscoveredJob> jobs = searchPage.search(request.filter(), request.filter().dailyApplyLimit());
            result.getMessages().add("Discovered " + jobs.size() + " jobs from search filters.");

            int processed = 0;
            for (DiscoveredJob job : jobs) {
                if (control.shouldStop() || processed >= request.filter().dailyApplyLimit()) {
                    break;
                }
                control.waitIfPaused();
                JobApplicationResult applicationResult = applyWithRetry(context, request, questionAnswerProvider, job);
                result.getJobResults().add(applicationResult);
                if (applicationResult.redirectedExternalSite()) {
                    result.getExternalRedirects().add(new ExternalRedirectResult(
                            applicationResult.companyName(),
                            applicationResult.jobUrl(),
                            Instant.now(),
                            ApplyStatus.EXTERNAL_REDIRECT));
                }
                processed++;
            }

            context.close();
            browser.close();
        } catch (Exception exception) {
            log.error("Automation failed", exception);
            result.getMessages().add(exception.getMessage());
        } finally {
            result.finish();
        }
        return result;
    }

    @Override
    public boolean testLogin(AutomationRunRequest request) {
        AutomationRunResult result = run(request, question -> java.util.Optional.empty(), new AutomationControl() {
            @Override
            public boolean shouldStop() {
                return true;
            }

            @Override
            public boolean isPaused() {
                return false;
            }
        });
        return !result.isCaptchaDetected() && result.getMessages().stream().noneMatch(message -> message.toLowerCase().contains("login"));
    }

    private JobApplicationResult applyWithRetry(BrowserContext context,
                                                AutomationRunRequest request,
                                                QuestionAnswerProvider questionAnswerProvider,
                                                DiscoveredJob job) {
        int maxAttempts = Math.max(1, request.maxRetries() + 1);
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Page jobPage = context.newPage();
            try {
                NaukriJobPage naukriJobPage = new NaukriJobPage(jobPage);
                JobApplicationResult outcome = naukriJobPage.apply(job, request, questionAnswerProvider, attempt);
                jobPage.close();
                return outcome;
            } catch (Exception exception) {
                lastFailure = exception;
                String screenshot = screenshot(jobPage, request.storageDirectory(), job.companyName(), attempt);
                jobPage.close();
                if (attempt == maxAttempts) {
                    return new JobApplicationResult(
                            job.companyName(),
                            job.jobTitle(),
                            Instant.now(),
                            ApplyStatus.FAILED,
                            job.jobUrl(),
                            job.experience(),
                            job.salary(),
                            job.location(),
                            false,
                            exception.getMessage(),
                            screenshot,
                            0.0,
                            attempt);
                }
            }
        }
        return new JobApplicationResult(job.companyName(), job.jobTitle(), Instant.now(), ApplyStatus.FAILED,
                job.jobUrl(), job.experience(), job.salary(), job.location(), false,
                lastFailure == null ? "Unknown failure" : lastFailure.getMessage(), null, 0.0, maxAttempts);
    }

    private String screenshot(Page page, Path storageDirectory, String companyName, int attempt) {
        try {
            Path directory = storageDirectory.resolve("screenshots");
            Files.createDirectories(directory);
            String cleanName = companyName == null ? "job" : companyName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = directory.resolve(cleanName + "_" + attempt + "_" + System.currentTimeMillis() + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(target).setFullPage(true));
            return target.toString();
        } catch (Exception screenshotFailure) {
            log.warn("Unable to capture failure screenshot", screenshotFailure);
            return null;
        }
    }
}
