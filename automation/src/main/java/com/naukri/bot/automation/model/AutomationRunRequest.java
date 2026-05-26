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
        ProxySettings proxy,
        int maxRetries
) {
}
