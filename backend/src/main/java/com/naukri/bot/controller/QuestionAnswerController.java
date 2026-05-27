package com.naukri.bot.controller;

import com.naukri.bot.dto.QuestionDtos.AnswerMatchResponse;
import com.naukri.bot.dto.QuestionDtos.PendingQuestionAnswerRequest;
import com.naukri.bot.dto.QuestionDtos.PendingQuestionResponse;
import com.naukri.bot.dto.QuestionDtos.QuestionAnswerRequest;
import com.naukri.bot.dto.QuestionDtos.QuestionAnswerResponse;
import com.naukri.bot.domain.User;
import com.naukri.bot.exception.AppException;
import com.naukri.bot.service.AIAnswerService;
import com.naukri.bot.service.CurrentUserService;
import com.naukri.bot.service.PendingQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionAnswerController {
    private final AIAnswerService aiAnswerService;
    private final PendingQuestionService pendingQuestionService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<QuestionAnswerResponse> list() {
        return aiAnswerService.list(currentUserService.currentUser());
    }

    @PostMapping
    public QuestionAnswerResponse save(@Valid @RequestBody QuestionAnswerRequest request) {
        return aiAnswerService.save(currentUserService.currentUser(), request);
    }

    @GetMapping("/match")
    public AnswerMatchResponse match(@RequestParam String question) {
        return aiAnswerService.match(currentUserService.currentUser(), question);
    }

    @GetMapping("/pending")
    public ResponseEntity<PendingQuestionResponse> pending() {
        return pendingQuestionService.current(currentUserService.currentUser())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/pending/{id}/answer")
    public QuestionAnswerResponse answerPending(@PathVariable String id,
                                                @Valid @RequestBody PendingQuestionAnswerRequest request) {
        User user = currentUserService.currentUser();
        PendingQuestionResponse pendingQuestion = pendingQuestionService.current(user)
                .filter(question -> question.id().equals(id))
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Pending question was not found or already expired"));
        QuestionAnswerResponse response = aiAnswerService.save(user,
                new QuestionAnswerRequest(pendingQuestion.question(), request.answer()));
        pendingQuestionService.answer(user, id, request.answer());
        return response;
    }
}
