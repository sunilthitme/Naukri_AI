package com.naukri.bot.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionSimilarityServiceTest {
    private final QuestionSimilarityService service = new QuestionSimilarityService();

    @Test
    void normalizesCommonQuestionTerms() {
        assertEquals("current compensation", service.normalize("Current CTC?"));
    }

    @Test
    void similarQuestionsScoreAboveThreshold() {
        double score = service.confidence("What is your current CTC?", "Current compensation");
        assertTrue(score > 0.70);
    }
}
