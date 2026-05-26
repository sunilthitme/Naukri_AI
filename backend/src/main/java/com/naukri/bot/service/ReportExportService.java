package com.naukri.bot.service;

import com.naukri.bot.domain.AppliedJob;
import com.naukri.bot.domain.User;
import com.naukri.bot.repository.AppliedJobRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportExportService {
    private final AppliedJobRepository appliedJobRepository;

    public byte[] exportCsv(User user) {
        try {
            StringWriter writer = new StringWriter();
            try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                    .setHeader("Company Name", "Job Title", "Apply DateTime", "Status", "Job URL", "Failure Reason").build())) {
                for (AppliedJob job : jobs(user)) {
                    printer.printRecord(job.getCompanyName(), job.getJobTitle(), job.getApplyDateTime(),
                            job.getStatus(), job.getJobUrl(), job.getFailureReason());
                }
            }
            return writer.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to export CSV", exception);
        }
    }

    public byte[] exportExcel(User user) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Applied Jobs");
            Row header = sheet.createRow(0);
            String[] columns = {"Company Name", "Job Title", "Apply DateTime", "Status", "Job URL", "Failure Reason"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }
            int rowIndex = 1;
            for (AppliedJob job : jobs(user)) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(job.getCompanyName());
                row.createCell(1).setCellValue(job.getJobTitle());
                row.createCell(2).setCellValue(String.valueOf(job.getApplyDateTime()));
                row.createCell(3).setCellValue(job.getStatus().name());
                row.createCell(4).setCellValue(job.getJobUrl());
                row.createCell(5).setCellValue(job.getFailureReason());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to export Excel", exception);
        }
    }

    private List<AppliedJob> jobs(User user) {
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        Instant start = now.minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = now.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return appliedJobRepository.findForDateRange(user, start, end);
    }
}
