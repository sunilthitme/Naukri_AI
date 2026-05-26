package com.naukri.bot.automation.model;

public record DiscoveredJob(
        String companyName,
        String jobTitle,
        String jobUrl,
        String experience,
        String salary,
        String location,
        String description
) {
}
