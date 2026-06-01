package com.naukri.bot.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionSimilarityService {
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "any", "answer", "are", "can", "choose", "continue", "do", "does",
            "enter", "for", "from", "have", "if", "in", "is", "it", "mention", "next", "no",
            "of", "ok", "on", "option", "or", "please", "provide", "save", "select", "submit",
            "the", "this", "to", "what", "which", "with", "would", "yes", "you", "your"
    );

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
        normalized = normalized
                .replaceAll("\\bctc\\b", " compensation ")
                .replaceAll("\\bpackage\\b", " compensation ")
                .replaceAll("\\bsalary\\b", " compensation ")
                .replaceAll("\\blpa\\b", " compensation ")
                .replaceAll("\\byrs\\b", " years ")
                .replaceAll("\\byr\\b", " year ")
                .replaceAll("\\bexp\\b", " experience ")
                .replaceAll("\\bcurr\\b", " current ")
                .replaceAll("\\bjoining\\b", " notice ")
                .replaceAll("\\bjoin\\b", " notice ");
        return Arrays.stream(normalized.replaceAll("\\s+", " ").trim().split(" "))
                .filter(token -> !token.isBlank())
                .filter(token -> !STOP_WORDS.contains(token))
                .map(this::canonicalToken)
                .collect(Collectors.joining(" "))
                .replaceAll("\\s+", " ")
                .trim();
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
        String leftCategory = category(a);
        String rightCategory = category(b);
        if (!leftCategory.isBlank() && leftCategory.equals(rightCategory)) {
            return 0.96;
        }
        if (!leftCategory.isBlank() && !rightCategory.isBlank()) {
            return 0.35;
        }
        if (a.contains(b) || b.contains(a)) {
            return 0.92;
        }
        Set<String> leftTokens = tokens(a);
        Set<String> rightTokens = tokens(b);
        double tokenScore = jaccard(leftTokens, rightTokens);
        double coverageScore = coverage(leftTokens, rightTokens);
        double editScore = 1.0 - ((double) levenshtein(a, b) / Math.max(a.length(), b.length()));
        return Math.max(0, Math.min(1, Math.max(coverageScore * 0.88, (tokenScore * 0.60) + (editScore * 0.40))));
    }

    private String category(String normalizedQuestion) {
        String text = normalizedQuestion == null ? "" : normalizedQuestion;
        if (containsAll(text, "current", "compensation")) {
            return "current_compensation";
        }
        if (containsAny(text, "expected", "expecting", "expectation") && text.contains("compensation")) {
            return "expected_compensation";
        }
        if (containsAny(text, "notice", "available", "availability") && !text.contains("interview")) {
            return "notice_period";
        }
        if (containsAny(text, "city", "location") && containsAny(text, "residing", "relocate", "relocation", "preferred")) {
            return "location_choice";
        }
        if (containsAny(text, "relocate", "relocation")) {
            return "relocation_yes_no";
        }
        if (containsAny(text, "authorization", "authorised", "authorized", "visa", "sponsor", "permit")) {
            return "work_authorization";
        }
        if (text.contains("experience") && !text.contains("interview")) {
            return "experience";
        }
        if (containsAny(text, "skill", "skills", "technology", "technologies")) {
            return "skills";
        }
        if (containsAny(text, "interview", "interviews")) {
            return "interview_availability";
        }
        if (containsAny(text, "qualification", "education", "degree")) {
            return "education";
        }
        if (containsAny(text, "reason", "change", "switch")) {
            return "job_change_reason";
        }
        return "";
    }

    private String canonicalToken(String token) {
        if (Set.of("compensation", "compensations").contains(token)) {
            return "compensation";
        }
        if (Set.of("expected", "expecting", "expectation").contains(token)) {
            return "expected";
        }
        if (Set.of("relocate", "relocating", "relocation").contains(token)) {
            return "relocate";
        }
        if (Set.of("authorized", "authorised", "authorization", "authorisation").contains(token)) {
            return "authorization";
        }
        if (Set.of("skills", "skill").contains(token)) {
            return "skill";
        }
        if (Set.of("interviews", "interview").contains(token)) {
            return "interview";
        }
        return token;
    }

    private boolean containsAll(String text, String... values) {
        return Arrays.stream(values).allMatch(text::contains);
    }

    private boolean containsAny(String text, String... values) {
        return Arrays.stream(values).anyMatch(text::contains);
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

    private double coverage(Set<String> a, Set<String> b) {
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        int smaller = Math.min(a.size(), b.size());
        return smaller == 0 ? 0 : (double) intersection.size() / smaller;
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
