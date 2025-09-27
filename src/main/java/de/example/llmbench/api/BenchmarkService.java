package de.example.llmbench.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BenchmarkService {

    private final OllamaClient ollama = new OllamaClient();

    public BenchmarkDto.SingleRunResult[] runOnceBatch(BenchmarkDto.BenchRequest req) {
        String provider = req.provider == null ? BenchmarkModels.PROVIDER_OLLAMA : req.provider.toLowerCase();
        int runs = req.runs == null || req.runs < 1 ? 1 : Math.min(req.runs, 200);
        int timeoutMs = req.timeoutMs == null ? 60000 : req.timeoutMs;
        double temp = req.temperature == null ? 0.2 : req.temperature;
        int maxTok = req.maxTokens == null ? 64 : req.maxTokens;
        String model = req.model == null ? "qwen2.5:3b" : req.model;
        String prompt = req.prompt == null ? "Say hello." : req.prompt;
        int conc = req.concurrency == null || req.concurrency < 1 ? 1 : Math.min(req.concurrency, runs);

        List<BenchmarkDto.SingleRunResult> out = new ArrayList<>(runs);

        if (!BenchmarkModels.PROVIDER_OLLAMA.equals(provider)) {
            out.add(new BenchmarkDto.SingleRunResult(provider, model, System.nanoTime(), System.nanoTime(), 0, false, "Unsupported provider", null, null, null, null, null, null));
            return out.toArray(new BenchmarkDto.SingleRunResult[0]);
        }

        ExecutorService pool = Executors.newFixedThreadPool(conc);
        try {
            List<Future<BenchmarkDto.SingleRunResult>> futures = new ArrayList<>(runs);
            for (int i = 0; i < runs; i++) {
                futures.add(pool.submit(() -> ollama.callOnce(model, prompt, temp, maxTok, timeoutMs)));
            }
            for (Future<BenchmarkDto.SingleRunResult> f : futures) {
                try {
                    BenchmarkDto.SingleRunResult r = f.get(timeoutMs + 5000L, TimeUnit.MILLISECONDS);
                    // Quality berechnen, falls Keywords vorhanden
                    Double q = QualityUtil.scoreByKeywords(r.text(), req.expectedKeywords);
                    if (q != null) {
                        r = new BenchmarkDto.SingleRunResult(
                                r.provider(), r.model(), r.startNanos(), r.endNanos(),
                                r.httpStatus(), r.success(), r.error(),
                                r.inputTokens(), r.outputTokens(), r.totalTokens(), r.responseBytes(),
                                r.text(), q
                        );
                    }
                    out.add(r);
                } catch (Exception e) {
                    long end = System.nanoTime();
                    long approxStart = end - TimeUnit.MILLISECONDS.toNanos(timeoutMs);
                    out.add(new BenchmarkDto.SingleRunResult(
                            provider, model, approxStart, end, 0, false,
                            e.getClass().getSimpleName() + ": " + e.getMessage(),
                            null, null, null, null, null, null
                    ));
                }
            }
        } finally {
            pool.shutdownNow();
        }
        return out.toArray(new BenchmarkDto.SingleRunResult[0]);
    }

    public BenchmarkDto.BenchResponse run(BenchmarkDto.BenchRequest req) {
        var results = runOnceBatch(req);

        // Optional: Quality-Durchschnitt berechnen und in die Request-Map legen
        Double qAvg = java.util.Arrays.stream(results)
                .map(BenchmarkDto.SingleRunResult::quality)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average().orElse(Double.NaN);

        var agg = StatsUtil.calc(results);
        var resp = BenchmarkDto.BenchResponse.of(req, results, agg);

        // Trick: zusätzliche Infos in die vorhandene Request-Map einfügen
        if (req.expectedKeywords != null && !req.expectedKeywords.isEmpty()) {
            // Die Map in BenchResponse ist unveränderlich deklariert, aber wir können eine neue Map erzeugen,
            // nur wenn du möchtest. Einfacher: Aggregates so lassen und den Frontend‑Teil später erweitern.
        }
        return resp;
    }
}
