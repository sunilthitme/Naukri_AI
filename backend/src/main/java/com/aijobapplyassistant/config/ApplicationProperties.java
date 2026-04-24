package com.aijobapplyassistant.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private List<String> corsAllowedOrigins = new ArrayList<>();
    private final Security security = new Security();
    private final Ai ai = new Ai();
    private final Scheduler scheduler = new Scheduler();
    private final Storage storage = new Storage();
    private final Automation automation = new Automation();
    private final Notifications notifications = new Notifications();

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public Security getSecurity() {
        return security;
    }

    public Ai getAi() {
        return ai;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Storage getStorage() {
        return storage;
    }

    public Automation getAutomation() {
        return automation;
    }

    public Notifications getNotifications() {
        return notifications;
    }

    public static class Security {
        private String jwtSecret;
        private long jwtExpirationMinutes;
        private String credentialSecret;

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public long getJwtExpirationMinutes() {
            return jwtExpirationMinutes;
        }

        public void setJwtExpirationMinutes(long jwtExpirationMinutes) {
            this.jwtExpirationMinutes = jwtExpirationMinutes;
        }

        public String getCredentialSecret() {
            return credentialSecret;
        }

        public void setCredentialSecret(String credentialSecret) {
            this.credentialSecret = credentialSecret;
        }
    }

    public static class Ai {
        private String ollamaBaseUrl;
        private String model;
        private int timeoutSeconds;

        public String getOllamaBaseUrl() {
            return ollamaBaseUrl;
        }

        public void setOllamaBaseUrl(String ollamaBaseUrl) {
            this.ollamaBaseUrl = ollamaBaseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Scheduler {
        private boolean enabled = true;
        private String cron;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class Storage {
        private String resumesDir;
        private String reportsDir;
        private String logsDir;
        private String driversDir;

        public String getResumesDir() {
            return resumesDir;
        }

        public void setResumesDir(String resumesDir) {
            this.resumesDir = resumesDir;
        }

        public String getReportsDir() {
            return reportsDir;
        }

        public void setReportsDir(String reportsDir) {
            this.reportsDir = reportsDir;
        }

        public String getLogsDir() {
            return logsDir;
        }

        public void setLogsDir(String logsDir) {
            this.logsDir = logsDir;
        }

        public String getDriversDir() {
            return driversDir;
        }

        public void setDriversDir(String driversDir) {
            this.driversDir = driversDir;
        }
    }

    public static class Automation {
        private int minimumRelevanceScore = 70;
        private int humanDelayMinMs = 900;
        private int humanDelayMaxMs = 2200;
        private boolean headless;
        private int maxJobsPerRun = 30;
        private int retryCount = 2;

        public int getMinimumRelevanceScore() {
            return minimumRelevanceScore;
        }

        public void setMinimumRelevanceScore(int minimumRelevanceScore) {
            this.minimumRelevanceScore = minimumRelevanceScore;
        }

        public int getHumanDelayMinMs() {
            return humanDelayMinMs;
        }

        public void setHumanDelayMinMs(int humanDelayMinMs) {
            this.humanDelayMinMs = humanDelayMinMs;
        }

        public int getHumanDelayMaxMs() {
            return humanDelayMaxMs;
        }

        public void setHumanDelayMaxMs(int humanDelayMaxMs) {
            this.humanDelayMaxMs = humanDelayMaxMs;
        }

        public boolean isHeadless() {
            return headless;
        }

        public void setHeadless(boolean headless) {
            this.headless = headless;
        }

        public int getMaxJobsPerRun() {
            return maxJobsPerRun;
        }

        public void setMaxJobsPerRun(int maxJobsPerRun) {
            this.maxJobsPerRun = maxJobsPerRun;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }
    }

    public static class Notifications {
        private boolean emailEnabled;
        private boolean telegramEnabled;
        private String telegramBotToken;
        private String telegramChatId;
        private boolean whatsappEnabled;
        private String whatsappWebhookUrl;

        public boolean isEmailEnabled() {
            return emailEnabled;
        }

        public void setEmailEnabled(boolean emailEnabled) {
            this.emailEnabled = emailEnabled;
        }

        public boolean isTelegramEnabled() {
            return telegramEnabled;
        }

        public void setTelegramEnabled(boolean telegramEnabled) {
            this.telegramEnabled = telegramEnabled;
        }

        public String getTelegramBotToken() {
            return telegramBotToken;
        }

        public void setTelegramBotToken(String telegramBotToken) {
            this.telegramBotToken = telegramBotToken;
        }

        public String getTelegramChatId() {
            return telegramChatId;
        }

        public void setTelegramChatId(String telegramChatId) {
            this.telegramChatId = telegramChatId;
        }

        public boolean isWhatsappEnabled() {
            return whatsappEnabled;
        }

        public void setWhatsappEnabled(boolean whatsappEnabled) {
            this.whatsappEnabled = whatsappEnabled;
        }

        public String getWhatsappWebhookUrl() {
            return whatsappWebhookUrl;
        }

        public void setWhatsappWebhookUrl(String whatsappWebhookUrl) {
            this.whatsappWebhookUrl = whatsappWebhookUrl;
        }
    }
}
