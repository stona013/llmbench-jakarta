package de.example.llmbench.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REST-Resource zum Auflisten verfügbarer LLM-Modelle vom Ollama-Server.
 * 
 * Fragt die Ollama-API ab und gibt die Modellnamen sowie Metadaten als JSON zurück.
 * Fehler werden im Ergebnisobjekt als Feld "error" ausgegeben, niemals als Exception.
 */
@Path("/models")
public class ModelsResource {

    // Regex-Pattern zum Extrahieren von Modellnamen aus der Ollama-API-Antwort
    private static final Pattern NAME_PATTERN =
            Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * Listet alle verfügbaren Modelle vom Ollama-Server auf.
     * 
     * @return Map mit Basis-URL, Modellnamen, Status und ggf. Fehlerdetails
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> list() {
        String base = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://ollama:11434");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("baseUrl", base);

        List<String> models = new ArrayList<>();
        String raw = null;
        String error = null;

        try {
            // HTTP-Client mit Timeout
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            // Anfrage an die Ollama-API
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/api/tags"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            int status = resp.statusCode();
            raw = new String(resp.body(), StandardCharsets.UTF_8);
            out.put("upstreamStatus", status);

            if (status >= 200 && status < 300) {
                // Modellnamen extrahieren
                Matcher m = NAME_PATTERN.matcher(raw);
                while (m.find()) {
                    models.add(m.group(1));
                }
                // Deduplizieren und sortieren
                LinkedHashSet<String> set = new LinkedHashSet<>(models);
                models = new ArrayList<>(set);
                Collections.sort(models, String.CASE_INSENSITIVE_ORDER);
            } else {
                error = "Upstream returned status " + status;
            }
        } catch (Exception ex) {
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }

        out.put("count", models.size());
        out.put("models", models);
        out.put("error", error);
        if (error != null) out.put("raw", raw); // damit man im Zweifel sieht, was kam
        return out; // niemals Exception werfen → kein 500 mehr
    }
}
