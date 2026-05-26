package com.naukri.bot.automation;

import java.util.Optional;

public interface QuestionAnswerProvider {
    Optional<String> answerFor(String question);
}
