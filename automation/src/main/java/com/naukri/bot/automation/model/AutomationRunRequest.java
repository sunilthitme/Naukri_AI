package com.naukri.bot.automation.model;

import java.nio.file.Path;
import java.util.List;

public record AutomationRunRequest(
        String naukriEmail,
        String naukriPassword,
        AutomationJobFilter filter,
        List<String> resumePaths,
        Path storageDirectory,
        boolean headless,
        boolean dryRun,
        boolean manualLoginOnCaptcha,
        int manualLoginTimeoutSeconds,
        ProxySettings proxy,
        int maxRetries
) {
}
