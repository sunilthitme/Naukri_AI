package com.naukri.bot.automation.playwright;

import com.naukri.bot.automation.model.AutomationJobFilter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NaukriUrlBuilderTest {
    @Test
    void buildsSearchUrlWithEncodedFilters() {
        AutomationJobFilter filter = new AutomationJobFilter(
                "java spring boot",
                "5",
                "Bengaluru",
                "20",
                "Remote",
                "7",
                List.of("Acme"),
                List.of("Blocked"),
                true,
                false,
                10,
                true,
                30);

        String url = new NaukriUrlBuilder().searchUrl(filter);

        assertTrue(url.contains("k=java+spring+boot"));
        assertTrue(url.contains("l=Bengaluru"));
        assertTrue(url.contains("experience=5"));
        assertTrue(url.contains("ctcFilter=20"));
        assertTrue(url.contains("jobAge=7"));
        assertTrue(url.contains("wfhType=2"));
        assertTrue(url.startsWith("https://www.naukri.com/java-spring-boot-jobs-in-bengaluru?"));
    }
}
