package com.naukri.bot.service;

import com.naukri.bot.domain.QuestionAnswer;
import com.naukri.bot.domain.User;
import com.naukri.bot.dto.QuestionDtos.AnswerMatchResponse;
import com.naukri.bot.dto.QuestionDtos.QuestionAnswerRequest;
import com.naukri.bot.dto.QuestionDtos.QuestionAnswerResponse;
import com.naukri.bot.repository.QuestionAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AIAnswerService {
    private static final double AUTO_ANSWER_THRESHOLD = 0.70;
    private static final double MERGE_QUESTION_THRESHOLD = 0.88;

    private final QuestionAnswerRepository questionAnswerRepository;
    private final QuestionSimilarityService similarityService;

    public List<QuestionAnswerResponse> list(User user) {
        return questionAnswerRepository.findByUserOrderByUpdatedAtDesc(user).stream().map(this::toResponse).toList();
    }

    @Transactional
    public QuestionAnswerResponse save(User user, QuestionAnswerRequest request) {
        String normalized = similarityService.normalize(request.question());
        QuestionAnswer answer = questionAnswerRepository.findByUserAndNormalizedQuestion(user, normalized)
                .orElseGet(() -> bestExisting(user, request.question())
                        .filter(candidate -> candidate.confidence() >= MERGE_QUESTION_THRESHOLD)
                        .map(ScoredQuestionAnswer::answer)
                        .orElseGet(QuestionAnswer::new));
        answer.setUser(user);
        answer.setQuestion(request.question());
        answer.setNormalizedQuestion(normalized);
        answer.setAnswer(request.answer());
        answer.setConfidenceScore(1.0);
        answer.setUpdatedAt(Instant.now());
        questionAnswerRepository.save(answer);
        return toResponse(answer);
    }

    @Transactional
    public Optional<String> answerFor(User user, String question) {
        Optional<ScoredQuestionAnswer> bestMatch = bestExisting(user, question)
                .filter(candidate -> candidate.confidence() >= AUTO_ANSWER_THRESHOLD);
        if (bestMatch.isEmpty()) {
            return Optional.empty();
        }
        QuestionAnswer answer = bestMatch.get().answer();
        answer.setLastUsedAt(Instant.now());
        answer.setUsageCount(answer.getUsageCount() + 1);
        answer.setConfidenceScore(bestMatch.get().confidence());
        return Optional.of(answer.getAnswer());
    }

    public AnswerMatchResponse match(User user, String question) {
        return bestExisting(user, question)
                .filter(candidate -> candidate.confidence() >= AUTO_ANSWER_THRESHOLD)
                .map(candidate -> new AnswerMatchResponse(true, candidate.answer().getAnswer(), candidate.confidence()))
                .orElse(new AnswerMatchResponse(false, null, 0));
    }

    private Optional<ScoredQuestionAnswer> bestExisting(User user, String question) {
        List<QuestionAnswer> answers = questionAnswerRepository.findByUserOrderByUpdatedAtDesc(user);
        if (answers == null || answers.isEmpty()) {
            return Optional.empty();
        }
        return answers.stream()
                .map(existing -> new ScoredQuestionAnswer(existing, confidence(question, existing)))
                .max(Comparator.comparingDouble(ScoredQuestionAnswer::confidence));
    }

    private double confidence(String question, QuestionAnswer existing) {
        return Math.max(
                similarityService.confidence(question, existing.getQuestion()),
                similarityService.confidence(question, existing.getNormalizedQuestion())
        );
    }

    private QuestionAnswerResponse toResponse(QuestionAnswer answer) {
        return new QuestionAnswerResponse(answer.getId(), answer.getQuestion(), answer.getAnswer(),
                answer.getConfidenceScore(), answer.getUsageCount(), answer.getLastUsedAt(), answer.getUpdatedAt());
    }

    private record ScoredQuestionAnswer(QuestionAnswer answer, double confidence) {
    }
}
