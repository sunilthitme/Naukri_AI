package com.aijobapplyassistant.service.bot;

import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.entity.enums.ApplicationStatus;
import com.aijobapplyassistant.entity.enums.PortalType;
import com.aijobapplyassistant.service.PortalCredentialService;
import com.aijobapplyassistant.service.QuestionQueueService;
import com.aijobapplyassistant.service.SystemLogService;
import com.aijobapplyassistant.service.ai.OllamaAiService;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class GenericCareerPortalAdapter implements CareerPortalAdapter {

    protected final OllamaAiService ollamaAiService;
    protected final QuestionQueueService questionQueueService;
    protected final SystemLogService systemLogService;
    protected final PortalCredentialService portalCredentialService;

    public GenericCareerPortalAdapter(OllamaAiService ollamaAiService, QuestionQueueService questionQueueService,
            SystemLogService systemLogService, PortalCredentialService portalCredentialService) {
        this.ollamaAiService = ollamaAiService;
        this.questionQueueService = questionQueueService;
        this.systemLogService = systemLogService;
        this.portalCredentialService = portalCredentialService;
    }

    @Override
    public boolean supports(String url) {
        return true;
    }

    @Override
    public PortalSubmissionResult apply(Page page, User user, JobListing listing, String resumePath) {
        if (isCaptchaVisible(page)) {
            return new PortalSubmissionResult(ApplicationStatus.CAPTCHA_WAITING, "Captcha detected on portal", false);
        }

        boolean credentialCreated = ensurePortalAccess(page, user, listing);
        uploadResume(page, resumePath);
        int labels = page.locator("label").count();
        for (int index = 0; index < labels; index++) {
            Locator label = page.locator("label").nth(index);
            String labelText = safe(label.innerText());
            if (labelText.isBlank()) {
                continue;
            }

            Locator field = resolveField(page, label);
            if (field == null || field.count() == 0) {
                continue;
            }

            String value = ollamaAiService.resolveFieldValue(labelText, user, listing.description());
            if (value.isBlank() && isRequiredField(field, labelText)) {
                questionQueueService.createQuestion(user, null, labelText, labelText, page.url());
                return new PortalSubmissionResult(ApplicationStatus.QUESTION_PENDING, "Manual answer required for " + labelText, credentialCreated);
            }

            fillField(page, field.first(), labelText, value);
        }

        String coverLetter = ollamaAiService.generateCoverLetter(user, listing.description());
        fillFirstVisible(page, new String[]{"textarea", "textarea[name*='cover']", "textarea[placeholder*='cover']"}, coverLetter);

        if (clickFirstVisible(page, new String[]{
                "button:has-text('Submit')",
                "button:has-text('Apply')",
                "button:has-text('Continue')",
                "input[type='submit']"
        })) {
            page.waitForTimeout(1500);
        }

        String content = safe(page.content()).toLowerCase();
        if (content.contains("thank you") || content.contains("application submitted") || content.contains("already applied")) {
            return new PortalSubmissionResult(ApplicationStatus.APPLIED, "Application submitted", credentialCreated);
        }
        return new PortalSubmissionResult(ApplicationStatus.APPLIED, "Submit action completed; verify portal response if needed", credentialCreated);
    }

    protected boolean ensurePortalAccess(Page page, User user, JobListing listing) {
        String content = safe(page.content()).toLowerCase(Locale.ROOT);
        String portalUrl = page.url();
        boolean loginScreen = content.contains("sign in") || content.contains("login");
        boolean registerScreen = content.contains("create account") || content.contains("register") || content.contains("sign up");
        if (!loginScreen && !registerScreen) {
            return false;
        }

        var existing = portalCredentialService.findByPortal(user.getId(), portalUrl);
        if (existing.isPresent()) {
            fillFirstVisible(page, new String[]{"input[type='email']", "input[name*='email']", "input[name*='username']"}, existing.get().getUsername());
            fillFirstVisible(page, new String[]{"input[type='password']"}, portalCredentialService.decryptPassword(existing.get()));
            clickFirstVisible(page, new String[]{"button:has-text('Sign in')", "button:has-text('Login')", "button[type='submit']"});
            page.waitForTimeout(1500);
            return false;
        }

        if (registerScreen) {
            String generatedPassword = "Ajob@" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            fillFirstVisible(page, new String[]{"input[name*='name']", "input[placeholder*='name']"}, user.getFullName());
            fillFirstVisible(page, new String[]{"input[type='email']", "input[name*='email']", "input[name*='username']"}, user.getEmail());
            fillFirstVisible(page, new String[]{"input[type='password']"}, generatedPassword);
            fillFirstVisible(page, new String[]{"input[name*='confirm']", "input[name*='passwordConfirmation']"}, generatedPassword);
            clickFirstVisible(page, new String[]{"button:has-text('Create')", "button:has-text('Register')", "button:has-text('Sign up')", "button[type='submit']"});
            page.waitForTimeout(1500);
            portalCredentialService.save(user, listing.company(), portalUrl, user.getEmail(), generatedPassword, detectPortalType(portalUrl));
            return true;
        }
        return false;
    }

    protected void uploadResume(Page page, String resumePath) {
        Locator fileInput = firstVisible(page, "input[type='file']");
        if (fileInput != null) {
            fileInput.setInputFiles(Path.of(resumePath));
        }
    }

    protected Locator resolveField(Page page, Locator label) {
        String forAttribute = label.getAttribute("for");
        if (forAttribute != null && !forAttribute.isBlank()) {
            Locator byId = page.locator("#" + forAttribute);
            if (byId.count() > 0) {
                return byId;
            }
        }
        Locator nested = label.locator("input, textarea, select");
        if (nested.count() > 0) {
            return nested;
        }
        return null;
    }

    protected void fillField(Page page, Locator field, String labelText, String value) {
        try {
            String tagName = safe((String) field.evaluate("element => element.tagName.toLowerCase()"));
            String inputType = safe(field.getAttribute("type")).toLowerCase();
            if ("select".equals(tagName)) {
                field.selectOption(value);
                return;
            }
            if ("radio".equals(inputType) || "checkbox".equals(inputType)) {
                Locator option = page.locator("label:has-text('" + escapeText(value) + "')");
                if (option.count() > 0) {
                    option.first().click();
                }
                return;
            }
            if (!"file".equals(inputType) && value != null && !value.isBlank()) {
                field.fill(value);
            }
        } catch (Exception exception) {
            systemLogService.warn(null, "GenericCareerPortalAdapter", "Field fill issue for " + labelText, exception.getMessage());
        }
    }

    protected boolean clickFirstVisible(Page page, String[] selectors) {
        Locator locator = firstVisible(page, selectors);
        if (locator != null) {
            locator.click();
            return true;
        }
        return false;
    }

    protected void fillFirstVisible(Page page, String[] selectors, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Locator locator = firstVisible(page, selectors);
        if (locator != null) {
            locator.fill(value);
        }
    }

    protected Locator firstVisible(Page page, String... selectors) {
        for (String selector : selectors) {
            Locator locator = page.locator(selector);
            if (locator.count() > 0) {
                return locator.first();
            }
        }
        return null;
    }

    protected boolean isCaptchaVisible(Page page) {
        String content = safe(page.content()).toLowerCase();
        return content.contains("captcha") || content.contains("i am not a robot");
    }

    protected boolean isRequiredField(Locator field, String labelText) {
        return labelText.contains("*") || "true".equalsIgnoreCase(field.getAttribute("required"));
    }

    protected String safe(String value) {
        return value == null ? "" : value;
    }

    protected String escapeText(String text) {
        return safe(text).replace("'", "\\'");
    }

    private PortalType detectPortalType(String url) {
        String normalized = safe(url).toLowerCase(Locale.ROOT);
        if (normalized.contains("workday")) {
            return PortalType.WORKDAY;
        }
        if (normalized.contains("greenhouse")) {
            return PortalType.GREENHOUSE;
        }
        if (normalized.contains("lever")) {
            return PortalType.LEVER;
        }
        if (normalized.contains("taleo")) {
            return PortalType.TALEO;
        }
        return PortalType.CUSTOM;
    }
}
