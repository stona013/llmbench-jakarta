package de.example.llmbench.api;

import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class BenchmarkDto {

    public static class BenchRequest {
        public String provider;
        public String model;
        public String prompt;
        public Double temperature;
        public Integer maxTokens;
        public Integer runs;
        public Integer timeoutMs;
        public Integer concurrency;

        // neu: erwartete Keywords für Quality-Bewertung (optional)
        public List<String> expectedKeywords;

        public BenchRequest() {}
    }

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

            // neu: reiner Antworttext, falls verfügbar
            String text,

            // neu: Quality-Score 0..1, falls berechnet
            Double quality
    ) {
        public double durationMs() { return (endNanos - startNanos) / 1_000_000.0; }
    }

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

    public static record BenchResponse(
            String timestamp,
            Map<String, Object> request,
            SingleRunResult[] results,
            Aggregates aggregates
    ) {
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
