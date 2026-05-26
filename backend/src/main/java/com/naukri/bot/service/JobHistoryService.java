package com.naukri.bot.service;

import com.naukri.bot.domain.User;
import com.naukri.bot.dto.HistoryDtos.AppliedJobResponse;
import com.naukri.bot.repository.AppliedJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobHistoryService {
    private final AppliedJobRepository appliedJobRepository;

    public List<AppliedJobResponse> history(User user, int limit) {
        return appliedJobRepository.findByUserOrderByApplyDateTimeDesc(user, PageRequest.of(0, Math.min(limit, 500))).stream()
                .map(job -> new AppliedJobResponse(job.getId(), job.getCompanyName(), job.getJobTitle(), job.getApplyDateTime(),
                        job.getStatus(), job.isRedirectedExternalSite(), job.getCsvFileName(), job.getFailureReason(),
                        job.getJobUrl(), job.getMatchScore()))
                .toList();
    }
}
