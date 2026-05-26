package com.naukri.bot.controller;

import com.naukri.bot.service.CurrentUserService;
import com.naukri.bot.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportExportService reportExportService;
    private final CurrentUserService currentUserService;

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=applied-jobs.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(reportExportService.exportCsv(currentUserService.currentUser()));
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=applied-jobs.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(reportExportService.exportExcel(currentUserService.currentUser()));
    }
}
