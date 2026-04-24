package com.aijobapplyassistant.service.bot;

import com.aijobapplyassistant.config.ApplicationProperties;
import com.aijobapplyassistant.entity.JobPreference;
import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.entity.enums.ApplicationStatus;
import com.aijobapplyassistant.service.SystemLogService;
import com.aijobapplyassistant.service.ai.OllamaAiService;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class PlaywrightJobBotEngine {

    private final ApplicationProperties properties;
    private final OllamaAiService ollamaAiService;
    private final SystemLogService systemLogService;
    private final List<CareerPortalAdapter> portalAdapters;

    public PlaywrightJobBotEngine(ApplicationProperties properties, OllamaAiService ollamaAiService,
            SystemLogService systemLogService, List<CareerPortalAdapter> portalAdapters) {
        this.properties = properties;
        this.ollamaAiService = ollamaAiService;
        this.systemLogService = systemLogService;
        this.portalAdapters = portalAdapters;
    }

    public BotRunResult execute(User user, List<JobPreference> preferences, String resumeText, String resumePath,
            AtomicBoolean stopRequested, String naukriPassword) {
        List<JobProcessingResult> results = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions().setHeadless(properties.getAutomation().isHeadless()));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            loginToNaukri(page, user, naukriPassword);
            if (detectCaptcha(page)) {
                results.add(new JobProcessingResult(
                        new JobListing("Naukri", "Login", "Naukri", page.url(), null, "", false, ""),
                        ApplicationStatus.CAPTCHA_WAITING,
                        0,
                        "Captcha detected during Naukri login. Complete it manually and rerun.",
                        false
                ));
                return summarize(results, false, "Captcha detected during login");
            }

            int remaining = properties.getAutomation().getMaxJobsPerRun();
            for (JobPreference preference : preferences) {
                if (stopRequested.get() || remaining <= 0) {
                    break;
                }

                List<JobListing> listings = searchJobs(page, preference, remaining);
                systemLogService.info(user, "PlaywrightJobBotEngine", "Found " + listings.size() + " jobs for " + preference.getTitle());
                for (JobListing listing : listings) {
                    if (stopRequested.get()) {
                        break;
                    }

                    int score = ollamaAiService.scoreJobMatch(resumeText, listing.description());
                    if (score < preference.getMinimumScore()) {
                        results.add(new JobProcessingResult(listing, ApplicationStatus.SKIPPED, score, "AI relevance score below threshold", false));
                        continue;
                    }

                    page.navigate(listing.jobUrl());
                    humanDelay(page);

                    PortalSubmissionResult submission = attemptApply(page, user, listing, resumePath);
                    results.add(new JobProcessingResult(listing, submission.status(), score, submission.remarks(), submission.credentialCreated()));
                    remaining--;
                    if (remaining <= 0) {
                        break;
                    }
                }
            }
            return summarize(results, stopRequested.get(), stopRequested.get() ? "Stopped by user" : "Automation completed");
        } catch (Exception exception) {
            systemLogService.error(user, "PlaywrightJobBotEngine", "Automation run failed", exception.getMessage());
            return new BotRunResult(0, 0, 0, 1, 0, false, exception.getMessage(), List.of());
        }
    }

    private void loginToNaukri(Page page, User user, String naukriPassword) {
        page.navigate("https://www.naukri.com/nlogin/login");
        humanDelay(page);
        fillFirstVisible(page, user.getNaukriUsername(), "input[type='email']", "input[placeholder*='Email']", "input[type='text']");
        fillFirstVisible(page, naukriPassword == null ? "" : naukriPassword, "input[type='password']");
        clickFirstVisible(page, "button:has-text('Login')", "button[type='submit']", "button:has-text('Sign in')");
        page.waitForTimeout(2000);
    }

    private List<JobListing> searchJobs(Page page, JobPreference preference, int limit) {
        String query = URLEncoder.encode((preference.getTitle() + " " + safe(preference.getSkills()) + " " + safe(preference.getKeywords())).trim(),
                StandardCharsets.UTF_8);
        page.navigate("https://www.naukri.com/jobs-in-india?k=" + query);
        humanDelay(page);

        List<JobListing> listings = new ArrayList<>();
        Locator cards = page.locator("article, .srp-jobtuple-wrapper, .jobTuple");
        int total = Math.min(cards.count(), limit);
        for (int index = 0; index < total; index++) {
            Locator card = cards.nth(index);
            String description = safe(card.innerText());
            String title = safe(textFrom(card, "a[title]", "h2 a", "a"));
            String company = safe(textFrom(card, ".comp-name", ".companyInfo", "span"));
            String jobUrl = safe(attrFrom(card, "a[title]", "href"));
            if (jobUrl.isBlank()) {
                jobUrl = safe(attrFrom(card, "a", "href"));
            }
            boolean easyApply = description.toLowerCase().contains("easy apply");
            if (!jobUrl.isBlank()) {
                listings.add(new JobListing(
                        company.isBlank() ? "Unknown Company" : company,
                        title.isBlank() ? preference.getTitle() : title,
                        "Naukri",
                        jobUrl,
                        null,
                        description,
                        easyApply,
                        ""
                ));
            }
        }
        return listings;
    }

    private PortalSubmissionResult attemptApply(Page page, User user, JobListing listing, String resumePath) {
        if (detectCaptcha(page)) {
            return new PortalSubmissionResult(ApplicationStatus.CAPTCHA_WAITING, "Captcha detected while opening job", false);
        }

        boolean applyClicked = clickFirstVisible(page,
                "button:has-text('Apply')",
                "a:has-text('Apply')",
                "button:has-text('Easy Apply')",
                "a:has-text('Proceed')"
        );

        if (applyClicked) {
            page.waitForTimeout(1500);
        }

        if (!page.url().toLowerCase().contains("naukri.com")) {
            CareerPortalAdapter adapter = portalAdapters.stream()
                    .filter(item -> item.supports(page.url()))
                    .findFirst()
                    .orElseThrow();
            return adapter.apply(page, user, listing, resumePath);
        }

        CareerPortalAdapter adapter = portalAdapters.stream()
                .filter(item -> item instanceof GenericCareerPortalAdapter)
                .findFirst()
                .orElseThrow();
        return adapter.apply(page, user, listing, resumePath);
    }

    private BotRunResult summarize(List<JobProcessingResult> results, boolean stopped, String summary) {
        int applied = (int) results.stream().filter(item -> item.status() == ApplicationStatus.APPLIED).count();
        int skipped = (int) results.stream().filter(item -> item.status() == ApplicationStatus.SKIPPED).count();
        int failed = (int) results.stream().filter(item ->
                item.status() == ApplicationStatus.FAILED || item.status() == ApplicationStatus.CAPTCHA_WAITING).count();
        int credentialsCreated = (int) results.stream().filter(JobProcessingResult::credentialCreated).count();
        return new BotRunResult(results.size(), applied, skipped, failed, credentialsCreated, stopped, summary, results);
    }

    private void humanDelay(Page page) {
        int min = properties.getAutomation().getHumanDelayMinMs();
        int max = properties.getAutomation().getHumanDelayMaxMs();
        int delay = java.util.concurrent.ThreadLocalRandom.current().nextInt(min, Math.max(min + 1, max));
        page.waitForTimeout(delay);
    }

    private boolean detectCaptcha(Page page) {
        String content = safe(page.content()).toLowerCase();
        return content.contains("captcha") || content.contains("i am not a robot");
    }

    private void fillFirstVisible(Page page, String value, String... selectors) {
        for (String selector : selectors) {
            Locator locator = page.locator(selector);
            if (locator.count() > 0) {
                locator.first().fill(value);
                return;
            }
        }
    }

    private boolean clickFirstVisible(Page page, String... selectors) {
        for (String selector : selectors) {
            Locator locator = page.locator(selector);
            if (locator.count() > 0) {
                locator.first().click();
                return true;
            }
        }
        return false;
    }

    private String textFrom(Locator scope, String... selectors) {
        for (String selector : selectors) {
            Locator locator = scope.locator(selector);
            if (locator.count() > 0) {
                return safe(locator.first().innerText());
            }
        }
        return "";
    }

    private String attrFrom(Locator scope, String selector, String attribute) {
        Locator locator = scope.locator(selector);
        if (locator.count() > 0) {
            String value = locator.first().getAttribute(attribute);
            return value == null ? "" : value;
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
