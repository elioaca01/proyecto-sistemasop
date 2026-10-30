package avilaos.metrics;

/** Contadores base inmutables. NaN significa que no existe el denominador. */
public record PerformanceMetrics(long cycles, int processors, long userCycles, long kernelCycles,
        long completed, long expired, long pending, long responded, double responseCycles,
        long admitted, double serviceSquared) {
    public PerformanceMetrics {
        if (cycles < 0 || processors < 0 || userCycles < 0 || kernelCycles < 0 || completed < 0
                || expired < 0 || pending < 0 || responded < 0 || admitted < 0
                || !Double.isFinite(responseCycles) || responseCycles < 0
                || !Double.isFinite(serviceSquared) || serviceSquared < 0) {
            throw new IllegalArgumentException("Contadores de rendimiento inválidos");
        }
    }
    private static double ratio(double numerator, double denominator) {
        return denominator == 0 ? Double.NaN : numerator / denominator;
    }
    public double throughput() { return ratio(completed, cycles); }
    public double utilization() { return ratio((double) userCycles + kernelCycles, (double) processors * cycles); }
    public double userUtilization() { return ratio(userCycles, (double) processors * cycles); }
    public double kernelUtilization() { return ratio(kernelCycles, (double) processors * cycles); }
    public double averageResponse() { return ratio(responseCycles, responded); }
    public double deadlineCompliance() { return ratio(completed, (double) completed + expired); }
    public double fairness() {
        double service = (double) userCycles + kernelCycles;
        return ratio(service * service, admitted * serviceSquared);
    }
}
