package com.naukri.bot.automation.playwright.page;

import com.naukri.bot.automation.model.LoginStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NaukriLoginStatusClassifierTest {
    private final NaukriLoginStatusClassifier classifier = new NaukriLoginStatusClassifier();

    @Test
    void detectsInvalidCredentials() {
        var result = classifier.classify("https://www.naukri.com/nlogin/login",
                "Invalid username or password. Please enter valid credentials.", true);

        assertEquals(LoginStatus.INVALID_CREDENTIALS, result.status());
    }

    @Test
    void detectsCaptcha() {
        var result = classifier.classify("https://www.naukri.com/captcha",
                "Please verify you are human before continuing.", false);

        assertEquals(LoginStatus.CAPTCHA_DETECTED, result.status());
    }

    @Test
    void detectsAccessBlock() {
        var result = classifier.classify("https://www.naukri.com/",
                "Access denied. Try reloading the page.", false);

        assertEquals(LoginStatus.CAPTCHA_DETECTED, result.status());
    }

    @Test
    void detectsSuccessfulProfilePage() {
        var result = classifier.classify("https://www.naukri.com/mnjuser/homepage",
                "My Naukri View Profile Recommended jobs", false);

        assertTrue(result.success());
    }
}
