package com.aijobapplyassistant.dto.question;

import com.aijobapplyassistant.entity.enums.QuestionStatus;

public record QuestionResponse(
        Long id,
        String question,
        String fieldLabel,
        String sourcePortal,
        QuestionStatus status,
        String answer
) {
}
