package avilaos.metrics;

/** Una muestra por ciclo completado. Ningún arreglo mutable sale del objeto. */
public final class UtilizationSample {
    private final long cycle;
    private final int[] ids;
    private final double[] utilizations;
    public UtilizationSample(long cycle, int[] computerIds, double[] utilizations) {
        if (cycle <= 0 || computerIds == null || utilizations == null || computerIds.length == 0
                || computerIds.length != utilizations.length) { throw new IllegalArgumentException("Muestra inválida"); }
        this.cycle = cycle; ids = computerIds.clone(); this.utilizations = utilizations.clone();
        for (int i = 0; i < ids.length; i++) {
            if (ids[i] <= 0 || !Double.isFinite(this.utilizations[i]) || this.utilizations[i] < 0 || this.utilizations[i] > 1) {
                throw new IllegalArgumentException("ID o utilización inválida");
            }
            for (int j = 0; j < i; j++) { if (ids[i] == ids[j]) { throw new IllegalArgumentException("ID repetido"); } }
        }
    }
    public long cycle() { return cycle; }
    public int computerCount() { return ids.length; }
    public int computerIdAt(int index) { return ids[index]; }
    public double utilizationAt(int index) { return utilizations[index]; }
}
