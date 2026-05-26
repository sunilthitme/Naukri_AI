package com.naukri.bot.automation.model;

import java.time.Instant;

public record ExternalRedirectResult(
        String companyName,
        String redirectUrl,
        Instant redirectDateTime,
        ApplyStatus status
) {
}
