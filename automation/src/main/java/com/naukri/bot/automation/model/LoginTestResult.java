package com.naukri.bot.automation.model;

public record LoginTestResult(
        boolean success,
        LoginStatus status,
        String message,
        String currentUrl,
        String screenshotPath
) {
    public static LoginTestResult success(String currentUrl) {
        return new LoginTestResult(true, LoginStatus.SUCCESS, "Naukri login succeeded", currentUrl, null);
    }

    public LoginTestResult withScreenshot(String screenshotPath) {
        return new LoginTestResult(success, status, message, currentUrl, screenshotPath);
    }
}
