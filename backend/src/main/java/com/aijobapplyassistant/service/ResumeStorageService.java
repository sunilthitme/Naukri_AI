package com.aijobapplyassistant.service;

import com.aijobapplyassistant.config.ApplicationProperties;
import com.aijobapplyassistant.dto.profile.ResumeUploadResponse;
import com.aijobapplyassistant.exception.ApiException;
import com.aijobapplyassistant.service.ai.ResumeParsingService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeStorageService {

    private final ApplicationProperties properties;
    private final ResumeParsingService resumeParsingService;

    public ResumeStorageService(ApplicationProperties properties, ResumeParsingService resumeParsingService) {
        this.properties = properties;
        this.resumeParsingService = resumeParsingService;
    }

    @PostConstruct
    void initializeFolders() throws IOException {
        Files.createDirectories(Path.of(properties.getStorage().getResumesDir()));
        Files.createDirectories(Path.of(properties.getStorage().getReportsDir()));
        Files.createDirectories(Path.of(properties.getStorage().getLogsDir()));
        Files.createDirectories(Path.of(properties.getStorage().getDriversDir()));
    }

    public ResumeUploadResponse store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Resume file is empty");
        }

        try {
            String safeName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path destination = Path.of(properties.getStorage().getResumesDir()).resolve(safeName).normalize();
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            String extractedText = resumeParsingService.extractText(destination);
            String summary = extractedText.length() > 300 ? extractedText.substring(0, 300) + "..." : extractedText;
            return new ResumeUploadResponse(file.getOriginalFilename(), destination.toString(), summary);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Resume upload failed");
        }
    }
}
