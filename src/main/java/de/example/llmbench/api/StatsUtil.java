package de.example.llmbench.api;

import java.util.Arrays;

public final class StatsUtil {
    private StatsUtil(){}

  public static BenchmarkDto.Aggregates calc(BenchmarkDto.SingleRunResult[] arr) {
    double[] d = Arrays.stream(arr)
            .mapToDouble(r -> (r.endNanos() - r.startNanos()) / 1_000_000.0)
            .filter(ms -> ms > 0)        // nicht nach success filtern
            .sorted()
            .toArray();

    if (d.length == 0) {
        return new BenchmarkDto.Aggregates(arr.length, 0, 0, 0, 0, 0, 0);
    }
    return new BenchmarkDto.Aggregates(
            arr.length,
            avg(d),
            d[0],
            d[d.length - 1],
            perc(d, 0.50),
            perc(d, 0.90),
            perc(d, 0.95)
    );
}


    private static double avg(double[] d){ double s=0; for(double x:d) s+=x; return s/d.length; }
    private static double perc(double[] d, double p){
        if (d.length == 0) return 0;
        int i = (int)Math.floor(p*(d.length-1));
        return d[Math.max(0, Math.min(i, d.length-1))];
    }
}
