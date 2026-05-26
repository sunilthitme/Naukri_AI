package com.naukri.bot.controller;

import com.naukri.bot.dto.QuestionDtos.AnswerMatchResponse;
import com.naukri.bot.dto.QuestionDtos.QuestionAnswerRequest;
import com.naukri.bot.dto.QuestionDtos.QuestionAnswerResponse;
import com.naukri.bot.service.AIAnswerService;
import com.naukri.bot.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}
