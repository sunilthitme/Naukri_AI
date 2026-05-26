package com.naukri.bot.service.llm;

import java.util.Optional;

public interface LlmClient {
    Optional<String> complete(String systemPrompt, String userPrompt);
}
