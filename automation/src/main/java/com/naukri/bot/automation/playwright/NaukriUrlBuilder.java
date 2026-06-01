package com.naukri.bot.automation.playwright;

import com.naukri.bot.automation.model.AutomationJobFilter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

public class NaukriUrlBuilder {
    public String searchUrl(AutomationJobFilter filter) {
        StringJoiner query = new StringJoiner("&");
        add(query, "k", filter.keywords());
        add(query, "l", filter.location());
        add(query, "experience", filter.experience());
        add(query, "ctcFilter", filter.salary());
        add(query, "jobAge", filter.freshness());
        add(query, "wfhType", workModeCode(filter.workMode()));
        String path = searchPath(filter);
        return query.length() == 0 ? path : path + "?" + query;
    }

    private void add(StringJoiner query, String key, String value) {
        if (value != null && !value.isBlank()) {
            query.add(key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }

    private String searchPath(AutomationJobFilter filter) {
        String keywordSlug = slug(filter.keywords());
        String locationSlug = slug(filter.location());
        StringBuilder path = new StringBuilder("https://www.naukri.com/");
        path.append(keywordSlug.isBlank() ? "jobs" : keywordSlug + "-jobs");
        path.append(locationSlug.isBlank() ? "-in-india" : "-in-" + locationSlug);
        return path.toString();
    }

    private String slug(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
    }

    private String workModeCode(String workMode) {
        if (workMode == null || workMode.isBlank()) {
            return null;
        }
        String normalized = workMode.toLowerCase();
        if (normalized.contains("remote") || normalized.contains("home") || normalized.contains("wfh")) {
            return "2";
        }
        if (normalized.contains("hybrid")) {
            return "3";
        }
        return null;
    }
}
