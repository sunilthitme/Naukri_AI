package com.naukri.bot.service;

import com.naukri.bot.domain.ResumeProfile;
import com.naukri.bot.domain.User;
import com.naukri.bot.repository.ResumeProfileRepository;
import com.naukri.bot.util.ListTextMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ResumeSelectionService {
    private final ResumeProfileRepository resumeProfileRepository;

    public List<String> selectResumePaths(User user, String keywords) {
        List<ResumeProfile> profiles = resumeProfileRepository.findByUserAndActiveTrue(user);
        if (profiles.isEmpty()) {
            return List.of();
        }
        String normalizedKeywords = keywords == null ? "" : keywords.toLowerCase(Locale.ROOT);
        return profiles.stream()
                .max(Comparator.comparingInt(profile -> score(profile, normalizedKeywords)))
                .map(profile -> List.of(profile.getFilePath()))
                .orElse(List.of());
    }

    private int score(ResumeProfile profile, String keywords) {
        return (int) ListTextMapper.split(profile.getKeywords()).stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .filter(keywords::contains)
                .count();
    }
}
