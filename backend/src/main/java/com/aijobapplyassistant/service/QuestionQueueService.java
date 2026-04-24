package com.aijobapplyassistant.service;

import com.aijobapplyassistant.dto.question.QuestionResponse;
import com.aijobapplyassistant.service.ai.OllamaAiService;
import com.aijobapplyassistant.entity.JobApplication;
import com.aijobapplyassistant.entity.QuestionQueueItem;
import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.entity.enums.QuestionStatus;
import com.aijobapplyassistant.exception.ApiException;
import com.aijobapplyassistant.repository.QuestionQueueRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class QuestionQueueService {

    private final QuestionQueueRepository repository;
    private final OllamaAiService ollamaAiService;

    public QuestionQueueService(QuestionQueueRepository repository, OllamaAiService ollamaAiService) {
        this.repository = repository;
        this.ollamaAiService = ollamaAiService;
    }

    public QuestionQueueItem createQuestion(User user, JobApplication application, String fieldLabel, String question, String sourcePortal) {
        QuestionQueueItem item = new QuestionQueueItem();
        item.setUser(user);
        item.setJobApplication(application);
        item.setFieldLabel(fieldLabel);
        item.setQuestion(question);
        item.setSourcePortal(sourcePortal);
        item.setStatus(QuestionStatus.OPEN);
        return repository.save(item);
    }

    public List<QuestionResponse> getOpenQuestions(Long userId) {
        return repository.findByUserIdAndStatusOrderByCreatedAtAsc(userId, QuestionStatus.OPEN).stream()
                .map(this::toResponse)
                .toList();
    }

    public QuestionResponse answer(User user, Long questionId, String answer) {
        QuestionQueueItem item = findUserQuestion(user, questionId);
        item.setAnswer(answer);
        item.setStatus(QuestionStatus.ANSWERED);
        item.setAnsweredAt(LocalDateTime.now());
        repository.save(item);
        return toResponse(item);
    }

    public String suggestAnswer(User user, Long questionId) {
        QuestionQueueItem item = findUserQuestion(user, questionId);
        return ollamaAiService.suggestQuestionAnswer(user, item);
    }

    public int countOpenQuestions(Long userId) {
        return repository.findByUserIdAndStatusOrderByCreatedAtAsc(userId, QuestionStatus.OPEN).size();
    }

    private QuestionResponse toResponse(QuestionQueueItem item) {
        return new QuestionResponse(item.getId(), item.getQuestion(), item.getFieldLabel(), item.getSourcePortal(), item.getStatus(), item.getAnswer());
    }

    private QuestionQueueItem findUserQuestion(User user, Long questionId) {
        QuestionQueueItem item = repository.findById(questionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Question not found"));
        if (!item.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Question does not belong to the current user");
        }
        return item;
    }
}
