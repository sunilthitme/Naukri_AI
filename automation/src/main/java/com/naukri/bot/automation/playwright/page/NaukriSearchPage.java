package com.naukri.bot.automation.playwright.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.naukri.bot.automation.AutomationActivityListener;
import com.naukri.bot.automation.model.AutomationJobFilter;
import com.naukri.bot.automation.model.DiscoveredJob;
import com.naukri.bot.automation.playwright.HumanBehavior;
import com.naukri.bot.automation.playwright.NaukriResultFilterMatcher;
import com.naukri.bot.automation.playwright.NaukriUrlBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NaukriSearchPage {
    private static final List<String> CURRENT_COMPANY_NAMES = List.of("tata consultancy services", "tcs");
    private static final String JOB_CARD_SELECTORS = ".srp-jobtuple-wrapper, article[class*='jobTuple'], "
            + ".jobTuple, [data-job-id], article";

    private final Page page;
    private final AutomationActivityListener activityListener;
    private final HumanBehavior human = new HumanBehavior();
    private final NaukriUrlBuilder urlBuilder = new NaukriUrlBuilder();
    private final NaukriResultFilterMatcher filterMatcher = new NaukriResultFilterMatcher();

    public NaukriSearchPage(Page page) {
        this(page, AutomationActivityListener.NOOP);
    }

    public NaukriSearchPage(Page page, AutomationActivityListener activityListener) {
        this.page = page;
        this.activityListener = activityListener;
    }

    public List<DiscoveredJob> search(AutomationJobFilter filter, int limit) {
        String searchUrl = urlBuilder.searchUrl(filter);
        activity("Opening Naukri job search for: " + safe(filter.keywords()));
        activity("Applying saved filters: " + filterSummary(filter));
        activity("Resolved Naukri search URL: " + searchUrl);
        page.navigate(searchUrl,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        activity("Waiting for Naukri search results to load");
        page.waitForLoadState();
        page.waitForTimeout(1_500);
        applySearchBarFallback(filter);
        activity("Naukri current search URL: " + page.url());
        List<DiscoveredJob> jobs = new ArrayList<>();
        int scrolls = 0;
        while (jobs.size() < limit && scrolls < 8) {
            activity("Scanning visible job cards. Found " + jobs.size() + " so far");
            collectVisibleJobs(filter, jobs, limit);
            activity("Scrolling Naukri results page");
            human.scroll(page);
            scrolls++;
        }
        activity("Naukri search completed. Found " + jobs.size() + " candidate jobs");
        return jobs;
    }

    private void collectVisibleJobs(AutomationJobFilter filter, List<DiscoveredJob> jobs, int limit) {
        Locator cards = page.locator(JOB_CARD_SELECTORS);
        int count = Math.min(cards.count(), Math.max(limit * 8, 40));
        for (int i = 0; i < count && jobs.size() < limit; i++) {
            Locator card = cards.nth(i);
            String text = safeText(card);
            if (text.isBlank()) {
                continue;
            }
            String company = safeText(card.locator(".comp-name, .companyName, .subTitle").first());
            if (currentCompany(company) || (company.isBlank() && currentCompany(text))) {
                activity("Skipping current company: " + safe(company));
                continue;
            }
            String url = safeAttribute(card.locator("a.title[href], a[href*='job-listings'], a[href*='jobs']").first(), "href");
            if (url == null || url.isBlank()) {
                continue;
            }
            String title = safeText(card.locator("a.title, .title, a[href*='job-listings']").first());
            NaukriResultFilterMatcher.MatchDecision decision = filterMatcher.evaluate(filter, company, title, text);
            if (!decision.accepted()) {
                activity("Skipping job because " + decision.reason() + ": " + safe(title) + " at " + safe(company));
                continue;
            }
            if (jobs.stream().noneMatch(job -> job.jobUrl().equals(url))) {
                String experience = firstText(card, ".expwdth, .experience, [class*='experience'], [class*='exp']");
                String salary = firstText(card, ".sal-wrap, .salary, [class*='salary'], [class*='sal']");
                String location = firstText(card, ".locWdth, .location, [class*='location'], [class*='loc']");
                jobs.add(new DiscoveredJob(company, title, absolute(url), safe(experience), safe(salary),
                        location.isBlank() ? filter.location() : location, text));
                activity("Discovered job: " + safe(title) + " at " + safe(company));
            }
        }
    }

    private void applySearchBarFallback(AutomationJobFilter filter) {
        try {
            boolean appliedKeyword = fillFirstVisible(
                    "input[placeholder*='skills' i], input[placeholder*='designation' i], "
                            + "input[placeholder*='keyword' i], input[id*='keyword' i], input[name*='keyword' i]",
                    filter.keywords());
            boolean appliedLocation = fillFirstVisible(
                    "input[placeholder*='location' i], input[id*='location' i], input[name*='location' i]",
                    filter.location());
            if (!appliedKeyword && !appliedLocation) {
                return;
            }
            Locator searchButton = firstVisible("button:has-text('Search'), [role='button']:has-text('Search'), .qsbSubmit");
            if (searchButton == null) {
                return;
            }
            activity("Confirmed filters in visible Naukri search bar; refreshing results");
            searchButton.click();
            page.waitForLoadState();
            page.waitForTimeout(1_500);
        } catch (Exception exception) {
            activity("Search bar filter fallback skipped: " + exception.getMessage());
        }
    }

    private boolean fillFirstVisible(String selector, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        Locator locator = firstVisible(selector);
        if (locator == null) {
            return false;
        }
        String existing = safeInputValue(locator);
        if (existing.toLowerCase(Locale.ROOT).contains(value.toLowerCase(Locale.ROOT))) {
            return false;
        }
        locator.click();
        locator.fill("");
        locator.pressSequentially(value);
        return true;
    }

    private Locator firstVisible(String selector) {
        try {
            Locator locator = page.locator(selector);
            int count = Math.min(locator.count(), 12);
            for (int i = 0; i < count; i++) {
                Locator candidate = locator.nth(i);
                if (candidate.isVisible() && candidate.isEnabled()) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String safeInputValue(Locator locator) {
        try {
            Object value = locator.evaluate("el => el.value || el.getAttribute('value') || ''");
            return value == null ? "" : value.toString();
        } catch (Exception exception) {
            return "";
        }
    }

    private boolean currentCompany(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return CURRENT_COMPANY_NAMES.stream().anyMatch(company -> {
            if ("tcs".equals(company)) {
                return normalized.matches(".*\\btcs\\b.*");
            }
            return normalized.contains(company);
        });
    }

    private String safeText(Locator locator) {
        try {
            if (locator.count() == 0) {
                return "";
            }
            return locator.first().innerText().trim();
        } catch (Exception exception) {
            return "";
        }
    }

    private String safeAttribute(Locator locator, String attribute) {
        try {
            if (locator.count() == 0) {
                return null;
            }
            return locator.getAttribute(attribute);
        } catch (Exception exception) {
            return null;
        }
    }

    private String firstText(Locator root, String selector) {
        try {
            Locator locator = root.locator(selector);
            int count = Math.min(locator.count(), 8);
            for (int i = 0; i < count; i++) {
                String text = safeText(locator.nth(i));
                if (!text.isBlank()) {
                    return text;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String absolute(String url) {
        if (url.startsWith("http")) {
            return url;
        }
        return "https://www.naukri.com" + (url.startsWith("/") ? url : "/" + url);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private String filterSummary(AutomationJobFilter filter) {
        return "keywords=" + safe(filter.keywords())
                + ", experience=" + safe(filter.experience())
                + ", location=" + safe(filter.location())
                + ", salary=" + safe(filter.salary())
                + ", workMode=" + safe(filter.workMode())
                + ", freshness=" + safe(filter.freshness())
                + ", easyApplyOnly=" + filter.easyApplyOnly()
                + ", dailyLimit=" + filter.dailyApplyLimit();
    }

    private void activity(String message) {
        activityListener.onActivity(message);
    }
}
