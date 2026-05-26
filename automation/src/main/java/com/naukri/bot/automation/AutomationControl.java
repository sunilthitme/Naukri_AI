package com.naukri.bot.automation;

public interface AutomationControl {
    boolean shouldStop();

    boolean isPaused();

    default void waitIfPaused() {
        while (isPaused() && !shouldStop()) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
