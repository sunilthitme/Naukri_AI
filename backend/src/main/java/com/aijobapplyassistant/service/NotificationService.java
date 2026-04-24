package com.aijobapplyassistant.service;

import com.aijobapplyassistant.config.ApplicationProperties;
import com.aijobapplyassistant.dto.report.DailyReportResponse;
import com.aijobapplyassistant.entity.User;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class NotificationService {

    private final ApplicationProperties properties;
    private final JavaMailSender mailSender;
    private final WebClient webClient;
    private final SystemLogService systemLogService;

    public NotificationService(ApplicationProperties properties, JavaMailSender mailSender, WebClient webClient,
            SystemLogService systemLogService) {
        this.properties = properties;
        this.mailSender = mailSender;
        this.webClient = webClient;
        this.systemLogService = systemLogService;
    }

    public void notifyReport(User user, DailyReportResponse report) {
        sendEmail(user, report);
        sendTelegram(user, report);
        sendWhatsapp(user, report);
    }

    private void sendEmail(User user, DailyReportResponse report) {
        if (!properties.getNotifications().isEmailEnabled()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Daily Job Apply Report");
            message.setText("Applied: " + report.totalApplied() + ", Failed: " + report.totalFailed() + ", Report: " + report.pdfPath());
            mailSender.send(message);
        } catch (Exception exception) {
            systemLogService.warn(user, "NotificationService", "Email notification failed", exception.getMessage());
        }
    }

    private void sendTelegram(User user, DailyReportResponse report) {
        if (!properties.getNotifications().isTelegramEnabled() || properties.getNotifications().getTelegramBotToken().isBlank()) {
            return;
        }
        try {
            String url = "https://api.telegram.org/bot" + properties.getNotifications().getTelegramBotToken() + "/sendMessage";
            webClient.post().uri(url)
                    .bodyValue(java.util.Map.of(
                            "chat_id", properties.getNotifications().getTelegramChatId(),
                            "text", "Daily job report: applied " + report.totalApplied() + ", failed " + report.totalFailed()
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception exception) {
            systemLogService.warn(user, "NotificationService", "Telegram notification failed", exception.getMessage());
        }
    }

    private void sendWhatsapp(User user, DailyReportResponse report) {
        if (!properties.getNotifications().isWhatsappEnabled() || properties.getNotifications().getWhatsappWebhookUrl().isBlank()) {
            return;
        }
        try {
            webClient.post().uri(properties.getNotifications().getWhatsappWebhookUrl())
                    .bodyValue(java.util.Map.of("text", "Daily job report: applied " + report.totalApplied() + ", failed " + report.totalFailed()))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception exception) {
            systemLogService.warn(user, "NotificationService", "WhatsApp notification failed", exception.getMessage());
        }
    }
}
