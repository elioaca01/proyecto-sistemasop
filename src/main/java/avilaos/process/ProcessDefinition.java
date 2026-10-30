package avilaos.process;

/** Atributos inmutables definidos al crear un proceso; no son su estado vivo. */
public record ProcessDefinition(String name, int computerId, ProcessType type,
                                int instructionCount, int memoryRequired, int priority,
                                long deadlineCycles, int cpuBurst, int ioCycles,
                                int bufferId, int operationInterval, int targetItems) {
    public ProcessDefinition {
        if (name == null || name.strip().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        name = name.strip();
        if (computerId <= 0 || type == null || instructionCount <= 0
                || memoryRequired <= 0 || priority < 0 || deadlineCycles <= 0) {
            throw new IllegalArgumentException("Atributos comunes del proceso inválidos");
        }
        switch (type) {
            case CPU_BOUND -> {
                if (cpuBurst != 0 || ioCycles != 0 || bufferId != 0
                        || operationInterval != 0 || targetItems != 0) {
                    throw new IllegalArgumentException("CPU bound no utiliza parámetros de E/S o buffer");
                }
            }
            case IO_BOUND -> {
                if (cpuBurst <= 0 || ioCycles <= 0 || bufferId != 0
                        || operationInterval != 0 || targetItems != 0) {
                    throw new IllegalArgumentException("I/O bound requiere ráfaga y duración de E/S positivas");
                }
            }
            case PRODUCER, CONSUMER -> {
                if (cpuBurst != 0 || ioCycles != 0 || bufferId <= 0
                        || operationInterval <= 0 || targetItems <= 0) {
                    throw new IllegalArgumentException("Productor/consumidor requiere buffer, intervalo y cuota");
                }
                // La suma y el producto se realizan en long antes de compararlos.
                long requiredInstructions = (operationInterval + 5L) * targetItems;
                if (instructionCount < requiredInstructions) {
                    throw new IllegalArgumentException("Instrucciones insuficientes para completar la cuota");
                }
            }
        }
    }
}
