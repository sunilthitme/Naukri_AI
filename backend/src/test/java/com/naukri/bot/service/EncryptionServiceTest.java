package com.naukri.bot.service;

import com.naukri.bot.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EncryptionServiceTest {
    @Test
    void encryptsAndDecryptsValue() {
        AppProperties properties = new AppProperties(
                new AppProperties.Security("01234567890123456789012345678901", "01234567890123456789012345678901", 60),
                new AppProperties.Bootstrap("", ""),
                new AppProperties.Bot("target/test-storage", true, true, true, 0, "", null, "", ""),
                new AppProperties.Llm("", "", ""),
                new AppProperties.Notifications("", "", "", ""));
        EncryptionService service = new EncryptionService(properties);

        String encrypted = service.encrypt("secret");

        assertNotEquals("secret", encrypted);
        assertEquals("secret", service.decrypt(encrypted));
    }
}
