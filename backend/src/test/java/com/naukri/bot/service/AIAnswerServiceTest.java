package com.naukri.bot.service;

import com.naukri.bot.domain.QuestionAnswer;
import com.naukri.bot.domain.User;
import com.naukri.bot.dto.QuestionDtos.QuestionAnswerRequest;
import com.naukri.bot.repository.QuestionAnswerRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIAnswerServiceTest {
    private final QuestionAnswerRepository repository = mock(QuestionAnswerRepository.class);
    private final AIAnswerService service = new AIAnswerService(repository, new QuestionSimilarityService());

    @Test
    void savedAnswerIsReusedForSimilarFutureQuestion() {
        User user = new User();
        when(repository.findByUserAndNormalizedQuestion(eq(user), eq("notice period"))).thenReturn(Optional.empty());
        when(repository.save(any(QuestionAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.save(user, new QuestionAnswerRequest("Notice period?", "30 days"));

        ArgumentCaptor<QuestionAnswer> captor = ArgumentCaptor.forClass(QuestionAnswer.class);
        verify(repository).save(captor.capture());
        QuestionAnswer saved = captor.getValue();
        when(repository.findByUserOrderByUpdatedAtDesc(user)).thenReturn(List.of(saved));

        Optional<String> answer = service.answerFor(user, "What is your notice period?");

        assertTrue(answer.isPresent());
        assertEquals("30 days", answer.get());
        assertEquals(1, saved.getUsageCount());
        assertTrue(saved.getConfidenceScore() >= 0.78);
    }
}
