package com.naukri.bot.service;

import com.naukri.bot.domain.ApplyStatus;
import com.naukri.bot.domain.BotStatus;
import com.naukri.bot.domain.User;
import com.naukri.bot.dto.DashboardDtos.DashboardResponse;
import com.naukri.bot.repository.AppliedJobRepository;
import com.naukri.bot.repository.ExternalRedirectJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final AppliedJobRepository appliedJobRepository;
    private final ExternalRedirectJobRepository externalRedirectJobRepository;
    private final BotStatusService botStatusService;

    public DashboardResponse dashboard(User user) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        BotStatus status = botStatusService.get(user);
        return new DashboardResponse(
                appliedJobRepository.countByUser(user),
                appliedJobRepository.countByUserAndStatus(user, ApplyStatus.FAILED),
                externalRedirectJobRepository.countByUser(user),
                appliedJobRepository.countByUserAndApplyDateTimeBetween(user,
                        today.atStartOfDay().toInstant(ZoneOffset.UTC),
                        today.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)),
                status.getStatus(),
                status.isRunning(),
                status.getLastRunTime()
        );
    }
}
