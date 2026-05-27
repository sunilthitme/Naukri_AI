package com.naukri.bot.automation.playwright.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
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
    private final HumanBehavior human = new HumanBehavior();
    private final NaukriLoginStatusClassifier classifier = new NaukriLoginStatusClassifier();

    public NaukriLoginPage(Page page) {
        this.page = page;
    }

    public LoginTestResult login(String email, String password) {
        LoginForm loginForm = openLoginForm();
        if (loginForm == null) {
            LoginTestResult blocked = classifyCurrentPage();
            if (blocked.status() == LoginStatus.CAPTCHA_DETECTED || blocked.status() == LoginStatus.OTP_REQUIRED) {
                return blocked;
            }
            return new LoginTestResult(false, LoginStatus.LOGIN_FORM_NOT_FOUND,
                    "Naukri login form was not found on supported login pages. Current URL: " + page.url(), page.url(), null);
        }
        human.type(loginForm.username(), email);
        human.pause();
        human.type(loginForm.password(), password);
        human.pause();
        Locator loginButton = waitForVisible(LOGIN_BUTTON_SELECTORS, 5_000);
        if (loginButton == null) {
            loginForm.password().press("Enter");
        } else {
            loginButton.click();
        }
        for (int i = 0; i < 20; i++) {
            waitQuietly(750);
            LoginTestResult result = classifyCurrentPage();
            if (result.success()
                    || result.status() == LoginStatus.INVALID_CREDENTIALS
                    || result.status() == LoginStatus.CAPTCHA_DETECTED
                    || result.status() == LoginStatus.OTP_REQUIRED) {
                return result;
            }
        }
        return classifyCurrentPage();
    }

    public boolean captchaDetected() {
        return classifyCurrentPage().status() == LoginStatus.CAPTCHA_DETECTED;
    }

    private LoginTestResult classifyCurrentPage() {
        return classifier.classify(page.url(), bodyText(), passwordFieldVisible());
    }

    private LoginForm openLoginForm() {
        for (String loginUrl : LOGIN_URLS) {
            page.navigate(loginUrl,
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED).setTimeout(45_000));
            human.pause();
            LoginForm form = findLoginForm(12_000);
            if (form != null) {
                return form;
            }
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

    private record LoginForm(Locator username, Locator password) {
    }
}
