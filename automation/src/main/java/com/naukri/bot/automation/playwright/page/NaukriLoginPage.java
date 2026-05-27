package com.naukri.bot.automation.playwright.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.naukri.bot.automation.AutomationActivityListener;
import com.naukri.bot.automation.model.LoginStatus;
import com.naukri.bot.automation.model.LoginTestResult;
import com.naukri.bot.automation.playwright.HumanBehavior;

import java.util.List;

public class NaukriLoginPage {
    private static final List<String> LOGIN_URLS = List.of(
            "https://www.naukri.com/nlogin/login",
            "https://www.naukri.com/mnj/login",
            "https://login.naukri.com/nLogin/Login.php",
            "https://www.naukri.com/");
    private static final String[] USERNAME_SELECTORS = {
            "#usernameField",
            "#emailTxt",
            "input[name='email']",
            "input[name='username']",
            "input[name='USERNAME']",
            "input[name='userName']",
            "input[autocomplete='username']",
            "input[type='email']",
            "input[placeholder*='Email']",
            "input[placeholder*='email']",
            "input[placeholder*='Username']",
            "input[placeholder*='username']",
            "input[placeholder*='Email ID']",
            "input[placeholder*='Email id']",
            "input[placeholder*='User ID']",
            "input[type='text']"
    };
    private static final String[] PASSWORD_SELECTORS = {
            "#passwordField",
            "#pwd1",
            "input[name='password']",
            "input[name='PASSWORD']",
            "input[autocomplete='current-password']",
            "input[type='password']",
            "input[placeholder*='Password']",
            "input[placeholder*='password']"
    };
    private static final String[] LOGIN_BUTTON_SELECTORS = {
            "button[type='submit']",
            "input[type='submit']",
            "button:has-text('Login')",
            "button:has-text('Log in')",
            "button:has-text('LOGIN')",
            "input[value='Login']",
            "input[value='Log in']",
            "#loginButton",
            ".loginButton",
            ".blue-btn",
            ".waves-effect"
    };
    private static final String[] LOGIN_ENTRY_SELECTORS = {
            ".nI-gNb-lg-rg__login",
            "a:has-text('Login')",
            "button:has-text('Login')",
            "text=Login"
    };

    private final Page page;
    private final AutomationActivityListener activityListener;
    private final HumanBehavior human = new HumanBehavior();
    private final NaukriLoginStatusClassifier classifier = new NaukriLoginStatusClassifier();

    public NaukriLoginPage(Page page) {
        this(page, AutomationActivityListener.NOOP);
    }

    public NaukriLoginPage(Page page, AutomationActivityListener activityListener) {
        this.page = page;
        this.activityListener = activityListener;
    }

    public LoginTestResult login(String email, String password) {
        activity("Preparing Naukri login");
        LoginForm loginForm = openLoginForm();
        if (loginForm == null) {
            LoginTestResult blocked = classifyCurrentPage();
            if (blocked.status() == LoginStatus.CAPTCHA_DETECTED || blocked.status() == LoginStatus.OTP_REQUIRED) {
                return blocked;
            }
            return new LoginTestResult(false, LoginStatus.LOGIN_FORM_NOT_FOUND,
                    "Naukri login form was not found on supported login pages. Current URL: " + page.url(), page.url(), null);
        }
        activity("Entering Naukri email");
        human.type(loginForm.username(), email);
        human.pause();
        activity("Entering Naukri password");
        human.type(loginForm.password(), password);
        human.pause();
        Locator loginButton = waitForVisible(LOGIN_BUTTON_SELECTORS, 5_000);
        if (loginButton == null) {
            activity("Submitting Naukri login form with Enter key");
            loginForm.password().press("Enter");
        } else {
            activity("Submitting Naukri login form");
            loginButton.click();
        }
        for (int i = 0; i < 20; i++) {
            waitQuietly(750);
            LoginTestResult result = classifyCurrentPage();
            if (result.success()
                    || result.status() == LoginStatus.INVALID_CREDENTIALS
                    || result.status() == LoginStatus.CAPTCHA_DETECTED
                    || result.status() == LoginStatus.OTP_REQUIRED) {
                activity(result.success() ? "Naukri login succeeded" : result.message());
                return result;
            }
        }
        LoginTestResult result = classifyCurrentPage();
        activity(result.message());
        return result;
    }

    public boolean captchaDetected() {
        return classifyCurrentPage().status() == LoginStatus.CAPTCHA_DETECTED;
    }

    public LoginTestResult waitForManualLogin(int timeoutSeconds) {
        int safeTimeoutSeconds = Math.max(60, timeoutSeconds);
        activity("Manual Naukri login required. Complete login/captcha in the visible browser window.");
        long deadline = System.currentTimeMillis() + (safeTimeoutSeconds * 1000L);
        long nextReminderAt = 0;
        while (System.currentTimeMillis() < deadline) {
            waitQuietly(2_000);
            LoginTestResult result = classifyCurrentPage();
            if (result.success()) {
                activity("Manual Naukri login completed. Continuing automation.");
                return result;
            }
            long now = System.currentTimeMillis();
            if (now >= nextReminderAt) {
                long remainingSeconds = Math.max(0, (deadline - now) / 1000L);
                activity("Waiting for manual Naukri login. Time remaining: " + remainingSeconds + " seconds.");
                nextReminderAt = now + 15_000;
            }
        }
        return new LoginTestResult(false, LoginStatus.CAPTCHA_DETECTED,
                "Manual Naukri login timed out. Click Login Manually & Continue and complete captcha/login within "
                        + safeTimeoutSeconds + " seconds.",
                page.url(),
                null);
    }

    private LoginTestResult classifyCurrentPage() {
        return classifier.classify(page.url(), bodyText(), passwordFieldVisible());
    }

    private LoginForm openLoginForm() {
        for (String loginUrl : LOGIN_URLS) {
            activity("Loading Naukri login page: " + loginUrl);
            page.navigate(loginUrl,
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(45_000));
            human.pause();
            activity("Looking for Naukri login form");
            LoginForm form = findLoginForm(12_000);
            if (form != null) {
                return form;
            }
            activity("Opening Naukri login popup if available");
            clickLoginEntryIfVisible();
            form = findLoginForm(8_000);
            if (form != null) {
                return form;
            }
            LoginTestResult currentState = classifyCurrentPage();
            if (currentState.status() == LoginStatus.CAPTCHA_DETECTED || currentState.status() == LoginStatus.OTP_REQUIRED) {
                return null;
            }
        }
        return null;
    }

    private LoginForm findLoginForm(int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Locator username = firstVisible(USERNAME_SELECTORS);
            Locator password = firstVisible(PASSWORD_SELECTORS);
            if (username != null && password != null) {
                return new LoginForm(username, password);
            }
            waitQuietly(250);
        }
        return null;
    }

    private void clickLoginEntryIfVisible() {
        Locator loginEntry = waitForVisible(LOGIN_ENTRY_SELECTORS, 2_000);
        if (loginEntry != null) {
            try {
                loginEntry.click();
                human.pause();
            } catch (Exception ignored) {
            }
        }
    }

    private Locator waitForVisible(String[] selectors, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Locator visible = firstVisible(selectors);
            if (visible != null) {
                return visible;
            }
            waitQuietly(250);
        }
        return null;
    }

    private Locator firstVisible(String[] selectors) {
        for (String selector : selectors) {
            try {
                Locator locator = page.locator(selector);
                int count = Math.min(locator.count(), 10);
                for (int i = 0; i < count; i++) {
                    Locator candidate = locator.nth(i);
                    if (candidate.isVisible()) {
                        return candidate;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String bodyText() {
        try {
            return page.locator("body").innerText(new Locator.InnerTextOptions().setTimeout(3_000));
        } catch (Exception exception) {
            return "";
        }
    }

    private boolean passwordFieldVisible() {
        try {
            return firstVisible(PASSWORD_SELECTORS) != null;
        } catch (Exception exception) {
            return false;
        }
    }

    private void waitQuietly(int milliseconds) {
        try {
            page.waitForTimeout(milliseconds);
        } catch (Exception ignored) {
        }
    }

    private void activity(String message) {
        activityListener.onActivity(message);
    }

    private record LoginForm(Locator username, Locator password) {
    }
}
