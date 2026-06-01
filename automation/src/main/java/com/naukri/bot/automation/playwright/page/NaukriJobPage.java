package com.naukri.bot.automation.playwright.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.microsoft.playwright.options.SelectOption;
import com.naukri.bot.automation.AutomationActivityListener;
import com.naukri.bot.automation.QuestionAnswerProvider;
import com.naukri.bot.automation.model.ApplyStatus;
import com.naukri.bot.automation.model.AutomationRunRequest;
import com.naukri.bot.automation.model.DiscoveredJob;
import com.naukri.bot.automation.model.JobApplicationResult;
import com.naukri.bot.automation.playwright.HumanBehavior;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class NaukriJobPage {
    private static final int MAX_APPLY_STEPS = 6;
    private static final String APPLICATION_CONTAINER_SELECTORS = "[role='dialog'], [class*='modal'], [class*='Modal'], "
            + "[class*='chatbot'], [class*='Chatbot'], [id*='chatbot'], [id*='Chatbot'], "
            + "[class*='chatBot'], [id*='chatBot'], [class*='drawer'], [class*='Drawer'], "
            + "[class*='ssrc__drawer'], [id*='ssrc']";
    private static final String SCOPED_NEXT_STEP_SELECTORS = "button:has-text('Submit'), button:has-text('Send'), "
            + "button:has-text('Continue'), button:has-text('Next'), button:has-text('Save'), "
            + "[role='button']:has-text('Save'), [class*='btn']:has-text('Save'), [class*='Btn']:has-text('Save'), "
            + "[class*='button']:has-text('Save'), [class*='Button']:has-text('Save'), "
            + "button:has-text('Done'), button:has-text('OK'), button:has-text('Proceed'), "
            + "button:has-text('Submit application'), button:has-text('Save and continue')";
    private static final String GLOBAL_NEXT_STEP_SELECTORS = "button:has-text('Submit'), button:has-text('Send'), "
            + "button:has-text('Continue'), button:has-text('Next'), button:has-text('Proceed'), "
            + "button:has-text('Submit application'), button:has-text('Save and continue')";
    private static final String QUESTION_FIELD_SELECTORS = "textarea, input[type='text'], input[type='number'], "
            + "input[type='tel'], input:not([type]), [contenteditable], [role='textbox'], select";
    private static final String CHOICE_CONTROL_SELECTORS = "input[type='radio'], input[type='checkbox'], "
            + "[role='radio'], [role='checkbox'], label, "
            + "[class*='radio'], [class*='Radio'], [class*='checkbox'], [class*='Checkbox'], "
            + "[class*='option'], [class*='Option'], button, [role='button']";

    private final Page page;
    private final AutomationActivityListener activityListener;
    private final HumanBehavior human = new HumanBehavior();

    public NaukriJobPage(Page page) {
        this(page, AutomationActivityListener.NOOP);
    }

    public NaukriJobPage(Page page, AutomationActivityListener activityListener) {
        this.page = page;
        this.activityListener = activityListener;
    }

    public JobApplicationResult apply(DiscoveredJob job,
                                      AutomationRunRequest request,
                                      QuestionAnswerProvider answerProvider,
                                      int attempt) {
        activity("Opening job: " + safe(job.jobTitle()) + " at " + safe(job.companyName()) + " (attempt " + attempt + ")");
        page.navigate(job.jobUrl(), new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        activity("Waiting for job detail page to load");
        page.waitForLoadState();
        human.pause();

        if (captchaDetected()) {
            activity("Captcha detected on job page");
            return result(job, ApplyStatus.CAPTCHA_DETECTED, "Captcha detected", false, attempt);
        }
        activity("Checking application state for: " + safe(job.jobTitle()));
        String body = page.locator("body").innerText();
        if (contains(body, "already applied", "application sent") || appliedStateVisible()) {
            activity("Already applied: " + safe(job.jobTitle()) + " at " + safe(job.companyName()));
            return result(job, ApplyStatus.ALREADY_APPLIED, null, false, attempt);
        }
        if (request.dryRun()) {
            activity("Dry run enabled. Skipping actual apply for: " + safe(job.jobTitle()));
            return result(job, ApplyStatus.DRY_RUN, "Dry run enabled", false, attempt);
        }

        activity("Looking for apply button");
        Locator applyButton = findApplyButton(request);
        if (applyButton == null) {
            activity("Apply button not found for: " + safe(job.jobTitle()));
            return failedResult(job, request, "Apply button not found", attempt);
        }

        String beforeUrl = page.url();
        activity("Clicking apply button for: " + safe(job.jobTitle()));
        applyButton.click();
        page.waitForLoadState();
        human.pause();
        waitForApplyResponse();
        if (externalRedirect(beforeUrl, page.url())) {
            activity("External career site redirect detected for: " + safe(job.companyName()));
            return result(job, ApplyStatus.EXTERNAL_REDIRECT, null, true, attempt);
        }

        for (int step = 1; step <= MAX_APPLY_STEPS; step++) {
            activity("Checking application step " + step + " for: " + safe(job.jobTitle()));
            AnswerOutcome answerOutcome = answerQuestions(answerProvider);
            if (answerOutcome == AnswerOutcome.NEEDS_USER_INPUT) {
                activity("Application question still needs an answer for: " + safe(job.jobTitle()));
                return result(job, ApplyStatus.QUESTION_NEEDS_ANSWER, "A question needs a saved answer", false, attempt);
            }
            if (applicationComplete()) {
                activity("Application success detected for: " + safe(job.jobTitle()));
                return result(job, ApplyStatus.SUCCESS, null, false, attempt);
            }
            Locator submitButton = waitForNextStepButton();
            if (submitButton == null) {
                break;
            }
            String beforeStepUrl = page.url();
            activity("Submitting application step " + step + " for: " + safe(job.jobTitle()));
            submitButton.click();
            page.waitForLoadState();
            human.pause();
            waitForApplyResponse();
            if (externalRedirect(beforeStepUrl, page.url())) {
                activity("External career site redirect detected for: " + safe(job.companyName()));
                return result(job, ApplyStatus.EXTERNAL_REDIRECT, null, true, attempt);
            }
        }
        if (applicationComplete()) {
            activity("Application completed for: " + safe(job.jobTitle()) + " at " + safe(job.companyName()));
            return result(job, ApplyStatus.SUCCESS, null, false, attempt);
        }
        activity("Application completion was not confirmed for: " + safe(job.jobTitle()));
        return failedResult(job, request, "Application completion was not confirmed after clicking apply", attempt);
    }

    private Locator findApplyButton(AutomationRunRequest request) {
        String[] selectors = {
                "button:has-text('Apply')",
                "a:has-text('Apply')",
                "[role='button']:has-text('Apply')",
                "[class*='apply']:has-text('Apply')",
                "[class*='Apply']:has-text('Apply')",
                "button:has-text('I am interested')",
                "button:has-text(\"I'm interested\")",
                "a:has-text('I am interested')",
                "[role='button']:has-text('I am interested')",
                "span:has-text('Apply')"
        };
        for (String selector : selectors) {
            Locator locator = page.locator(selector);
            int count = Math.min(locator.count(), 12);
            for (int i = 0; i < count; i++) {
                Locator candidate = clickableTarget(locator.nth(i));
                if (candidate == null || !visible(candidate) || !enabled(candidate)) {
                    continue;
                }
                String text = safeText(candidate);
                if (!isApplyText(text)) {
                    continue;
                }
                if (isExternalApplyText(text) && !request.filter().externalCareerApply()) {
                    activity("Skipping external company-site apply button because external career apply is disabled");
                    continue;
                }
                if (isExternalApplyText(text) && request.filter().easyApplyOnly()) {
                    activity("Skipping external company-site apply button because easy apply only is enabled");
                    continue;
                }
                activity("Found apply control: " + compact(text));
                return candidate;
            }
        }
        return null;
    }

    private AnswerOutcome answerQuestions(QuestionAnswerProvider answerProvider) {
        AnswerOutcome fieldOutcome = answerFieldQuestions(answerProvider);
        if (fieldOutcome == AnswerOutcome.NEEDS_USER_INPUT) {
            return fieldOutcome;
        }
        return answerChoiceQuestions(answerProvider);
    }

    private AnswerOutcome answerFieldQuestions(QuestionAnswerProvider answerProvider) {
        Locator fields = questionScopedLocator(QUESTION_FIELD_SELECTORS);
        int count = Math.min(fields.count(), 8);
        Set<String> processed = new HashSet<>();
        for (int i = 0; i < count; i++) {
            Locator field = fields.nth(i);
            if (!visible(field) || !enabled(field) || isNonApplicationField(field) || hasAnswerValue(field)) {
                continue;
            }
            String question = inferQuestion(field);
            if (question.isBlank()) {
                continue;
            }
            if (!processed.add(normalizeForProcessing(question))) {
                continue;
            }
            String answer = answerForQuestion(answerProvider, question);
            if (answer == null || answer.isBlank()) {
                activity("No saved answer found for question: " + question);
                return AnswerOutcome.NEEDS_USER_INPUT;
            }
            activity("Found saved answer for question: " + question);
            fillField(field, answer);
            if (!hasAnswerValue(field)) {
                forceFillField(field, answer);
            }
            activity("Filled saved answer for question: " + question);
        }
        return AnswerOutcome.COMPLETE;
    }

    private AnswerOutcome answerChoiceQuestions(QuestionAnswerProvider answerProvider) {
        Locator container = firstApplicationContainerWith(CHOICE_CONTROL_SELECTORS);
        if (container == null) {
            container = page.locator("body");
        }
        Locator choices = container.locator(CHOICE_CONTROL_SELECTORS);
        int count = Math.min(choices.count(), 32);
        Set<String> processed = new HashSet<>();
        for (int i = 0; i < count; i++) {
            Locator choice = choices.nth(i);
            if (!isChoiceOptionCandidate(choice) || !enabled(choice) || isAlreadySelected(choice)) {
                continue;
            }
            String optionText = choiceText(choice);
            if (isNextStepText(optionText) || optionText.isBlank()) {
                continue;
            }
            Locator group = nearestQuestionGroup(choice);
            String question = questionText(group);
            if (question.isBlank()) {
                question = inferQuestion(choice);
            }
            if (question.isBlank() || question.equalsIgnoreCase(optionText)) {
                question = questionText(group);
            }
            if (question.isBlank()) {
                continue;
            }
            if (!processed.add(normalizeForProcessing(question))) {
                continue;
            }
            String answer = answerForQuestion(answerProvider, question);
            if (answer == null || answer.isBlank()) {
                activity("No saved answer found for question: " + question);
                return AnswerOutcome.NEEDS_USER_INPUT;
            }
            if (!clickMatchingChoice(group, answer) && !clickMatchingChoice(container, answer)) {
                activity("No visible answer option matched '" + compact(answer) + "' for question: " + question);
                return AnswerOutcome.NEEDS_USER_INPUT;
            }
            return AnswerOutcome.COMPLETE;
        }
        return AnswerOutcome.COMPLETE;
    }

    private Locator firstVisible(String selector) {
        Locator locator = page.locator(selector);
        int count = Math.min(locator.count(), 10);
        for (int i = 0; i < count; i++) {
            Locator candidate = locator.nth(i);
            if (visible(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Locator clickableTarget(Locator locator) {
        try {
            Locator ancestor = locator.locator("xpath=ancestor-or-self::*[self::button or self::a or @role='button' or contains(@class,'apply') or contains(@class,'Apply')][1]");
            if (ancestor.count() > 0) {
                return ancestor.first();
            }
        } catch (Exception ignored) {
        }
        return locator;
    }

    private boolean applicationComplete() {
        String body = page.locator("body").innerText();
        return contains(body,
                "application sent",
                "successfully applied",
                "applied successfully",
                "application submitted",
                "you have successfully applied",
                "your application has been sent")
                || appliedStateVisible();
    }

    private void waitForApplyResponse() {
        for (int i = 0; i < 12; i++) {
            if (captchaDetected() || applicationComplete() || hasQuestionFields() || findNextStepButton() != null) {
                return;
            }
            page.waitForTimeout(750);
        }
    }

    private Locator findNextStepButton() {
        Locator scoped = firstVisibleInContainers(APPLICATION_CONTAINER_SELECTORS, SCOPED_NEXT_STEP_SELECTORS);
        if (scoped != null) {
            return scoped;
        }
        return firstVisible(GLOBAL_NEXT_STEP_SELECTORS);
    }

    private Locator waitForNextStepButton() {
        for (int i = 0; i < 12; i++) {
            Locator button = findNextStepButton();
            if (button != null) {
                return button;
            }
            page.waitForTimeout(500);
        }
        return null;
    }

    private Locator questionScopedLocator(String selector) {
        Locator container = firstApplicationContainerWith(selector);
        return container == null ? page.locator(selector) : container.locator(selector);
    }

    private Locator firstApplicationContainerWith(String childSelector) {
        try {
            Locator containers = page.locator(APPLICATION_CONTAINER_SELECTORS);
            int containerCount = Math.min(containers.count(), 12);
            for (int i = 0; i < containerCount; i++) {
                Locator container = containers.nth(i);
                if (!visible(container) || container.locator(childSelector).count() == 0) {
                    continue;
                }
                return container;
            }
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    private Locator firstVisibleInContainers(String containerSelector, String childSelector) {
        try {
            Locator containers = page.locator(containerSelector);
            int containerCount = Math.min(containers.count(), 12);
            for (int i = 0; i < containerCount; i++) {
                Locator container = containers.nth(i);
                if (!visible(container)) {
                    continue;
                }
                Locator buttons = container.locator(childSelector);
                int buttonCount = Math.min(buttons.count(), 10);
                for (int j = 0; j < buttonCount; j++) {
                    Locator button = buttons.nth(j);
                    if (visible(button) && enabled(button) && isNextStepText(safeText(button))) {
                        return clickableTarget(button);
                    }
                }
            }
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean hasQuestionFields() {
        try {
            return firstApplicationContainerWith(QUESTION_FIELD_SELECTORS) != null
                    || firstApplicationContainerWith(CHOICE_CONTROL_SELECTORS) != null;
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean appliedStateVisible() {
        try {
            Locator locator = page.locator("button:has-text('Applied'), [role='button']:has-text('Applied')");
            int count = Math.min(locator.count(), 10);
            for (int i = 0; i < count; i++) {
                Locator candidate = locator.nth(i);
                if (!visible(candidate)) {
                    continue;
                }
                String normalized = safeText(candidate).toLowerCase(Locale.ROOT).trim();
                if (normalized.equals("applied") || normalized.contains("already applied")) {
                    return true;
                }
            }
            return false;
        } catch (Exception exception) {
            return false;
        }
    }

    private String inferQuestion(Locator field) {
        try {
            Object question = field.evaluate("""
                    el => {
                      const clean = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const type = (el.getAttribute('type') || '').toLowerCase();
                      const containerText = () => {
                        const container = el.closest('[class*="question"], [class*="Question"], [class*="ques"], [class*="Ques"], fieldset, li, div');
                        if (!container) return '';
                        const clone = container.cloneNode(true);
                        clone.querySelectorAll('input, textarea, select, option, button, svg, path').forEach(node => node.remove());
                        return clean(clone.innerText || clone.textContent);
                      };
                      if (type === 'radio' || type === 'checkbox' || el.tagName.toLowerCase() === 'button') {
                        const grouped = containerText();
                        if (grouped) return grouped;
                      }
                      const aria = clean(el.getAttribute('aria-label'));
                      if (aria) return aria;
                      const labelledBy = clean(el.getAttribute('aria-labelledby'));
                      if (labelledBy) {
                        const labelText = labelledBy.split(/\\s+/)
                          .map(id => document.getElementById(id))
                          .filter(Boolean)
                          .map(node => clean(node.innerText || node.textContent))
                          .filter(Boolean)
                          .join(' ');
                        if (labelText) return labelText;
                      }
                      const id = el.getAttribute('id');
                      if (id) {
                        const label = document.querySelector(`label[for="${CSS.escape(id)}"]`);
                        const labelText = clean(label && (label.innerText || label.textContent));
                        if (labelText) return labelText;
                      }
                      const placeholder = clean(el.getAttribute('placeholder'));
                      if (placeholder) return placeholder;
                      return containerText();
                    }
                    """);
            return cleanQuestion(question == null ? "" : question.toString());
        } catch (Exception exception) {
            return "";
        }
    }

    private String questionText(Locator group) {
        try {
            Object question = group.evaluate("""
                    el => {
                      const clone = el.cloneNode(true);
                      clone.querySelectorAll('[role="radio"], [role="checkbox"], label:has(input[type="radio"]), label:has(input[type="checkbox"]), [class*="radio"], [class*="Radio"], [class*="checkbox"], [class*="Checkbox"], [class*="option"], [class*="Option"]').forEach(node => {
                        if (node !== clone) node.remove();
                      });
                      clone.querySelectorAll('input, textarea, select, option').forEach(node => {
                        const optionRow = node.closest('label, li, [class*="option"], [class*="Option"], [class*="radio"], [class*="Radio"], [class*="checkbox"], [class*="Checkbox"]');
                        if (optionRow && optionRow !== clone) {
                          optionRow.remove();
                        } else {
                          node.remove();
                        }
                      });
                      clone.querySelectorAll('button, [role="button"], svg, path').forEach(node => node.remove());
                      clone.querySelectorAll('label').forEach(label => {
                        if (label.getAttribute('for') || label.querySelector('input, textarea, select')) {
                          label.remove();
                        }
                      });
                      return (clone.innerText || clone.textContent || '').replace(/\\s+/g, ' ').trim();
                    }
                    """);
            return cleanQuestion(question == null ? "" : question.toString());
        } catch (Exception exception) {
            return "";
        }
    }

    private Locator nearestQuestionGroup(Locator control) {
        try {
            Locator drawer = control.locator(
                    "xpath=ancestor::*[contains(@class,'drawer') or contains(@class,'Drawer') "
                            + "or contains(@class,'ssrc__drawer') or contains(@id,'ssrc') "
                            + "or contains(@class,'chatbot') or contains(@class,'Chatbot') "
                            + "or contains(@class,'chatBot') or @role='dialog']"
                            + "[count(.//input[@type='radio' or @type='checkbox']) + count(.//button) "
                            + "+ count(.//*[@role='button' or @role='radio' or @role='checkbox']) "
                            + "+ count(.//*[contains(@class,'radio') or contains(@class,'Radio') or contains(@class,'checkbox') or contains(@class,'Checkbox') or contains(@class,'option') or contains(@class,'Option')]) > 1][1]");
            if (drawer.count() > 0) {
                return drawer.first();
            }
        } catch (Exception ignored) {
        }
        try {
            Locator groupedChoices = control.locator(
                    "xpath=ancestor::*[self::fieldset or self::form or self::section or self::article or self::aside or self::li or self::div]"
                            + "[not(contains(@class,'radio') or contains(@class,'Radio') or contains(@class,'checkbox') or contains(@class,'Checkbox') or contains(@class,'option') or contains(@class,'Option'))]"
                            + "[count(.//input[@type='radio' or @type='checkbox']) + count(.//button) "
                            + "+ count(.//*[@role='button' or @role='radio' or @role='checkbox']) "
                            + "+ count(.//*[contains(@class,'radio') or contains(@class,'Radio') or contains(@class,'checkbox') or contains(@class,'Checkbox') or contains(@class,'option') or contains(@class,'Option')]) > 1][1]");
            if (groupedChoices.count() > 0) {
                return groupedChoices.first();
            }
        } catch (Exception ignored) {
        }
        try {
            Locator preferred = control.locator("xpath=ancestor::*[self::fieldset or self::form or self::section or self::article or self::aside or self::li or self::div][contains(@class,'question') or contains(@class,'Question') or contains(@class,'ques') or contains(@class,'Ques')][1]");
            if (preferred.count() > 0) {
                return preferred.first();
            }
        } catch (Exception ignored) {
        }
        try {
            Locator generic = control.locator("xpath=ancestor::*[self::fieldset or self::form or self::section or self::article or self::aside or self::li or self::div][1]");
            if (generic.count() > 0) {
                return generic.first();
            }
        } catch (Exception ignored) {
        }
        return control;
    }

    private void fillField(Locator field, String answer) {
        try {
            String tagName = field.evaluate("el => el.tagName.toLowerCase()").toString();
            if ("select".equals(tagName)) {
                try {
                    field.selectOption(answer);
                } catch (Exception valueMiss) {
                    field.selectOption(new SelectOption().setLabel(answer));
                }
                return;
            }
            human.type(field, answer);
        } catch (Exception typingFailure) {
            field.click();
            field.pressSequentially(answer);
        }
    }

    private void forceFillField(Locator field, String answer) {
        try {
            field.evaluate("""
                    (el, value) => {
                      const tag = el.tagName.toLowerCase();
                      el.scrollIntoView({ block: 'center', inline: 'nearest' });
                      if (tag === 'select') {
                        const option = Array.from(el.options).find(candidate =>
                          candidate.value === value || candidate.textContent.trim().toLowerCase() === String(value).trim().toLowerCase());
                        if (option) el.value = option.value;
                      } else if (el.isContentEditable) {
                        el.textContent = value;
                      } else {
                        el.value = value;
                      }
                      el.dispatchEvent(new Event('input', { bubbles: true }));
                      el.dispatchEvent(new Event('change', { bubbles: true }));
                      el.dispatchEvent(new Event('blur', { bubbles: true }));
                    }
                    """, answer);
        } catch (Exception ignored) {
        }
    }

    private boolean clickMatchingChoice(Locator group, String answer) {
        Locator choices = group.locator(CHOICE_CONTROL_SELECTORS);
        int count = Math.min(choices.count(), 32);
        for (int i = 0; i < count; i++) {
            Locator choice = choices.nth(i);
            if (!isChoiceOptionCandidate(choice) || !enabled(choice) || isAlreadySelected(choice)) {
                continue;
            }
            String option = choiceText(choice);
            if (isNextStepText(option) || !answerMatchesOption(answer, option)) {
                continue;
            }
            activity("Selecting answer option: " + compact(option));
            selectChoice(choice);
            human.pause();
            return true;
        }
        return false;
    }

    private void selectChoice(Locator choice) {
        try {
            Object selected = choice.evaluate("""
                    el => {
                      const input = el.matches("input[type='radio'], input[type='checkbox']")
                        ? el
                        : el.querySelector("input[type='radio'], input[type='checkbox']");
                      if (!input) return false;
                      input.scrollIntoView({ block: 'center', inline: 'nearest' });
                      const label = input.id
                        ? document.querySelector(`label[for="${CSS.escape(input.id)}"]`)
                        : null;
                      const optionRow = input.closest('label, [role="radio"], [role="checkbox"], [class*="radio"], [class*="Radio"], [class*="checkbox"], [class*="Checkbox"], [class*="option"], [class*="Option"], li, div');
                      const clickable = label || input.closest('label') || input;
                      clickable.click();
                      if (!input.checked && optionRow && optionRow !== input) {
                        optionRow.click();
                      }
                      if (!input.checked) {
                        input.checked = true;
                      }
                      input.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
                      input.dispatchEvent(new Event('input', { bubbles: true }));
                      input.dispatchEvent(new Event('change', { bubbles: true }));
                      return true;
                    }
                    """);
            if (Boolean.TRUE.equals(selected)) {
                human.pause();
                return;
            }
        } catch (Exception ignored) {
        }
        try {
            choice.scrollIntoViewIfNeeded();
            choice.click();
            return;
        } catch (Exception clickFailure) {
            try {
                choice.evaluate("""
                        el => {
                          const input = el.matches("input[type='radio'], input[type='checkbox']")
                            ? el
                            : el.querySelector("input[type='radio'], input[type='checkbox']");
                          const target = input || el;
                          target.scrollIntoView({ block: 'center', inline: 'nearest' });
                          if (input) {
                            const label = input.id
                              ? document.querySelector(`label[for="${CSS.escape(input.id)}"]`)
                              : null;
                            const optionRow = input.closest('label, [role="radio"], [role="checkbox"], [class*="radio"], [class*="Radio"], [class*="checkbox"], [class*="Checkbox"], [class*="option"], [class*="Option"], li, div');
                            const clickable = label || input.closest('label') || optionRow || input;
                            clickable.click();
                            input.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
                            if (!input.checked) {
                              input.checked = true;
                            }
                            input.dispatchEvent(new Event('input', { bubbles: true }));
                            input.dispatchEvent(new Event('change', { bubbles: true }));
                            return true;
                          }
                          target.click();
                          return true;
                        }
                        """);
            } catch (Exception domClickFailure) {
                choice.click(new Locator.ClickOptions().setForce(true));
            }
        }
    }

    private String choiceText(Locator choice) {
        try {
            Object text = choice.evaluate("""
                    el => {
                      const clean = value => (value || '').replace(/\\s+/g, ' ').trim();
                      const tag = el.tagName.toLowerCase();
                      const role = el.getAttribute('role');
                      const className = (el.className || '').toString().toLowerCase();
                      if (tag === 'button' || tag === 'label' || role === 'button' || role === 'radio' || role === 'checkbox'
                          || className.includes('radio') || className.includes('checkbox') || className.includes('option')) {
                        return clean(el.innerText || el.textContent || el.getAttribute('aria-label'));
                      }
                      const id = el.getAttribute('id');
                      if (id) {
                        const label = document.querySelector(`label[for="${CSS.escape(id)}"]`);
                        const labelText = clean(label && (label.innerText || label.textContent));
                        if (labelText) return labelText;
                      }
                      const closestLabel = el.closest('label');
                      const closestLabelText = clean(closestLabel && (closestLabel.innerText || closestLabel.textContent));
                      if (closestLabelText) return closestLabelText;
                      const aria = clean(el.getAttribute('aria-label'));
                      if (aria) return aria;
                      const parent = el.parentElement;
                      const parentText = clean(parent && (parent.innerText || parent.textContent));
                      return parentText || clean(el.value || el.getAttribute('value'));
                    }
                    """);
            return compact(text == null ? "" : text.toString());
        } catch (Exception exception) {
            return "";
        }
    }

    private boolean hasAnswerValue(Locator field) {
        try {
            Object value = field.evaluate("""
                    el => {
                      const tag = el.tagName.toLowerCase();
                      if (tag === 'select') return el.value || '';
                      if (el.isContentEditable) return el.innerText || el.textContent || '';
                      return el.value || '';
                    }
                    """);
            return value != null && !value.toString().isBlank();
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isChoiceOptionCandidate(Locator choice) {
        try {
            Object candidate = choice.evaluate("""
                    el => {
                      const tag = el.tagName.toLowerCase();
                      const type = (el.getAttribute('type') || '').toLowerCase();
                      const role = el.getAttribute('role');
                      const className = (el.className || '').toString().toLowerCase();
                      const nestedChoiceCount = el.querySelectorAll("input[type='radio'], input[type='checkbox'], [role='radio'], [role='checkbox']").length;
                      if (tag === 'input') return type === 'radio' || type === 'checkbox';
                      if (role === 'radio' || role === 'checkbox') return nestedChoiceCount <= 1;
                      if (tag === 'label') return nestedChoiceCount === 1;
                      if (className.includes('radio') || className.includes('checkbox') || className.includes('option')) return nestedChoiceCount <= 1;
                      return tag === 'button' || role === 'button';
                    }
                    """);
            return Boolean.TRUE.equals(candidate);
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isAlreadySelected(Locator choice) {
        try {
            Object selected = choice.evaluate("""
                    el => Boolean(el.checked || el.getAttribute('aria-checked') === 'true' || el.className?.toString().toLowerCase().includes('selected'))
                    """);
            return Boolean.TRUE.equals(selected);
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isNonApplicationField(Locator field) {
        try {
            Object descriptor = field.evaluate("""
                    el => [el.getAttribute('placeholder'), el.getAttribute('aria-label'), el.getAttribute('name'), el.getAttribute('type')]
                      .filter(Boolean)
                      .join(' ')
                      .toLowerCase()
                    """);
            String text = descriptor == null ? "" : descriptor.toString();
            return text.contains("search jobs") || text.equals("search") || text.contains("search here");
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean answerMatchesOption(String answer, String option) {
        String normalizedAnswer = normalizeForProcessing(answer);
        String normalizedOption = normalizeForProcessing(option);
        if (normalizedAnswer.isBlank() || normalizedOption.isBlank()) {
            return false;
        }
        if (normalizedOption.equals(normalizedAnswer)
                || normalizedOption.contains(normalizedAnswer)
                || normalizedAnswer.contains(normalizedOption)) {
            return true;
        }
        return (normalizedAnswer.startsWith("y") && normalizedOption.equals("yes"))
                || (normalizedAnswer.startsWith("n") && normalizedOption.equals("no"));
    }

    private boolean isNextStepText(String text) {
        String normalized = normalizeForProcessing(text);
        return normalized.equals("submit")
                || normalized.equals("send")
                || normalized.equals("continue")
                || normalized.equals("next")
                || normalized.equals("save")
                || normalized.equals("done")
                || normalized.equals("ok")
                || normalized.equals("proceed")
                || normalized.equals("apply")
                || normalized.equals("applied")
                || normalized.contains("submit application")
                || normalized.contains("save and continue");
    }

    private boolean captchaDetected() {
        String body = page.locator("body").innerText().toLowerCase(Locale.ROOT);
        return body.contains("captcha") || body.contains("verify you are human") || page.url().toLowerCase(Locale.ROOT).contains("captcha");
    }

    private boolean externalRedirect(String beforeUrl, String afterUrl) {
        return afterUrl != null
                && !afterUrl.isBlank()
                && !afterUrl.contains("naukri.com")
                && !afterUrl.equals(beforeUrl);
    }

    private boolean contains(String source, String... values) {
        String normalized = source == null ? "" : source.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (normalized.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private boolean isApplyText(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (normalized.isBlank()
                || normalized.contains("already applied")
                || normalized.equals("applied")
                || normalized.contains("application sent")) {
            return false;
        }
        return normalized.contains("apply") || normalized.contains("interested");
    }

    private boolean isExternalApplyText(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return normalized.contains("company site")
                || normalized.contains("company website")
                || normalized.contains("external")
                || normalized.contains("career site")
                || normalized.contains("employer site");
    }

    private boolean enabled(Locator locator) {
        try {
            if (!locator.isEnabled()) {
                return false;
            }
            Object disabled = locator.evaluate("""
                    el => {
                      const className = (el.className || '').toString().toLowerCase();
                      const ariaDisabled = (el.getAttribute('aria-disabled') || '').toLowerCase() === 'true';
                      const disabledClass = className.includes('disabled');
                      const disabledAncestor = el.closest('[aria-disabled="true"], .disabled, [class*="disabled"], [class*="Disabled"]');
                      return Boolean(el.disabled || ariaDisabled || disabledClass || disabledAncestor);
                    }
                    """);
            return !Boolean.TRUE.equals(disabled);
        } catch (Exception exception) {
            return false;
        }
    }

    private String safeText(Locator locator) {
        try {
            return locator.innerText().trim();
        } catch (Exception exception) {
            return "";
        }
    }

    private String compact(String value) {
        String text = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return text.length() <= 120 ? text : text.substring(0, 117) + "...";
    }

    private String cleanQuestion(String value) {
        return compact(value)
                .replaceAll("(?i)\\b(submit|continue|next|save and continue|save|done|ok|proceed)\\b", "")
                .replaceAll("(?i)\\b(yes\\s+no|no\\s+yes|yesno|noyes)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String answerForQuestion(QuestionAnswerProvider answerProvider, String question) {
        for (String candidate : questionVariants(question)) {
            String answer = answerProvider.answerFor(candidate).orElse(null);
            if (answer != null && !answer.isBlank()) {
                if (!candidate.equals(question)) {
                    activity("Matched saved answer using simplified question: " + candidate);
                }
                return answer;
            }
        }
        return null;
    }

    private java.util.List<String> questionVariants(String question) {
        String compactQuestion = compact(question);
        String withoutOptions = cleanQuestion(compactQuestion)
                .replaceAll("(?i)\\b(yes|no|true|false)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
        String beforeQuestionMark = compactQuestion.contains("?")
                ? compactQuestion.substring(0, compactQuestion.indexOf('?') + 1).trim()
                : compactQuestion;
        java.util.LinkedHashSet<String> variants = new java.util.LinkedHashSet<>();
        variants.add(compactQuestion);
        variants.add(withoutOptions);
        variants.add(beforeQuestionMark);
        return variants.stream().filter(value -> value != null && !value.isBlank()).toList();
    }

    private String normalizeForProcessing(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean visible(Locator locator) {
        try {
            return locator.isVisible();
        } catch (Exception exception) {
            return false;
        }
    }

    private JobApplicationResult result(DiscoveredJob job, ApplyStatus status, String reason, boolean redirect, int attempt) {
        return result(job, status, reason, redirect, null, attempt);
    }

    private JobApplicationResult failedResult(DiscoveredJob job, AutomationRunRequest request, String reason, int attempt) {
        String screenshotPath = screenshot(request.storageDirectory(), job.companyName(), attempt);
        if (screenshotPath != null) {
            activity("Failure screenshot saved: " + screenshotPath);
        }
        return result(job, ApplyStatus.FAILED, reason, false, screenshotPath, attempt);
    }

    private JobApplicationResult result(DiscoveredJob job, ApplyStatus status, String reason, boolean redirect,
                                        String screenshotPath, int attempt) {
        return new JobApplicationResult(job.companyName(), job.jobTitle(), Instant.now(), status, page.url(),
                job.experience(), job.salary(), job.location(), redirect, reason, screenshotPath, 0.0, attempt);
    }

    private String screenshot(Path storageDirectory, String companyName, int attempt) {
        try {
            Path directory = storageDirectory.resolve("screenshots");
            Files.createDirectories(directory);
            String cleanName = companyName == null ? "job" : companyName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = directory.resolve(cleanName + "_apply_" + attempt + "_" + System.currentTimeMillis() + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(target).setFullPage(true));
            return target.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

    private void activity(String message) {
        activityListener.onActivity(message);
    }

    private enum AnswerOutcome {
        COMPLETE,
        NEEDS_USER_INPUT
    }
}
