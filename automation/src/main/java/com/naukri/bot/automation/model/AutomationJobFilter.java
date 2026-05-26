package com.naukri.bot.automation.model;

import java.util.List;

public record AutomationJobFilter(
        String keywords,
        String experience,
        String location,
        String salary,
        String workMode,
        String freshness,
        List<String> preferredCompanies,
        List<String> blacklistedCompanies,
        boolean autoApply,
        boolean externalCareerApply,
        int dailyApplyLimit,
        boolean easyApplyOnly,
        int duplicatePreventionDays
) {
}
