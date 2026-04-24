package com.aijobapplyassistant.service.ai;

import com.aijobapplyassistant.config.ApplicationProperties;
import com.aijobapplyassistant.entity.QuestionQueueItem;
import com.aijobapplyassistant.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OllamaAiService {

    private static final Pattern SCORE_PATTERN = Pattern.compile("(\\d{1,3})");
    private final ApplicationProperties properties;
    private final WebClient webClient;

    public OllamaAiService(ApplicationProperties properties, WebClient webClient) {
        this.properties = properties;
        this.webClient = webClient;
    }

    public int scoreJobMatch(String resumeText, String jobDescription) {
        String prompt = """
                Compare this candidate resume with this job description.
                Return only one integer score from 0 to 100.

                Resume:
                %s

                Job Description:
                %s
                """.formatted(trim(resumeText, 6000), trim(jobDescription, 5000));

        String response = generate(prompt);
        Matcher matcher = SCORE_PATTERN.matcher(response);
        if (matcher.find()) {
            return Math.min(100, Integer.parseInt(matcher.group(1)));
        }
        return heuristicScore(resumeText, jobDescription);
    }

    public String resolveFieldValue(String label, User user, String jobDescription) {
        String known = knownFieldValue(label, user);
        if (known != null && !known.isBlank()) {
            return known;
        }

        String prompt = """
                Fill the job application field using candidate details.
                Return value only. No explanation.

                Field label: %s
                Candidate:
                Full name: %s
                Email: %s
                Phone: %s
                Experience: %s
                Current company: %s
                Current CTC: %s
                Expected CTC: %s
                Notice period: %s
                Location: %s
                Skills: %s

                Job Description:
                %s
                """.formatted(
                label,
                safe(user.getFullName()),
                safe(user.getEmail()),
                safe(user.getPhoneNumber()),
                safe(user.getTotalExperience()),
                safe(user.getCurrentCompany()),
                safe(user.getCurrentSalary()),
                safe(user.getExpectedSalary()),
                safe(user.getNoticePeriod()),
                safe(user.getLocationPreference()),
                safe(user.getSkills()),
                trim(jobDescription, 4000)
        );
        return generate(prompt).trim();
    }

    public String generateCoverLetter(User user, String jobDescription) {
        String prompt = """
                Write a short professional cover letter under 180 words.

                Candidate name: %s
                Experience: %s years
                Skills: %s
                Current company: %s
                Job description:
                %s
                """.formatted(
                safe(user.getFullName()),
                safe(user.getTotalExperience()),
                safe(user.getSkills()),
                safe(user.getCurrentCompany()),
                trim(jobDescription, 5000)
        );
        return generate(prompt).trim();
    }

    public String suggestQuestionAnswer(User user, QuestionQueueItem item) {
        String known = knownFieldValue(item.getFieldLabel(), user);
        if (known != null && !known.isBlank()) {
            return known;
        }

        String prompt = """
                You are helping a candidate answer a job application question.
                Return only the answer text. Keep it professional, concise, and truthful.
                If the candidate profile does not contain enough information, say:
                "Please provide this information manually."

                Field label: %s
                Portal: %s
                Question: %s

                Candidate:
                Full name: %s
                Email: %s
                Phone: %s
                Experience: %s
                Current company: %s
                Current CTC: %s
                Expected CTC: %s
                Notice period: %s
                Location: %s
                Skills: %s
                Preferred keywords: %s
                """.formatted(
                safe(item.getFieldLabel()),
                safe(item.getSourcePortal()),
                safe(item.getQuestion()),
                safe(user.getFullName()),
                safe(user.getEmail()),
                safe(user.getPhoneNumber()),
                safe(user.getTotalExperience()),
                safe(user.getCurrentCompany()),
                safe(user.getCurrentSalary()),
                safe(user.getExpectedSalary()),
                safe(user.getNoticePeriod()),
                safe(user.getLocationPreference()),
                safe(user.getSkills()),
                safe(user.getPreferredKeywords())
        );

        String response = generate(prompt).trim();
        return response.isBlank() ? "Please provide this information manually." : response;
    }

    public String generate(String prompt) {
        try {
            JsonNode response = webClient.post()
                    .uri(properties.getAi().getOllamaBaseUrl() + "/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(java.util.Map.of(
                            "model", properties.getAi().getModel(),
                            "prompt", prompt,
                            "stream", false
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            return response == null ? "" : response.path("response").asText("");
        } catch (Exception exception) {
            return "";
        }
    }

    private int heuristicScore(String resumeText, String jobDescription) {
        Set<String> resumeWords = tokenize(resumeText);
        Set<String> jdWords = tokenize(jobDescription);
        if (resumeWords.isEmpty() || jdWords.isEmpty()) {
            return 55;
        }
        long matches = jdWords.stream().filter(resumeWords::contains).count();
        return Math.max(40, (int) ((matches * 100.0) / jdWords.size()));
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        for (String token : safe(text).toLowerCase().split("[^a-z0-9+#.]+")) {
            if (token.length() > 2) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String knownFieldValue(String label, User user) {
        String normalized = safe(label).toLowerCase();
        if (normalized.contains("name")) {
            return user.getFullName();
        }
        if (normalized.contains("email")) {
            return user.getEmail();
        }
        if (normalized.contains("phone") || normalized.contains("mobile")) {
            return user.getPhoneNumber();
        }
        if (normalized.contains("notice")) {
            return user.getNoticePeriod();
        }
        if (normalized.contains("current ctc") || normalized.contains("current salary")) {
            return user.getCurrentSalary();
        }
        if (normalized.contains("expected")) {
            return user.getExpectedSalary();
        }
        if (normalized.contains("experience")) {
            return user.getTotalExperience() == null ? "" : user.getTotalExperience() + " years";
        }
        if (normalized.contains("current company")) {
            return user.getCurrentCompany();
        }
        if (normalized.contains("skills")) {
            return user.getSkills();
        }
        if (normalized.contains("location")) {
            return user.getLocationPreference();
        }
        return null;
    }

    private String trim(String value, int maxLength) {
        String safe = safe(value);
        return safe.length() <= maxLength ? safe : safe.substring(0, maxLength);
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }
}
