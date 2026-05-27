package com.naukri.bot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Security security,
        Bootstrap bootstrap,
        Bot bot,
        Llm llm,
        Notifications notifications
) {
    public record Security(String jwtSecret, String encryptionKey, long tokenExpirationMinutes, List<String> corsAllowedOriginPatterns) {
    }

    public record Bootstrap(String adminEmail, String adminPassword) {
    }

    public record Bot(String storageDir,
                      boolean headless,
                      boolean dryRun,
                      boolean mock,
                      int maxRetries,
                      String proxyHost,
                      Integer proxyPort,
                      String proxyUsername,
                      String proxyPassword,
                      int manualLoginTimeoutSeconds,
                      int questionAnswerTimeoutSeconds) {
    }

    public record Llm(String baseUrl, String apiKey, String model) {
    }

    public record Notifications(String mailFrom, String mailTo, String telegramToken, String telegramChatId) {
    }
}
