package com.naukri.bot.automation.playwright;

import com.naukri.bot.automation.model.AutomationJobFilter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NaukriResultFilterMatcher {
    private static final Set<String> GENERIC_KEYWORDS = Set.of(
            "developer", "engineer", "software", "job", "jobs", "role", "opening", "hiring", "senior", "junior"
    );
    private static final Pattern EXPERIENCE_RANGE = Pattern.compile("(\\d{1,2})\\s*(?:-|to)\\s*(\\d{1,2})\\s*(?:yrs?|years?)");
    private static final Pattern EXPERIENCE_MINIMUM = Pattern.compile("(\\d{1,2})\\+?\\s*(?:yrs?|years?)");
    private static final Pattern SALARY_RANGE = Pattern.compile("(\\d{1,3}(?:\\.\\d+)?)\\s*(?:-|to)\\s*(\\d{1,3}(?:\\.\\d+)?)\\s*(?:lpa|lac|lacs|lakhs?)");

    public MatchDecision evaluate(AutomationJobFilter filter, String company, String title, String cardText) {
        String text = normalize(company + " " + title + " " + cardText);
        if (isBlank(text)) {
            return MatchDecision.reject("empty job card");
        }
        if (containsAny(text, filter.blacklistedCompanies())) {
            return MatchDecision.reject("blacklisted company");
        }
        if (hasConfiguredValues(filter.preferredCompanies()) && !containsAny(text, filter.preferredCompanies())) {
            return MatchDecision.reject("not in preferred companies");
        }
        if (!keywordsMatch(filter.keywords(), text)) {
            return MatchDecision.reject("keywords do not match saved filter");
        }
        if (!locationMatches(filter.location(), text)) {
            return MatchDecision.reject("location does not match saved filter");
        }
        if (!experienceMatches(filter.experience(), text)) {
            return MatchDecision.reject("experience does not match saved filter");
        }
        if (!salaryMatches(filter.salary(), text)) {
            return MatchDecision.reject("salary does not match saved filter");
        }
        if (!workModeMatches(filter.workMode(), text)) {
            return MatchDecision.reject("work mode does not match saved filter");
        }
        return MatchDecision.accept();
    }

    private boolean keywordsMatch(String configuredKeywords, String text) {
        List<String> keywords = splitTerms(configuredKeywords).stream()
                .map(this::normalize)
                .filter(keyword -> keyword.length() > 1)
                .filter(keyword -> !GENERIC_KEYWORDS.contains(keyword))
                .toList();
        if (keywords.isEmpty()) {
            return true;
        }
        String normalizedKeywords = normalize(configuredKeywords);
        if (!normalizedKeywords.isBlank() && text.contains(normalizedKeywords)) {
            return true;
        }
        long hits = keywords.stream().filter(text::contains).count();
        int requiredHits = keywords.size() <= 2 ? keywords.size() : (int) Math.ceil(keywords.size() * 0.60);
        return hits >= requiredHits;
    }

    private boolean locationMatches(String configuredLocation, String text) {
        List<String> locations = splitConfiguredValues(configuredLocation);
        if (locations.isEmpty()) {
            return true;
        }
        return locations.stream()
                .flatMap(location -> locationAliases(location).stream())
                .map(this::normalize)
                .anyMatch(text::contains);
    }

    private boolean experienceMatches(String configuredExperience, String text) {
        Optional<Integer> desired = firstInteger(configuredExperience);
        if (desired.isEmpty()) {
            return true;
        }
        Matcher range = EXPERIENCE_RANGE.matcher(text);
        boolean foundExperience = false;
        while (range.find()) {
            foundExperience = true;
            int min = Integer.parseInt(range.group(1));
            int max = Integer.parseInt(range.group(2));
            if (desired.get() >= min && desired.get() <= max) {
                return true;
            }
        }
        if (foundExperience) {
            return false;
        }
        Matcher minimum = EXPERIENCE_MINIMUM.matcher(text);
        while (minimum.find()) {
            int min = Integer.parseInt(minimum.group(1));
            if (desired.get() >= min) {
                return true;
            }
            foundExperience = true;
        }
        return !foundExperience;
    }

    private boolean salaryMatches(String configuredSalary, String text) {
        Optional<Integer> desired = firstInteger(configuredSalary);
        if (desired.isEmpty() || text.contains("not disclosed")) {
            return true;
        }
        Matcher range = SALARY_RANGE.matcher(text);
        boolean foundSalary = false;
        while (range.find()) {
            foundSalary = true;
            double min = Double.parseDouble(range.group(1));
            double max = Double.parseDouble(range.group(2));
            if (desired.get() >= min && desired.get() <= max) {
                return true;
            }
        }
        return !foundSalary;
    }

    private boolean workModeMatches(String configuredWorkMode, String text) {
        String workMode = normalize(configuredWorkMode);
        if (workMode.isBlank()) {
            return true;
        }
        boolean cardHasWorkMode = containsAny(text, "remote", "work from home", "wfh", "hybrid",
                "office", "onsite", "on site", "work from office");
        if (!cardHasWorkMode) {
            return true;
        }
        if (containsAny(workMode, "remote", "work home", "wfh")) {
            return containsAny(text, "remote", "work from home", "wfh");
        }
        if (workMode.contains("hybrid")) {
            return text.contains("hybrid");
        }
        if (containsAny(workMode, "office", "onsite", "on site", "work office")) {
            return containsAny(text, "office", "onsite", "on site", "work from office");
        }
        return true;
    }

    private List<String> locationAliases(String location) {
        String normalized = normalize(location);
        if (normalized.equals("bangalore") || normalized.equals("bengaluru")) {
            return List.of("bangalore", "bengaluru");
        }
        if (normalized.equals("gurgaon") || normalized.equals("gurugram")) {
            return List.of("gurgaon", "gurugram");
        }
        return List.of(location);
    }

    private boolean containsAny(String text, String... values) {
        return Arrays.stream(values).anyMatch(text::contains);
    }

    private boolean containsAny(String text, List<String> values) {
        if (values == null) {
            return false;
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalize)
                .anyMatch(text::contains);
    }

    private boolean hasConfiguredValues(List<String> values) {
        return values != null && values.stream().anyMatch(value -> value != null && !value.isBlank());
    }

    private List<String> splitConfiguredValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[,;/|\\n]+"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }

    private List<String> splitTerms(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("[^a-zA-Z0-9+#.]+"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }

    private Optional<Integer> firstInteger(String value) {
        if (value == null) {
            return Optional.empty();
        }
        Matcher matcher = Pattern.compile("\\d{1,3}").matcher(value);
        return matcher.find() ? Optional.of(Integer.parseInt(matcher.group())) : Optional.empty();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+#.\\- ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record MatchDecision(boolean accepted, String reason) {
        public static MatchDecision accept() {
            return new MatchDecision(true, "");
        }

        public static MatchDecision reject(String reason) {
            return new MatchDecision(false, reason);
        }
    }
}
