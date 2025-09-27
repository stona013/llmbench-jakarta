package de.example.llmbench.api;

import java.util.List;

public final class QualityUtil {
    private QualityUtil() {}

    public static Double scoreByKeywords(String text, List<String> keywords) {
        if (text == null || text.isBlank() || keywords == null || keywords.isEmpty()) return null;
        String norm = normalize(text);
        int hit = 0;
        int total = 0;
        for (String kw : keywords) {
            if (kw == null || kw.isBlank()) continue;
            total++;
            String k = normalize(kw);
            if (k.isEmpty()) continue;
            if (norm.contains(k)) hit++;
        }
        if (total == 0) return null;
        return Math.max(0.0, Math.min(1.0, hit / (double) total));
    }

    private static String normalize(String s) {
        return s.toLowerCase()
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
