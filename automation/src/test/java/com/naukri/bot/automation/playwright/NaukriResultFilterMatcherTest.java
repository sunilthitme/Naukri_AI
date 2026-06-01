package com.naukri.bot.automation.playwright;

import com.naukri.bot.automation.model.AutomationJobFilter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaukriResultFilterMatcherTest {
    private final NaukriResultFilterMatcher matcher = new NaukriResultFilterMatcher();

    @Test
    void acceptsJobMatchingSavedFilters() {
        AutomationJobFilter filter = filter("Java Spring Boot", "5", "Pune", "20", "Hybrid", List.of(), List.of());

        NaukriResultFilterMatcher.MatchDecision decision = matcher.evaluate(filter,
                "Acme",
                "Senior Java Spring Developer",
                "Acme Senior Java Spring Developer 3-7 yrs 15-25 lacs pa Pune Hybrid");

        assertTrue(decision.accepted());
    }

    @Test
    void rejectsJobOutsideSavedLocationAndWorkMode() {
        AutomationJobFilter filter = filter("Java Spring Boot", "5", "Pune", "", "Remote", List.of(), List.of());

        NaukriResultFilterMatcher.MatchDecision decision = matcher.evaluate(filter,
                "Acme",
                "Java Developer",
                "Acme Java Developer 3-7 yrs Bengaluru Hybrid");

        assertFalse(decision.accepted());
    }

    @Test
    void respectsPreferredAndBlacklistedCompanies() {
        AutomationJobFilter preferred = filter("Java", "", "", "", "", List.of("Good Corp"), List.of());
        AutomationJobFilter blacklisted = filter("Java", "", "", "", "", List.of(), List.of("Blocked Corp"));

        assertFalse(matcher.evaluate(preferred, "Other Corp", "Java Developer", "Other Corp Java Developer").accepted());
        assertFalse(matcher.evaluate(blacklisted, "Blocked Corp", "Java Developer", "Blocked Corp Java Developer").accepted());
    }

    private AutomationJobFilter filter(String keywords,
                                       String experience,
                                       String location,
                                       String salary,
                                       String workMode,
                                       List<String> preferredCompanies,
                                       List<String> blacklistedCompanies) {
        return new AutomationJobFilter(keywords, experience, location, salary, workMode, "1",
                preferredCompanies, blacklistedCompanies, true, false, 10, true, 30);
    }
}
