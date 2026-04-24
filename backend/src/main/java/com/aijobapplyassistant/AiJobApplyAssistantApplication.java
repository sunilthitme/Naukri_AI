package com.aijobapplyassistant;

import com.aijobapplyassistant.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class AiJobApplyAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiJobApplyAssistantApplication.class, args);
    }
}
