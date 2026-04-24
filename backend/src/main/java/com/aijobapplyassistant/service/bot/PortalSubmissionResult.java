package com.aijobapplyassistant.service.bot;

import com.aijobapplyassistant.entity.enums.ApplicationStatus;

public record PortalSubmissionResult(
        ApplicationStatus status,
        String remarks,
        boolean credentialCreated
) {
}
