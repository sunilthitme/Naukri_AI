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

    @Test
    void expectedCompensationDoesNotBecomeExperience() {
        assertEquals("expected compensation", service.normalize("Expected CTC?"));
    }

    @Test
    void recognizesRepeatedRelocationQuestions() {
        double score = service.confidence(
                "Are you currently residing in Bengaluru or willing to relocate to Bengaluru?",
                "Are you willing to relocate?");

        assertTrue(score > 0.90);
    }

    @Test
    void keepsLocationChoiceSeparateFromRelocationYesNo() {
        double score = service.confidence(
                "Please select the city you are currently residing or willing to relocate to",
                "Are you willing to relocate?");

        assertTrue(score < 0.70);
    }
}
