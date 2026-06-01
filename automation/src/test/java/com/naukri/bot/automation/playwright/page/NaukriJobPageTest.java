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
import java.util.ArrayList;
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

    @Test
    void answersNaukriStyleRadioQuestionWhenInputIsHidden() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            List<String> activities = new ArrayList<>();
            NaukriJobPage jobPage = new NaukriJobPage(page, activities::add);
            AtomicReference<String> askedQuestion = new AtomicReference<>();

            JobApplicationResult result = jobPage.apply(
                    new DiscoveredJob("Example Co", "Java Developer", dataUrl("""
                            <!doctype html>
                            <html>
                              <body>
                                <button onclick="document.body.insertAdjacentHTML('beforeend', `
                                  <div role='dialog'>
                                    <p>Are you currently residing in Bengaluru, Karnataka or willing to relocate to Bengaluru, Karnataka?</p>
                                    <div>
                                      <input id='Yes' type='radio' name='relocate' value='Yes' style='display:none'
                                        onclick=&quot;document.getElementById('answer').textContent='yes'&quot;>
                                      <span>Yes</span>
                                    </div>
                                    <div>
                                      <input id='No' type='radio' name='relocate' value='No' style='display:none'>
                                      <span>No</span>
                                    </div>
                                    <span id='answer'></span>
                                    <button onclick=&quot;if (document.getElementById('answer').textContent) document.body.innerHTML='Application sent'&quot;>Save</button>
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

            assertEquals(ApplyStatus.SUCCESS, result.status(), String.join(" | ", activities));
            assertTrue(askedQuestion.get().contains("Bengaluru"));
        }
    }

    @Test
    void submitsNaukriDrawerAfterCheckboxChoice() {
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
                                  <section class='ssrc__drawer'>
                                    <p>Please select the city you are currently residing or willing to relocate to</p>
                                    <div class='ssrc__checkbox-list'>
                                      <div class='ssrc__checkbox-wrap'><input id='bengaluru' type='checkbox' value='Bengaluru'><span>Bengaluru</span></div>
                                      <div class='ssrc__checkbox-wrap'><input id='chennai' type='checkbox' value='Chennai'><span>Chennai</span></div>
                                      <div class='ssrc__checkbox-wrap'><input id='pune' type='checkbox' value='Pune'
                                        onclick=&quot;document.getElementById('answer').textContent='Pune'; setTimeout(() => document.getElementById('save').disabled = false, 400)&quot;><span>Pune</span></div>
                                    </div>
                                    <span id='answer'></span>
                                    <footer><button id='save' disabled onclick=&quot;if (document.getElementById('answer').textContent) document.body.innerHTML='Application sent'&quot;>Save</button></footer>
                                  </section>`)">Apply</button>
                              </body>
                            </html>
                            """), "3-5 years", "Not disclosed", "Pune", ""),
                    request(),
                    question -> {
                        askedQuestion.set(question);
                        return Optional.of("Pune");
                    },
                    1);

            assertEquals(ApplyStatus.SUCCESS, result.status());
            assertTrue(askedQuestion.get().contains("city"));
        }
    }

    @Test
    void customNaukriRadioRowsNeedUserAnswerWhenUnknown() {
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
                                  <section class='ssrc__drawer'>
                                    <p>Availability to attend virtual interview if scheduled on either 21 May or 22 May</p>
                                    <div class='ssrc__option'><span>Yes</span></div>
                                    <div class='ssrc__option'><span>No</span></div>
                                    <footer><button disabled>Save</button></footer>
                                  </section>`)">Apply</button>
                              </body>
                            </html>
                            """), "3-5 years", "Not disclosed", "Pune", ""),
                    request(),
                    question -> {
                        askedQuestion.set(question);
                        return Optional.empty();
                    },
                    1);

            assertEquals(ApplyStatus.QUESTION_NEEDS_ANSWER, result.status());
            assertTrue(askedQuestion.get().contains("virtual interview"));
        }
    }

    @Test
    void answersEveryChoiceQuestionBeforeSubmittingStep() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            Page page = browser.newPage();
            List<String> activities = new ArrayList<>();
            NaukriJobPage jobPage = new NaukriJobPage(page, activities::add);
            List<String> askedQuestions = new ArrayList<>();

            JobApplicationResult result = jobPage.apply(
                    new DiscoveredJob("Example Co", "Java Developer", dataUrl("""
                            <!doctype html>
                            <html>
                              <body>
                                <button onclick="document.body.insertAdjacentHTML('beforeend', `
                                  <section class='ssrc__drawer'>
                                    <div class='ssrc__question'>
                                      <p>Are you willing to relocate?</p>
                                      <div class='ssrc__option' onclick=&quot;document.getElementById('relocate').textContent='yes'; document.getElementById('save').disabled = !(document.getElementById('relocate').textContent && document.getElementById('auth').textContent)&quot;><span>Yes</span></div>
                                      <div class='ssrc__option'><span>No</span></div>
                                    </div>
                                    <div class='ssrc__question'>
                                      <p>Are you authorized to work in India?</p>
                                      <div class='ssrc__option' onclick=&quot;document.getElementById('auth').textContent='yes'; document.getElementById('save').disabled = !(document.getElementById('relocate').textContent && document.getElementById('auth').textContent)&quot;><span>Yes</span></div>
                                      <div class='ssrc__option'><span>No</span></div>
                                    </div>
                                    <span id='relocate'></span><span id='auth'></span>
                                    <footer><button id='save' disabled onclick=&quot;if (document.getElementById('relocate').textContent && document.getElementById('auth').textContent) document.body.innerHTML='Application sent'&quot;>Save</button></footer>
                                  </section>`)">Apply</button>
                              </body>
                            </html>
                            """), "3-5 years", "Not disclosed", "Pune", ""),
                    request(),
                    question -> {
                        askedQuestions.add(question);
                        return Optional.of("yes");
                    },
                    1);

            assertEquals(ApplyStatus.SUCCESS, result.status(), String.join(" | ", activities));
            assertTrue(askedQuestions.stream().anyMatch(question -> question.contains("relocate")));
            assertTrue(askedQuestions.stream().anyMatch(question -> question.contains("authorized")));
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
