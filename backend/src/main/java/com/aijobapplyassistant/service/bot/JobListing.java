package com.aijobapplyassistant.service.bot;

public record JobListing(
        String company,
        String title,
        String portal,
        String jobUrl,
        String externalUrl,
        String description,
        boolean easyApply,
        String postedText
) {
}
