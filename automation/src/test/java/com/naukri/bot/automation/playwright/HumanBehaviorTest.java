package com.naukri.bot.automation.playwright;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanBehaviorTest {
    @Test
    void delayIsWithinConfiguredBounds() {
        HumanBehavior behavior = new HumanBehavior(10, 20);

        int delay = behavior.nextDelay();

        assertTrue(delay >= 10);
        assertTrue(delay < 20);
    }
}
