package com.naukri.bot.service;

import com.naukri.bot.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final AppProperties properties;
    private final JavaMailSender mailSender;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void dailySummary(String subject, String body) {
        sendEmail(subject, body);
        sendTelegram(subject + "\n" + body);
    }

    private void sendEmail(String subject, String body) {
        try {
            if (properties.notifications().mailTo() == null || properties.notifications().mailTo().isBlank()) {
                return;
            }
            MimeMessage message = mailSender.createMimeMessage();
            message.setFrom(properties.notifications().mailFrom());
            message.setRecipients(MimeMessage.RecipientType.TO, properties.notifications().mailTo());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ignored) {
        }
    }

    private void sendTelegram(String text) {
        try {
            String token = properties.notifications().telegramToken();
            String chatId = properties.notifications().telegramChatId();
            if (token == null || token.isBlank() || chatId == null || chatId.isBlank()) {
                return;
            }
            String payload = "{\"chat_id\":\"%s\",\"text\":\"%s\"}"
                    .formatted(escape(chatId), escape(text));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.telegram.org/bot" + token + "/sendMessage"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
