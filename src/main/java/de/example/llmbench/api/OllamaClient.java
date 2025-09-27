package de.example.llmbench.api;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OllamaClient {

    private final HttpClient http = HttpClient.newBuilder().build();
    private final String baseUrl = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");

    private static final Pattern P_INT_PROMPT = Pattern.compile("\"prompt_eval_count\"\\s*:\\s*(\\d+)");
    private static final Pattern P_INT_OUTPUT = Pattern.compile("\"eval_count\"\\s*:\\s*(\\d+)");
    private static final Pattern P_STR_RESPONSE = Pattern.compile("\"response\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);

    public BenchmarkDto.SingleRunResult callOnce(String model, String prompt, double temperature, int maxTokens, int timeoutMs) {

        long start = System.nanoTime();
        int status = 0;
        boolean ok = false;
        String err = null;
        Integer promptTok = null, outputTok = null, totalTok = null, respBytes = null;
        String answerText = null;

        try {
            String body = """
                {
                  "model": %s,
                  "prompt": %s,
                  "stream": false,
                  "options": { "temperature": %s, "num_predict": %s }
                }
                """.formatted(
                    jsonString(model),
                    jsonString(prompt == null ? "" : prompt),
                    Double.toString(temperature),
                    Integer.toString(maxTokens <= 0 ? 1 : maxTokens)
                );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .timeout(Duration.ofMillis(Math.max(1000, timeoutMs)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            status = resp.statusCode();
            String s = resp.body() == null ? "" : new String(resp.body(), StandardCharsets.UTF_8);
            respBytes = resp.body() == null ? 0 : resp.body().length;

            promptTok = findInt(P_INT_PROMPT, s);
            outputTok = findInt(P_INT_OUTPUT, s);
            totalTok  = (promptTok != null && outputTok != null) ? promptTok + outputTok : null;

            // Antworttext extrahieren, falls vorhanden
            answerText = findString(P_STR_RESPONSE, s);

            ok = status >= 200 && status < 300;
            if (!ok) err = s;

        } catch (Exception ex) {
            err = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }

        long end = System.nanoTime();
        return new BenchmarkDto.SingleRunResult(
                BenchmarkModels.PROVIDER_OLLAMA, model, start, end,
                status, ok, err, promptTok, outputTok, totalTok, respBytes,
                answerText,  // text
                null         // quality noch nicht gesetzt
        );
    }

    private static String jsonString(String s) {
        String e = s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
        return "\"" + e + "\"";
    }

    private static Integer findInt(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private static String findString(Pattern p, String s) {
        Matcher m = p.matcher(s);
        if (!m.find()) return null;
        String raw = m.group(1);
        // einfache Unescape-Behandlung für gängigste Sequenzen
        return raw.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
