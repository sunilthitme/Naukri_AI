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
    private static final double AUTO_ANSWER_THRESHOLD = 0.78;

    private final QuestionAnswerRepository questionAnswerRepository;
    private final QuestionSimilarityService similarityService;

    public List<QuestionAnswerResponse> list(User user) {
        return questionAnswerRepository.findByUserOrderByUpdatedAtDesc(user).stream().map(this::toResponse).toList();
    }

    @Transactional
    public QuestionAnswerResponse save(User user, QuestionAnswerRequest request) {
        String normalized = similarityService.normalize(request.question());
        QuestionAnswer answer = questionAnswerRepository.findByUserAndNormalizedQuestion(user, normalized).orElseGet(QuestionAnswer::new);
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
        AnswerMatchResponse match = match(user, question);
        if (!match.matched()) {
            return Optional.empty();
        }
        String normalized = similarityService.normalize(question);
        questionAnswerRepository.findByUserOrderByUpdatedAtDesc(user).stream()
                .max(Comparator.comparingDouble(existing -> similarityService.confidence(normalized, existing.getNormalizedQuestion())))
                .ifPresent(existing -> {
                    existing.setLastUsedAt(Instant.now());
                    existing.setUsageCount(existing.getUsageCount() + 1);
                    existing.setConfidenceScore(match.confidenceScore());
                });
        return Optional.of(match.answer());
    }

    public AnswerMatchResponse match(User user, String question) {
        String normalized = similarityService.normalize(question);
        return questionAnswerRepository.findByUserOrderByUpdatedAtDesc(user).stream()
                .map(existing -> new AnswerMatchResponse(true, existing.getAnswer(),
                        similarityService.confidence(normalized, existing.getNormalizedQuestion())))
                .max(Comparator.comparingDouble(AnswerMatchResponse::confidenceScore))
                .filter(match -> match.confidenceScore() >= AUTO_ANSWER_THRESHOLD)
                .orElse(new AnswerMatchResponse(false, null, 0));
    }

    private QuestionAnswerResponse toResponse(QuestionAnswer answer) {
        return new QuestionAnswerResponse(answer.getId(), answer.getQuestion(), answer.getAnswer(),
                answer.getConfidenceScore(), answer.getUsageCount(), answer.getLastUsedAt(), answer.getUpdatedAt());
    }
}
