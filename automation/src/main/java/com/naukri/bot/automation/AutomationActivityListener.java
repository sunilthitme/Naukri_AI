package com.naukri.bot.automation;

@FunctionalInterface
public interface AutomationActivityListener {
    AutomationActivityListener NOOP = message -> {
    };

    void onActivity(String message);
}
