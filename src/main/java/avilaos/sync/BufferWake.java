package avilaos.sync;
import avilaos.process.*;
/** Aviso interno; el Kernel de destino conserva la propiedad de las transiciones. */
public record BufferWake(ProcessControlBlock process, BlockReason reason, long availableCycle) { }
