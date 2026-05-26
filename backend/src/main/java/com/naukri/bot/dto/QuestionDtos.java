package com.naukri.bot.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class QuestionDtos {
    private QuestionDtos() {
    }

    public record QuestionAnswerRequest(@NotBlank String question, @NotBlank String answer) {
    }

    public record QuestionAnswerResponse(
            Long id,
            String question,
            String answer,
            double confidenceScore,
            int usageCount,
            Instant lastUsedAt,
            Instant updatedAt
    ) {
    }

    public record AnswerMatchResponse(boolean matched, String answer, double confidenceScore) {
    }
}
