package com.naukri.bot.automation.playwright;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.security.SecureRandom;

public class HumanBehavior {
    private final SecureRandom random = new SecureRandom();
    private final int minDelayMs;
    private final int maxDelayMs;

    public HumanBehavior() {
        this(450, 1_500);
    }

    public HumanBehavior(int minDelayMs, int maxDelayMs) {
        this.minDelayMs = minDelayMs;
        this.maxDelayMs = maxDelayMs;
    }

    public int nextDelay() {
        return minDelayMs + random.nextInt(Math.max(1, maxDelayMs - minDelayMs));
    }

    public void pause() {
        try {
            Thread.sleep(nextDelay());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    public void type(Locator locator, String text) {
        locator.click();
        locator.fill("");
        for (char character : text.toCharArray()) {
            locator.pressSequentially(String.valueOf(character));
            shortPause();
        }
    }

    public void scroll(Page page) {
        page.mouse().wheel(0, 350 + random.nextInt(550));
        pause();
    }

    private void shortPause() {
        try {
            Thread.sleep(30 + random.nextInt(80));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
