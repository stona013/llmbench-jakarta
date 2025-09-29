package de.example.llmbench.api;

import java.util.List;

/**
 * Dienstklasse zur Bewertung der Antwortqualität anhand erwarteter Schlüsselwörter.
 * 
 * Bietet Methoden, um einen Qualitätswert (zwischen 0 und 1) zu berechnen,
 * basierend darauf, wie viele der erwarteten Schlüsselwörter im Antworttext vorkommen.
 */
public final class QualityUtil {
    // Privater Konstruktor verhindert Instanziierung
    private QualityUtil() {}

    /**
     * Bewertet die Qualität eines Textes anhand einer Liste von Schlüsselwörtern.
     * 
     * @param text Antworttext
     * @param keywords Erwartete Schlüsselwörter
     * @return Qualitätswert zwischen 0.0 und 1.0 oder null, falls nicht bewertbar
     */
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

    /**
     * Normalisiert einen String für den Vergleich (Kleinschreibung, Whitespace vereinheitlichen).
     * 
     * @param s Eingabestring
     * @return Normalisierter String
     */
    private static String normalize(String s) {
        return s.toLowerCase()
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
