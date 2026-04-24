package com.aijobapplyassistant.service.report;

import com.aijobapplyassistant.config.ApplicationProperties;
import com.aijobapplyassistant.dto.report.DailyReportResponse;
import com.aijobapplyassistant.dto.report.ReportRowResponse;
import com.aijobapplyassistant.entity.AutomationRun;
import com.aijobapplyassistant.entity.JobApplication;
import com.aijobapplyassistant.entity.User;
import com.aijobapplyassistant.entity.enums.ApplicationStatus;
import com.aijobapplyassistant.repository.AutomationRunRepository;
import com.aijobapplyassistant.repository.JobApplicationRepository;
import com.aijobapplyassistant.service.SystemLogService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final JobApplicationRepository jobApplicationRepository;
    private final AutomationRunRepository automationRunRepository;
    private final ApplicationProperties properties;
    private final SystemLogService systemLogService;

    public ReportService(JobApplicationRepository jobApplicationRepository, AutomationRunRepository automationRunRepository,
            ApplicationProperties properties, SystemLogService systemLogService) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.automationRunRepository = automationRunRepository;
        this.properties = properties;
        this.systemLogService = systemLogService;
    }

    public DailyReportResponse generateDailyReport(User user, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<JobApplication> applications = jobApplicationRepository.findByUserIdAndAppliedDateBetweenOrderByAppliedDateDesc(user.getId(), start, end);
        List<ReportRowResponse> rows = applications.stream().map(this::toRow).toList();
        AutomationRun run = automationRunRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId()).orElse(null);

        Path reportDir = Path.of(properties.getStorage().getReportsDir());
        try {
            Files.createDirectories(reportDir);
            String baseName = "daily-report-" + user.getId() + "-" + date;
            Path csvPath = reportDir.resolve(baseName + ".csv");
            Path excelPath = reportDir.resolve(baseName + ".xlsx");
            Path pdfPath = reportDir.resolve(baseName + ".pdf");

            writeCsv(csvPath, rows);
            writeExcel(excelPath, rows);
            writePdf(pdfPath, date, rows);

            return new DailyReportResponse(
                    date,
                    rows.size(),
                    rows.stream().filter(item -> item.status() == ApplicationStatus.APPLIED).count(),
                    rows.stream().filter(item -> item.status() == ApplicationStatus.SKIPPED).count(),
                    rows.stream().filter(item -> item.status() == ApplicationStatus.FAILED).count(),
                    run == null ? 0 : run.getCredentialsCreated(),
                    pdfPath.toString(),
                    excelPath.toString(),
                    csvPath.toString(),
                    rows
            );
        } catch (IOException exception) {
            systemLogService.error(user, "ReportService", "Daily report generation failed", exception.getMessage());
            throw new IllegalStateException("Unable to create daily report", exception);
        }
    }

    private void writeCsv(Path path, List<ReportRowResponse> rows) throws IOException {
        try (CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(path), CSVFormat.DEFAULT.withHeader(
                "Date", "Company", "Title", "Portal", "Status", "Remarks", "Relevance Score"))) {
            for (ReportRowResponse row : rows) {
                printer.printRecord(row.appliedDate(), row.company(), row.title(), row.portal(), row.status(), row.remarks(), row.relevanceScore());
            }
        }
    }

    private void writeExcel(Path path, List<ReportRowResponse> rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Applications");
            Row header = sheet.createRow(0);
            String[] headers = {"Date", "Company", "Title", "Portal", "Status", "Remarks", "Relevance Score"};
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
            }
            int rowIndex = 1;
            for (ReportRowResponse item : rows) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(item.appliedDate() == null ? "" : item.appliedDate().toString());
                row.createCell(1).setCellValue(item.company());
                row.createCell(2).setCellValue(item.title());
                row.createCell(3).setCellValue(item.portal());
                row.createCell(4).setCellValue(item.status().name());
                row.createCell(5).setCellValue(item.remarks());
                row.createCell(6).setCellValue(item.relevanceScore());
            }
            workbook.write(Files.newOutputStream(path));
        }
    }

    private void writePdf(Path path, LocalDate date, List<ReportRowResponse> rows) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(font, 12);
                stream.newLineAtOffset(40, 760);
                stream.showText("AI Job Apply Assistant - Daily Report - " + date);
                int lineOffset = 20;
                for (ReportRowResponse row : rows) {
                    stream.newLineAtOffset(0, -lineOffset);
                    String line = "%s | %s | %s | %s".formatted(
                            row.company(),
                            row.title(),
                            row.portal(),
                            row.status().name()
                    );
                    stream.showText(line.length() > 95 ? line.substring(0, 95) : line);
                }
                stream.endText();
            }
            document.save(path.toFile());
        }
    }

    private ReportRowResponse toRow(JobApplication item) {
        return new ReportRowResponse(
                item.getAppliedDate(),
                item.getCompany(),
                item.getTitle(),
                item.getPortal(),
                item.getStatus(),
                item.getRemarks(),
                item.getRelevanceScore()
        );
    }
}
