package com.naukri.bot.automation.playwright;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Proxy;
import com.naukri.bot.automation.AutomationControl;
import com.naukri.bot.automation.AutomationActivityListener;
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
    public AutomationRunResult run(AutomationRunRequest request,
                                   QuestionAnswerProvider questionAnswerProvider,
                                   AutomationControl control,
                                   AutomationActivityListener activityListener) {
        AutomationRunResult result = new AutomationRunResult();
        try (Playwright playwright = Playwright.create()) {
            activity(activityListener, "Starting Playwright browser");
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(request.headless());
            if (request.proxy() != null && request.proxy().enabled()) {
                activity(activityListener, "Configuring browser proxy");
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

            NaukriLoginPage loginPage = new NaukriLoginPage(page, activityListener);
            LoginTestResult loginResult = loginPage.login(request.naukriEmail(), request.naukriPassword());
            if (!loginResult.success()
                    && request.manualLoginOnCaptcha()
                    && manualLoginAllowed(loginResult.status())) {
                activity(activityListener, "Switching to manual login recovery mode");
                loginResult = loginPage.waitForManualLogin(request.manualLoginTimeoutSeconds());
            }
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
                context.close();
                browser.close();
                return result;
            }

            NaukriSearchPage searchPage = new NaukriSearchPage(page, activityListener);
            List<DiscoveredJob> jobs = searchPage.search(request.filter(), request.filter().dailyApplyLimit());
            result.getMessages().add("Discovered " + jobs.size() + " jobs from search filters.");

            int processed = 0;
            for (DiscoveredJob job : jobs) {
                if (control.shouldStop() || processed >= request.filter().dailyApplyLimit()) {
                    activity(activityListener, "Stop requested or daily apply limit reached");
                    break;
                }
                activity(activityListener, "Preparing job " + (processed + 1) + " of " + jobs.size() + ": " + job.jobTitle());
                control.waitIfPaused();
                JobApplicationResult applicationResult = applyWithRetry(context, request, questionAnswerProvider, job, activityListener);
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
            activity(activityListener, "Automation failed: " + exception.getMessage());
            log.error("Automation failed", exception);
            result.getMessages().add(exception.getMessage());
        } finally {
            activity(activityListener, "Automation run finished");
            result.finish();
        }
        return result;
    }

    @Override
    public LoginTestResult testLogin(AutomationRunRequest request, AutomationActivityListener activityListener) {
        try (Playwright playwright = Playwright.create()) {
            activity(activityListener, "Starting Playwright browser for login test");
            Browser browser = playwright.chromium().launch(launchOptions(request));
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1366, 768)
                    .setIgnoreHTTPSErrors(true));
            Page page = context.newPage();
            NaukriLoginPage loginPage = new NaukriLoginPage(page, activityListener);
            LoginTestResult result = loginPage.login(request.naukriEmail(), request.naukriPassword());
            if (!result.success() && request.manualLoginOnCaptcha() && manualLoginAllowed(result.status())) {
                activity(activityListener, "Switching to manual login recovery mode");
                result = loginPage.waitForManualLogin(request.manualLoginTimeoutSeconds());
            }
            if (!result.success()) {
                result = result.withScreenshot(screenshot(page, request.storageDirectory(), "naukri_login_test", 1));
            }
            activity(activityListener, result.success() ? "Naukri login test succeeded" : result.message());
            context.close();
            browser.close();
            return result;
        } catch (Exception exception) {
            activity(activityListener, "Naukri login test failed: " + exception.getMessage());
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
                                                DiscoveredJob job,
                                                AutomationActivityListener activityListener) {
        int maxAttempts = Math.max(1, request.maxRetries() + 1);
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            Page jobPage = context.newPage();
            try {
                NaukriJobPage naukriJobPage = new NaukriJobPage(jobPage, activityListener);
                JobApplicationResult outcome = naukriJobPage.apply(job, request, questionAnswerProvider, attempt);
                jobPage.close();
                return outcome;
            } catch (Exception exception) {
                lastFailure = exception;
                activity(activityListener, "Job failed on attempt " + attempt + ": " + exception.getMessage());
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

    private void activity(AutomationActivityListener activityListener, String message) {
        activityListener.onActivity(message);
    }

    private boolean manualLoginAllowed(LoginStatus status) {
        return status == LoginStatus.CAPTCHA_DETECTED
                || status == LoginStatus.OTP_REQUIRED
                || status == LoginStatus.LOGIN_FORM_NOT_FOUND
                || status == LoginStatus.STILL_ON_LOGIN_PAGE;
    }
}
