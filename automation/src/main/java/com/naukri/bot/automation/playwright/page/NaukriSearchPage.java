package com.naukri.bot.automation.playwright.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.naukri.bot.automation.AutomationActivityListener;
import com.naukri.bot.automation.model.AutomationJobFilter;
import com.naukri.bot.automation.model.DiscoveredJob;
import com.naukri.bot.automation.playwright.HumanBehavior;
import com.naukri.bot.automation.playwright.NaukriUrlBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NaukriSearchPage {
    private static final List<String> CURRENT_COMPANY_NAMES = List.of("tata consultancy services", "tcs");

    private final Page page;
    private final AutomationActivityListener activityListener;
    private final HumanBehavior human = new HumanBehavior();
    private final NaukriUrlBuilder urlBuilder = new NaukriUrlBuilder();

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
        page.navigate(searchUrl,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        activity("Waiting for Naukri search results to load");
        page.waitForLoadState();
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
        Locator cards = page.locator("article, .srp-jobtuple-wrapper, .jobTuple, [data-job-id]");
        int count = Math.min(cards.count(), limit * 2);
        for (int i = 0; i < count && jobs.size() < limit; i++) {
            Locator card = cards.nth(i);
            String text = safeText(card);
            if (text.isBlank() || blacklisted(text, filter)) {
                continue;
            }
            String company = safeText(card.locator(".comp-name, .companyName, .subTitle").first());
            if (currentCompany(company) || (company.isBlank() && currentCompany(text))) {
                activity("Skipping current company: " + safe(company));
                continue;
            }
            String url = safeAttribute(card.locator("a[href]").first(), "href");
            if (url == null || url.isBlank()) {
                continue;
            }
            String title = safeText(card.locator("a.title, .title, a[href*='job-listings']").first());
            if (jobs.stream().noneMatch(job -> job.jobUrl().equals(url))) {
                jobs.add(new DiscoveredJob(company, title, absolute(url), "N/A", "N/A", filter.location(), text));
                activity("Discovered job: " + safe(title) + " at " + safe(company));
            }
        }
    }

    private boolean blacklisted(String text, AutomationJobFilter filter) {
        if (filter.blacklistedCompanies() == null) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return filter.blacklistedCompanies().stream()
                .filter(company -> company != null && !company.isBlank())
                .map(company -> company.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
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

    private String absolute(String url) {
        if (url.startsWith("http")) {
            return url;
        }
        return "https://www.naukri.com" + (url.startsWith("/") ? url : "/" + url);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private void activity(String message) {
        activityListener.onActivity(message);
    }
}
