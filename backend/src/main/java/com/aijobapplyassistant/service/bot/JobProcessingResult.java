package com.aijobapplyassistant.service.bot;

import com.aijobapplyassistant.entity.enums.ApplicationStatus;

public record JobProcessingResult(
        JobListing listing,
        ApplicationStatus status,
        int relevanceScore,
        String remarks,
        boolean credentialCreated
) {
}
