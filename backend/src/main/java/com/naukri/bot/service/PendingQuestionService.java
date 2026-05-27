package com.naukri.bot.service;

import com.naukri.bot.domain.User;
import com.naukri.bot.dto.QuestionDtos.PendingQuestionResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class PendingQuestionService {
    private final ConcurrentMap<Long, PendingQuestion> pendingQuestions = new ConcurrentHashMap<>();

    public Optional<String> waitForAnswer(User user, String question, Duration timeout) {
        PendingQuestion pendingQuestion = new PendingQuestion(
                UUID.randomUUID().toString(),
                user.getId(),
                question,
                Instant.now(),
                Instant.now().plus(timeout),
                new CompletableFuture<>());
        PendingQuestion previous = pendingQuestions.put(user.getId(), pendingQuestion);
        if (previous != null) {
            previous.answer().complete("");
        }
        try {
            String answer = pendingQuestion.answer().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return answer == null || answer.isBlank() ? Optional.empty() : Optional.of(answer);
        } catch (TimeoutException exception) {
            pendingQuestion.answer().complete("");
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception exception) {
            return Optional.empty();
        } finally {
            pendingQuestions.remove(user.getId(), pendingQuestion);
        }
    }

    public Optional<PendingQuestionResponse> current(User user) {
        PendingQuestion pendingQuestion = pendingQuestions.get(user.getId());
        if (pendingQuestion == null || pendingQuestion.answer().isDone()) {
            return Optional.empty();
        }
        return Optional.of(toResponse(pendingQuestion));
    }

    public boolean answer(User user, String questionId, String answer) {
        PendingQuestion pendingQuestion = pendingQuestions.get(user.getId());
        if (pendingQuestion == null || !pendingQuestion.id().equals(questionId)) {
            return false;
        }
        pendingQuestion.answer().complete(answer);
        return true;
    }

    public void cancel(User user) {
        PendingQuestion pendingQuestion = pendingQuestions.remove(user.getId());
        if (pendingQuestion != null) {
            pendingQuestion.answer().complete("");
        }
    }

    private PendingQuestionResponse toResponse(PendingQuestion pendingQuestion) {
        return new PendingQuestionResponse(
                pendingQuestion.id(),
                pendingQuestion.question(),
                pendingQuestion.createdAt(),
                pendingQuestion.expiresAt());
    }

    private record PendingQuestion(
            String id,
            Long userId,
            String question,
            Instant createdAt,
            Instant expiresAt,
            CompletableFuture<String> answer
    ) {
    }
}
