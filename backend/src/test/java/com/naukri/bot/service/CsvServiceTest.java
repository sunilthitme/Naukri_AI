package com.naukri.bot.service;

import com.naukri.bot.domain.AppliedJob;
import com.naukri.bot.domain.ApplyStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void writesAppliedJobCsvWithHeader() throws Exception {
        CsvService service = new CsvService(tempDir.toString());
        AppliedJob job = new AppliedJob();
        job.setCompanyName("Example Tech");
        job.setJobTitle("Java Developer");
        job.setApplyDateTime(Instant.parse("2026-05-26T10:15:00Z"));
        job.setStatus(ApplyStatus.SUCCESS);
        job.setJobUrl("https://example.com");
        job.setExperience("5");
        job.setSalary("20 LPA");
        job.setLocation("Bengaluru");

        String fileName = service.writeAppliedJob(job);

        String csv = Files.readString(tempDir.resolve("csv").resolve(fileName));
        assertTrue(csv.contains("Company Name"));
        assertTrue(csv.contains("Example Tech"));
    }
}
