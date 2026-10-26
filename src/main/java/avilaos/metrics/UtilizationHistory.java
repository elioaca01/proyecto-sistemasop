package avilaos.metrics;

import avilaos.structures.LinkedList;

/** Solo escribe el coordinador después de confirmar todos los computadores. */
public final class UtilizationHistory {
    private final LinkedList<UtilizationSample> samples = new LinkedList<>();
    private long lastCycle;
    public void record(MetricsSnapshot metrics) {
        if (metrics == null || metrics.computerCount() == 0 || lastCycle == Long.MAX_VALUE
                || metrics.global().cycles() != lastCycle + 1) { throw new IllegalArgumentException("Muestra fuera de secuencia"); }
        int[] ids = new int[metrics.computerCount()]; double[] values = new double[ids.length];
        for (int i = 0; i < ids.length; i++) { ids[i] = metrics.computerIdAt(i); values[i] = metrics.computerAt(i).utilization(); }
        samples.addLast(new UtilizationSample(lastCycle + 1, ids, values)); lastCycle++;
    }
    public UtilizationSample[] copy() {
        UtilizationSample[] result = new UtilizationSample[samples.size()]; samples.copyInto(result); return result;
    }
}
