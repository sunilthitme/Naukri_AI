package com.aijobapplyassistant.controller;

import com.aijobapplyassistant.dto.common.ApiResponse;
import com.aijobapplyassistant.dto.question.AnswerQuestionRequest;
import com.aijobapplyassistant.dto.question.AiSuggestionResponse;
import com.aijobapplyassistant.dto.question.QuestionResponse;
import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.service.QuestionQueueService;
import com.aijobapplyassistant.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionQueueService questionQueueService;
    private final UserProfileService userProfileService;

    public QuestionController(QuestionQueueService questionQueueService, UserProfileService userProfileService) {
        this.questionQueueService = questionQueueService;
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ApiResponse<List<QuestionResponse>> openQuestions(@AuthenticationPrincipal String email) {
        User user = userProfileService.findByEmail(email);
        return ApiResponse.ok(questionQueueService.getOpenQuestions(user.getId()), "Open questions loaded");
    }

    @PostMapping("/{questionId}/answer")
    public ApiResponse<QuestionResponse> answer(@AuthenticationPrincipal String email, @PathVariable Long questionId,
            @Valid @RequestBody AnswerQuestionRequest request) {
        User user = userProfileService.findByEmail(email);
        return ApiResponse.ok(questionQueueService.answer(user, questionId, request.answer()), "Answer saved");
    }

    @PostMapping("/{questionId}/suggest")
    public ApiResponse<AiSuggestionResponse> suggest(@AuthenticationPrincipal String email, @PathVariable Long questionId) {
        User user = userProfileService.findByEmail(email);
        String answer = questionQueueService.suggestAnswer(user, questionId);
        return ApiResponse.ok(new AiSuggestionResponse(answer), "AI suggestion generated");
    }
}
