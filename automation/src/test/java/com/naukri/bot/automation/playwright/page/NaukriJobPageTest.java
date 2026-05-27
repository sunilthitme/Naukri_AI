package com.naukri.bot.automation.playwright.page;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.naukri.bot.automation.model.ApplyStatus;
import com.naukri.bot.automation.model.AutomationJobFilter;
import com.naukri.bot.automation.model.AutomationRunRequest;
import com.naukri.bot.automation.model.DiscoveredJob;
import com.naukri.bot.automation.model.JobApplicationResult;
import com.naukri.bot.automation.model.ProxySettings;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaukriJobPageTest {
    @Test
    void detectsAlreadyAppliedButtonBeforeLookingForApplyButton() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            NaukriJobPage jobPage = new NaukriJobPage(page);

            JobApplicationResult result = jobPage.apply(
                    new DiscoveredJob("Example Co", "Java Developer", dataUrl("""
                            <!doctype html>
                            <html>
                              <body>
                                <main>
                                  <h1>Java Developer</h1>
                                  <button>Applied</button>
                                </main>
                              </body>
                            </html>
                            """), "3-5 years", "Not disclosed", "Pune", ""),
                    request(),
                    question -> Optional.empty(),
                    1);

            assertEquals(ApplyStatus.ALREADY_APPLIED, result.status());
        }
    }

    @Test
    void asksForUnknownTextQuestionAndSubmitsSavedAnswer() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            NaukriJobPage jobPage = new NaukriJobPage(page);
            AtomicReference<String> askedQuestion = new AtomicReference<>();

            JobApplicationResult result = jobPage.apply(
                    new DiscoveredJob("Example Co", "Java Developer", dataUrl("""
                            <!doctype html>
                            <html>
                              <body>
                                <button onclick="document.body.insertAdjacentHTML('beforeend', `
                                  <div role='dialog'>
                                    <label for='notice'>What is your notice period?</label>
                                    <input id='notice' type='text'>
                                    <button onclick=&quot;document.body.innerHTML='Application sent'&quot;>Continue</button>
                                  </div>`)">Apply</button>
                              </body>
                            </html>
                            """), "3-5 years", "Not disclosed", "Pune", ""),
                    request(),
                    question -> {
                        askedQuestion.set(question);
                        return Optional.of("30 days");
                    },
                    1);

            assertEquals(ApplyStatus.SUCCESS, result.status());
            assertTrue(askedQuestion.get().contains("notice period"));
        }
    }

    @Test
    void answersChoiceQuestionFromApplicationDialog() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            NaukriJobPage jobPage = new NaukriJobPage(page);
            AtomicReference<String> askedQuestion = new AtomicReference<>();

            JobApplicationResult result = jobPage.apply(
                    new DiscoveredJob("Example Co", "Java Developer", dataUrl("""
                            <!doctype html>
                            <html>
                              <body>
                                <button onclick="document.body.insertAdjacentHTML('beforeend', `
                                  <div role='dialog'>
                                    <p>Are you willing to relocate?</p>
                                    <span id='answer'></span>
                                    <button onclick=&quot;document.getElementById('answer').textContent='yes'&quot;>Yes</button>
                                    <button onclick=&quot;document.getElementById('answer').textContent='no'&quot;>No</button>
                                    <button onclick=&quot;if (document.getElementById('answer').textContent) document.body.innerHTML='Applied successfully'&quot;>Continue</button>
                                  </div>`)">Apply</button>
                              </body>
                            </html>
                            """), "3-5 years", "Not disclosed", "Pune", ""),
                    request(),
                    question -> {
                        askedQuestion.set(question);
                        return Optional.of("yes");
                    },
                    1);

            assertEquals(ApplyStatus.SUCCESS, result.status());
            assertTrue(askedQuestion.get().contains("relocate"));
        }
    }

    private AutomationRunRequest request() {
        return new AutomationRunRequest(
                "candidate@example.com",
                "secret",
                new AutomationJobFilter("Java", "", "Pune", "", "", "", List.of(), List.of(),
                        true, false, 1, true, 30),
                List.of(),
                Path.of("target", "test-storage"),
                true,
                false,
                false,
                60,
                new ProxySettings(null, null, null, null),
                0);
    }

    private String dataUrl(String html) {
        return "data:text/html;base64," + Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
    }
}
