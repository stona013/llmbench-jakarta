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

@Path("/models")
public class ModelsResource {

    private static final Pattern NAME_PATTERN =
            Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

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
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

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
