package de.example.llmbench.api;

import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Datenübertragungsobjekte (DTOs) für Benchmark-Anfragen und -Antworten.
 * 
 * Enthält verschachtelte Klassen für die Repräsentation von:
 * - BenchRequest: Parameter für einen Benchmark-Lauf
 * - SingleRunResult: Ergebnis eines einzelnen Durchlaufs
 * - Aggregates: Statistische Auswertung mehrerer Durchläufe
 * - BenchResponse: Antwortobjekt mit allen Ergebnissen und Metadaten
 */
public class BenchmarkDto {

    /**
     * Repräsentiert die Parameter einer Benchmark-Anfrage.
     * Felder sind öffentlich für einfache Serialisierung.
     * expectedKeywords ist optional und dient der Qualitätsbewertung.
     */
    public static class BenchRequest {
        public String provider;           // Name des LLM-Providers
        public String model;              // Modellbezeichnung
        public String prompt;             // Eingabetext
        public Double temperature;        // Sampling-Temperatur
        public Integer maxTokens;         // Maximale Token-Anzahl
        public Integer runs;              // Anzahl der Durchläufe
        public Integer timeoutMs;         // Timeout pro Anfrage (ms)
        public Integer concurrency;       // Parallele Anfragen

        // Optional: Erwartete Schlüsselwörter für Qualitätsbewertung
        public List<String> expectedKeywords;

        public BenchRequest() {}
    }

    /**
     * Ergebnis eines einzelnen Benchmark-Durchlaufs.
     * Enthält Timing, Status, Token-Statistiken, Antworttext und Qualitätsbewertung.
     */
    public static record SingleRunResult(
            String provider,
            String model,
            long startNanos,
            long endNanos,
            int httpStatus,
            boolean success,
            String error,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            Integer responseBytes,

            // Antworttext (falls verfügbar)
            String text,

            // Qualitätsbewertung (0..1, falls berechnet)
            Double quality
    ) {
        /**
         * Berechnet die Dauer des Durchlaufs in Millisekunden.
         */
        public double durationMs() { return (endNanos - startNanos) / 1_000_000.0; }
    }

    /**
     * Statistische Auswertung mehrerer Durchläufe.
     * Enthält typische Metriken wie Durchschnitt, Minimum, Maximum und Perzentile.
     */
    public static record Aggregates(
            int runs,
            double avgMs,
            double minMs,
            double maxMs,
            double p50Ms,
            double p90Ms,
            double p95Ms
            // Hinweis: Quality-Statistik fügen wir nicht hier ein,
            // sondern als Felder im request-Block unten.
    ) {}

    /**
     * Antwortobjekt für einen Benchmark-Lauf.
     * Enthält Zeitstempel, Anfrageparameter, Einzelergebnisse und Statistiken.
     */
    public static record BenchResponse(
            String timestamp,
            Map<String, Object> request,
            SingleRunResult[] results,
            Aggregates aggregates
    ) {
        /**
         * Hilfsmethode zum Erzeugen einer BenchResponse aus Anfrage, Ergebnissen und Statistik.
         * Die Anfrageparameter werden als Map serialisiert.
         */
        public static BenchResponse of(BenchRequest req, SingleRunResult[] results, Aggregates agg) {
            Map<String, Object> reqMap = new LinkedHashMap<>();
            reqMap.put("provider", req.provider);
            reqMap.put("model", req.model);
            reqMap.put("runs", req.runs);
            reqMap.put("concurrency", req.concurrency);
            reqMap.put("timeoutMs", req.timeoutMs);
            reqMap.put("temperature", req.temperature);
            reqMap.put("maxTokens", req.maxTokens);
            reqMap.put("promptChars", req.prompt == null ? 0 : req.prompt.length());
            if (req.expectedKeywords != null && !req.expectedKeywords.isEmpty()) {
                reqMap.put("expectedKeywords", req.expectedKeywords);
            }
            return new BenchResponse(Instant.now().toString(), reqMap, results, agg);
        }
    }
}
