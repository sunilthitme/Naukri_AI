package com.naukri.bot.service;

import com.naukri.bot.domain.JobFilter;
import com.naukri.bot.service.llm.LlmClient;
import com.naukri.bot.util.ListTextMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SmartMatchService {
    private final LlmClient llmClient;

    public double score(JobFilter filter, String jobTitle, String companyName, String description) {
        String llmPrompt = "Return only a number from 0 to 100 for how well this job matches the candidate filters.\n"
                + "Filters: " + filter.getKeywords() + " " + filter.getExperience() + " " + filter.getLocation() + "\n"
                + "Job: " + jobTitle + " at " + companyName + "\n" + description;
        return llmClient.complete("You are a strict job relevance scorer.", llmPrompt)
                .flatMap(this::parseScore)
                .orElseGet(() -> fallbackScore(filter, jobTitle + " " + companyName + " " + description));
    }

    private java.util.Optional<Double> parseScore(String value) {
        try {
            String normalized = value.replaceAll("[^0-9.]", "");
            return java.util.Optional.of(Math.max(0, Math.min(100, Double.parseDouble(normalized))));
        } catch (Exception exception) {
            return java.util.Optional.empty();
        }
    }

    private double fallbackScore(JobFilter filter, String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        long hits = ListTextMapper.split(filter.getKeywords()).stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .filter(normalized::contains)
                .count();
        long preferred = ListTextMapper.split(filter.getPreferredCompanies()).stream()
                .map(company -> company.toLowerCase(Locale.ROOT))
                .filter(normalized::contains)
                .count();
        return Math.min(100, (hits * 18) + (preferred * 10) + 35);
    }
}
