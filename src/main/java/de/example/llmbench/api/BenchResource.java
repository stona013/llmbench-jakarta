package de.example.llmbench.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST-Resource für Benchmark-Anfragen.
 * 
 * Stellt Endpunkte bereit, um Benchmarks gegen LLM-Modelle auszuführen
 * und die Ergebnisse als JSON oder CSV abzurufen.
 */
@Path("/bench")
public class BenchResource {

    // Service-Instanz für die Benchmark-Logik
    private final BenchmarkService service = new BenchmarkService();

    /**
     * Führt einen Benchmark-Lauf aus und liefert das Ergebnis als JSON.
     * 
     * @param req Benchmark-Anfrage (wird aus JSON geparst)
     * @return Antwort mit Einzelergebnissen und Statistiken
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BenchmarkDto.BenchResponse run(BenchmarkDto.BenchRequest req) {
        if (req == null) req = new BenchmarkDto.BenchRequest();
        return service.run(req);
    }

    /**
     * Führt einen Benchmark-Lauf aus und liefert das Ergebnis als CSV-Datei.
     * 
     * @param req Benchmark-Anfrage (wird aus JSON geparst)
     * @return CSV-Datei mit den wichtigsten Metriken aller Durchläufe
     */
    @POST
    @Path("/csv")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("text/csv")
    public Response runCsv(BenchmarkDto.BenchRequest req) {
        if (req == null) req = new BenchmarkDto.BenchRequest();
        var res = service.run(req);
        StringBuilder sb = new StringBuilder();
        // CSV-Header
        sb.append("provider,model,httpStatus,success,durationMs,inputTokens,outputTokens,totalTokens,responseBytes\n");
        for (var r : res.results()) {
            double ms = (r.endNanos() - r.startNanos()) / 1_000_000.0;
            sb.append(r.provider()).append(',')
              .append(escape(res.request().get("model"))).append(',')
              .append(r.httpStatus()).append(',')
              .append(r.success()).append(',')
              .append(String.format(java.util.Locale.US,"%.1f", ms)).append(',')
              .append(n(r.inputTokens())).append(',')
              .append(n(r.outputTokens())).append(',')
              .append(n(r.totalTokens())).append(',')
              .append(n(r.responseBytes())).append('\n');
        }
        return Response.ok(sb.toString())
                .header("Content-Disposition", "attachment; filename=\"bench.csv\"")
                .build();
    }

    // Hilfsmethode zum Escapen von CSV-Feldern (ersetzt Kommas durch Leerzeichen)
    private static String escape(Object o){ return o==null?"":o.toString().replace(","," "); }

    // Hilfsmethode für Null-sichere Integer-Ausgabe
    private static String n(Integer i){ return i==null? "": i.toString(); }
}
