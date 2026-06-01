package com.naukri.bot.automation.playwright.page;

import com.naukri.bot.automation.model.LoginStatus;
import com.naukri.bot.automation.model.LoginTestResult;

import java.util.Locale;

public class NaukriLoginStatusClassifier {
    public LoginTestResult classify(String currentUrl, String bodyText, boolean passwordFieldVisible) {
        String url = normalize(currentUrl);
        String body = normalize(bodyText);
        if (containsAny(url + " " + body,
                "captcha",
                "verify you are human",
                "robot check",
                "security check",
                "access denied",
                "request blocked",
                "temporarily blocked",
                "unusual traffic",
                "too many requests",
                "are you a robot",
                "try reloading the page")) {
            return new LoginTestResult(false, LoginStatus.CAPTCHA_DETECTED,
                    "Captcha, access block, or human verification detected during Naukri login.", currentUrl, null);
        }
        if (authenticatedUrl(url) && !passwordFieldVisible) {
            return LoginTestResult.success(currentUrl);
        }
        if (containsAny(body, "otp", "one time password", "verification code", "verify mobile", "verify email")) {
            return new LoginTestResult(false, LoginStatus.OTP_REQUIRED,
                    "Naukri asked for OTP or account verification.", currentUrl, null);
        }
        if (containsAny(body, "invalid", "incorrect", "wrong password", "not registered", "enter valid",
                "unable to login", "unable to log in", "could not login", "could not log in",
                "account does not exist", "please enter password")) {
            return new LoginTestResult(false, LoginStatus.INVALID_CREDENTIALS,
                    "Naukri rejected the saved credentials.", currentUrl, null);
        }
        if (containsAny(body, "my naukri", "view profile", "update profile", "recommended jobs",
                "profile performance", "logout")) {
            return LoginTestResult.success(currentUrl);
        }
        return new LoginTestResult(false, LoginStatus.STILL_ON_LOGIN_PAGE,
                "Naukri did not leave the login page after submitting credentials.", currentUrl, null);
    }

    private boolean authenticatedUrl(String url) {
        return containsAny(url,
                "naukri.com/mnjuser/",
                "naukri.com/my-naukri");
    }

    private boolean containsAny(String source, String... needles) {
        for (String needle : needles) {
            if (source.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
