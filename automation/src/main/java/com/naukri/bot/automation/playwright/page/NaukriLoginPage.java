package com.naukri.bot.automation.playwright.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitUntilState;
import com.naukri.bot.automation.model.LoginStatus;
import com.naukri.bot.automation.model.LoginTestResult;
import com.naukri.bot.automation.playwright.HumanBehavior;

public class NaukriLoginPage {
    private static final String LOGIN_URL = "https://www.naukri.com/nlogin/login";
    private static final String USERNAME_SELECTOR = "#usernameField, input[name='email'], input[name='username'], " +
            "input[type='email'], input[placeholder*='Email'], input[placeholder*='Username'], input[type='text']";
    private static final String PASSWORD_SELECTOR = "#passwordField, input[name='password'], input[type='password']";
    private static final String LOGIN_BUTTON_SELECTOR = "button[type='submit'], button:has-text('Login'), " +
            "input[type='submit'], .loginButton, .blue-btn";

    private final Page page;
    private final HumanBehavior human = new HumanBehavior();
    private final NaukriLoginStatusClassifier classifier = new NaukriLoginStatusClassifier();

    public NaukriLoginPage(Page page) {
        this.page = page;
    }

    public LoginTestResult login(String email, String password) {
        page.navigate(LOGIN_URL,
                new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        human.pause();
        Locator username = waitForFirst(USERNAME_SELECTOR);
        Locator passwordField = waitForFirst(PASSWORD_SELECTOR);
        if (username == null || passwordField == null) {
            return new LoginTestResult(false, LoginStatus.LOGIN_FORM_NOT_FOUND,
                    "Naukri login form was not found. The login page may have changed or did not load.", page.url(), null);
        }
        human.type(username, email);
        human.pause();
        human.type(passwordField, password);
        human.pause();
        Locator loginButton = waitForFirst(LOGIN_BUTTON_SELECTOR);
        if (loginButton == null) {
            return new LoginTestResult(false, LoginStatus.LOGIN_FORM_NOT_FOUND,
                    "Naukri login button was not found. The login page may have changed.", page.url(), null);
        }
        loginButton.click();
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

    private Locator waitForFirst(String selector) {
        try {
            Locator locator = page.locator(selector).first();
            locator.waitFor(new Locator.WaitForOptions().setTimeout(15_000));
            return locator;
        } catch (Exception exception) {
            return null;
        }
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
            Locator locator = page.locator(PASSWORD_SELECTOR).first();
            return locator.count() > 0 && locator.isVisible();
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
}
