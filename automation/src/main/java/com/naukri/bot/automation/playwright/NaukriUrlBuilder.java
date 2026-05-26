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
        return "https://www.naukri.com/jobs-in-india?" + query;
    }

    private void add(StringJoiner query, String key, String value) {
        if (value != null && !value.isBlank()) {
            query.add(key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }
}
