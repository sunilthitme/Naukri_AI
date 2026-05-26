package com.naukri.bot.automation.playwright.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.naukri.bot.automation.model.AutomationJobFilter;
import com.naukri.bot.automation.model.DiscoveredJob;
import com.naukri.bot.automation.playwright.HumanBehavior;
import com.naukri.bot.automation.playwright.NaukriUrlBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NaukriSearchPage {
    private final Page page;
    private final HumanBehavior human = new HumanBehavior();
    private final NaukriUrlBuilder urlBuilder = new NaukriUrlBuilder();

    public NaukriSearchPage(Page page) {
        this.page = page;
    }

    public List<DiscoveredJob> search(AutomationJobFilter filter, int limit) {
        page.navigate(urlBuilder.searchUrl(filter),
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForLoadState();
        List<DiscoveredJob> jobs = new ArrayList<>();
        int scrolls = 0;
        while (jobs.size() < limit && scrolls < 8) {
            collectVisibleJobs(filter, jobs, limit);
            human.scroll(page);
            scrolls++;
        }
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
            String url = safeAttribute(card.locator("a[href]").first(), "href");
            if (url == null || url.isBlank()) {
                continue;
            }
            String title = safeText(card.locator("a.title, .title, a[href*='job-listings']").first());
            String company = safeText(card.locator(".comp-name, .companyName, .subTitle").first());
            if (jobs.stream().noneMatch(job -> job.jobUrl().equals(url))) {
                jobs.add(new DiscoveredJob(company, title, absolute(url), "N/A", "N/A", filter.location(), text));
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
}
