package com.naukri.bot.service;

import com.naukri.bot.domain.AppliedJob;
import com.naukri.bot.domain.ExternalRedirectJob;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class CsvService {
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy_MM_dd").withZone(ZoneOffset.UTC);
    private final Path csvDirectory;

    public CsvService(@Value("${app.bot.storage-dir:storage}") String storageDir) {
        this.csvDirectory = Path.of(storageDir, "csv");
    }

    public String writeAppliedJob(AppliedJob job) {
        String fileName = "applied_jobs_" + FILE_DATE.format(job.getApplyDateTime()) + ".csv";
        Path file = csvDirectory.resolve(fileName);
        append(file, new String[]{"Company Name", "Job Title", "Apply DateTime", "Status", "Job URL", "Experience", "Salary", "Location"},
                new Object[]{job.getCompanyName(), job.getJobTitle(), job.getApplyDateTime(), job.getStatus(), job.getJobUrl(),
                        job.getExperience(), job.getSalary(), job.getLocation()});
        return fileName;
    }

    public String writeExternalRedirect(ExternalRedirectJob job) {
        String fileName = "external_redirect_jobs_" + FILE_DATE.format(job.getRedirectDateTime()) + ".csv";
        Path file = csvDirectory.resolve(fileName);
        append(file, new String[]{"Company Name", "Redirect URL", "Redirect DateTime", "Status"},
                new Object[]{job.getCompanyName(), job.getRedirectUrl(), job.getRedirectDateTime(), job.getStatus()});
        return fileName;
    }

    private synchronized void append(Path file, String[] headers, Object[] values) {
        try {
            Files.createDirectories(csvDirectory);
            boolean newFile = Files.notExists(file) || Files.size(file) == 0;
            try (BufferedWriter writer = Files.newBufferedWriter(file, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                 CSVPrinter printer = new CSVPrinter(writer, newFile
                         ? CSVFormat.DEFAULT.builder().setHeader(headers).build()
                         : CSVFormat.DEFAULT)) {
                printer.printRecord(values);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to write CSV file " + file, exception);
        }
    }
}
