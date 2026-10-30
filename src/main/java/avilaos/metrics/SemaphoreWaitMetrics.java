package avilaos.metrics;

/** El promedio usa solo episodios cerrados; los abiertos se muestran aparte. */
public record SemaphoreWaitMetrics(long completedEpisodes, long completedCycles, long openEpisodes, long openCycles) {
    public SemaphoreWaitMetrics {
        if (completedEpisodes < 0 || completedCycles < 0 || openEpisodes < 0 || openCycles < 0) {
            throw new IllegalArgumentException("Contadores de espera inválidos");
        }
    }
    public double averageWait() { return completedEpisodes == 0 ? Double.NaN : (double) completedCycles / completedEpisodes; }
    public static SemaphoreWaitMetrics combine(SemaphoreWaitMetrics[] values) {
        if (values == null) { throw new IllegalArgumentException("Esperas requeridas"); }
        long count = 0, cycles = 0, open = 0, openCycles = 0;
        for (SemaphoreWaitMetrics m : values) {
            if (m == null) { throw new IllegalArgumentException("Espera nula"); }
            count = Math.addExact(count, m.completedEpisodes()); cycles = Math.addExact(cycles, m.completedCycles());
            open = Math.addExact(open, m.openEpisodes()); openCycles = Math.addExact(openCycles, m.openCycles());
        }
        return new SemaphoreWaitMetrics(count, cycles, open, openCycles);
    }
}
