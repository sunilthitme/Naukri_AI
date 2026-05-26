package com.naukri.bot.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class QuestionSimilarityService {
    public String normalize(String question) {
        if (question == null) {
            return "";
        }
        String normalized = Normalizer.normalize(question, Normalizer.Form.NFKD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized
                .replace("ctc", "compensation")
                .replace("yrs", "years")
                .replace("exp", "experience");
    }

    public double confidence(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        if (a.isBlank() || b.isBlank()) {
            return 0;
        }
        if (a.equals(b)) {
            return 1;
        }
        if (a.contains(b) || b.contains(a)) {
            return 0.9;
        }
        double tokenScore = jaccard(tokens(a), tokens(b));
        double editScore = 1.0 - ((double) levenshtein(a, b) / Math.max(a.length(), b.length()));
        return Math.max(0, Math.min(1, (tokenScore * 0.65) + (editScore * 0.35)));
    }

    private Set<String> tokens(String text) {
        return new HashSet<>(Arrays.asList(text.split(" ")));
    }

    private double jaccard(Set<String> a, Set<String> b) {
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }
}
