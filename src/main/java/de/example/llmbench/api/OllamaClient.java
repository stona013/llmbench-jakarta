package de.example.llmbench.api;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client für den Zugriff auf die Ollama-API.
 * 
 * Stellt Methoden bereit, um ein Modell mit einem Prompt anzusprechen und
 * relevante Metriken sowie den Antworttext zu extrahieren.
 */
public class OllamaClient {

    // HTTP-Client für Anfragen an Ollama
    private final HttpClient http = HttpClient.newBuilder().build();
    // Basis-URL für Ollama-API (über Umgebungsvariable konfigurierbar)
    private final String baseUrl = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");

    // Regex-Patterns zum Extrahieren von Zahlen und Antworttext aus der JSON-Antwort
    private static final Pattern P_INT_PROMPT = Pattern.compile("\"prompt_eval_count\"\\s*:\\s*(\\d+)");
    private static final Pattern P_INT_OUTPUT = Pattern.compile("\"eval_count\"\\s*:\\s*(\\d+)");
    private static final Pattern P_STR_RESPONSE = Pattern.compile("\"response\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);

    /**
     * Führt einen einzelnen Aufruf gegen die Ollama-API durch.
     * 
     * @param model Modellname
     * @param prompt Eingabetext
     * @param temperature Sampling-Temperatur
     * @param maxTokens Maximale Token-Anzahl
     * @param timeoutMs Timeout in Millisekunden
     * @return Ergebnisobjekt mit Metriken und Antworttext
     */
    public BenchmarkDto.SingleRunResult callOnce(String model, String prompt, double temperature, int maxTokens, int timeoutMs) {

        long start = System.nanoTime();
        int status = 0;
        boolean ok = false;
        String err = null;
        Integer promptTok = null, outputTok = null, totalTok = null, respBytes = null;
        String answerText = null;

        try {
            // JSON-Body für die Anfrage erzeugen
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

            // Token-Anzahlen und Antworttext extrahieren
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

    /**
     * Hilfsmethode zum Serialisieren eines Strings als JSON-String (mit Escaping).
     */
    private static String jsonString(String s) {
        String e = s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
        return "\"" + e + "\"";
    }

    /**
     * Extrahiert eine Ganzzahl aus einem String mittels Regex.
     */
    private static Integer findInt(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    /**
     * Extrahiert einen String aus einem JSON-Feld mittels Regex und unescaped gängige Sequenzen.
     */
    private static String findString(Pattern p, String s) {
        Matcher m = p.matcher(s);
        if (!m.find()) return null;
        String raw = m.group(1);
        // einfache Unescape-Behandlung für gängigste Sequenzen
        return raw.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
