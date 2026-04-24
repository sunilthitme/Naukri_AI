package com.aijobapplyassistant.service.bot;

import java.util.List;

public record BotRunResult(
        int searched,
        int applied,
        int skipped,
        int failed,
        int credentialsCreated,
        boolean stopped,
        String summary,
        List<JobProcessingResult> results
) {
}
