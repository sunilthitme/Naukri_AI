package com.naukri.bot.config;

import com.naukri.bot.automation.NaukriAutomationClient;
import com.naukri.bot.automation.mock.MockNaukriAutomationClient;
import com.naukri.bot.automation.playwright.PlaywrightNaukriAutomationClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutomationConfig {
    @Bean
    public NaukriAutomationClient naukriAutomationClient(AppProperties properties) {
        return properties.bot().mock() ? new MockNaukriAutomationClient() : new PlaywrightNaukriAutomationClient();
    }
}
