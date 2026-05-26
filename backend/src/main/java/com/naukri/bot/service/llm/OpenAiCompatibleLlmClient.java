package com.naukri.bot.service.llm;

import com.naukri.bot.config.AppProperties;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Service
public class OpenAiCompatibleLlmClient implements LlmClient {
    private final AppProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public OpenAiCompatibleLlmClient(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> complete(String systemPrompt, String userPrompt) {
        if (properties.llm().apiKey() == null || properties.llm().apiKey().isBlank()) {
            return Optional.empty();
        }
        try {
            String body = """
                    {"model":"%s","messages":[{"role":"system","content":"%s"},{"role":"user","content":"%s"}],"temperature":0.1}
                    """.formatted(escape(properties.llm().model()), escape(systemPrompt), escape(userPrompt));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.llm().baseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + properties.llm().apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Optional.ofNullable(extractContent(response.body()));
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private String extractContent(String json) {
        int marker = json.indexOf("\"content\"");
        if (marker < 0) {
            return null;
        }
        int colon = json.indexOf(':', marker);
        int start = json.indexOf('"', colon + 1);
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                builder.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return builder.toString();
            } else {
                builder.append(c);
            }
        }
        return null;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
