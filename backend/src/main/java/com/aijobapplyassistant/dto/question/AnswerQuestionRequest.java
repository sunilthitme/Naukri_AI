package com.aijobapplyassistant.dto.question;

import jakarta.validation.constraints.NotBlank;

public record AnswerQuestionRequest(@NotBlank String answer) {
}
