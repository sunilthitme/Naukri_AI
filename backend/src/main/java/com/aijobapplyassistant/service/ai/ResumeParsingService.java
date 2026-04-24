package com.aijobapplyassistant.service.ai;

import com.aijobapplyassistant.exception.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ResumeParsingService {

    public String extractText(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        try {
            if (fileName.endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(path.toFile())) {
                    return new PDFTextStripper().getText(document);
                }
            }
            if (fileName.endsWith(".docx")) {
                try (XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
                        XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    return extractor.getText();
                }
            }
            return Files.readString(path);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unable to parse resume file");
        }
    }
}
