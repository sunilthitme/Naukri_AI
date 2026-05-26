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
import com.naukri.bot.automation.model.LoginStatus;
import com.naukri.bot.automation.model.LoginTestResult;
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
            LoginTestResult loginResult = loginPage.login(request.naukriEmail(), request.naukriPassword());
            result.getMessages().add(loginResult.message());
            if (!loginResult.success()) {
                LoginTestResult withScreenshot = loginResult.withScreenshot(screenshot(page, request.storageDirectory(), "naukri_login", 1));
                result.setFailureReason(withScreenshot.message());
                result.setLoginFailed(true);
                result.getMessages().add("Naukri login failed: " + withScreenshot.message());
                if (withScreenshot.status() == LoginStatus.CAPTCHA_DETECTED) {
                    result.setCaptchaDetected(true);
                }
                result.finish();
                context.close();
                browser.close();
                return result;
            }
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
    public LoginTestResult testLogin(AutomationRunRequest request) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(launchOptions(request));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1366, 768)
                    .setIgnoreHTTPSErrors(true));
            Page page = context.newPage();
            LoginTestResult result = new NaukriLoginPage(page).login(request.naukriEmail(), request.naukriPassword());
            if (!result.success()) {
                result = result.withScreenshot(screenshot(page, request.storageDirectory(), "naukri_login_test", 1));
            }
            context.close();
            browser.close();
            return result;
        } catch (Exception exception) {
            log.error("Naukri login test failed", exception);
            return new LoginTestResult(false, LoginStatus.FAILED,
                    "Naukri login test failed: " + exception.getMessage(), null, null);
        }
    }

    private BrowserType.LaunchOptions launchOptions(AutomationRunRequest request) {
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(request.headless());
        if (request.proxy() != null && request.proxy().enabled()) {
            Proxy proxy = new Proxy(request.proxy().server());
            if (request.proxy().username() != null && !request.proxy().username().isBlank()) {
                proxy.setUsername(request.proxy().username());
                proxy.setPassword(request.proxy().password());
            }
            launchOptions.setProxy(proxy);
        }
        return launchOptions;
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
