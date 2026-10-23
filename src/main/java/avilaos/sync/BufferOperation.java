package avilaos.sync;
import avilaos.process.*;
/** Estado privado de recursos de una única operación activa por PCB. */
final class BufferOperation {
    final ProcessControlBlock process; final BoundedBuffer buffer;
    boolean countingPermit, mutexHeld, modified, networkStarted;
    long networkAvailableAt = -1;
    int completedItems, lastPc;
    BlockReason waiting = BlockReason.NONE;
    BufferOperation(ProcessControlBlock process, BoundedBuffer buffer) { this.process = process; this.buffer = buffer; }
    boolean producer() { return process.definition().type() == ProcessType.PRODUCER; }
    BufferOperationSnapshot snapshot() {
        BufferPhase phase = switch (waiting) {
            case SEMAPHORE_EMPTY -> BufferPhase.WAIT_EMPTY;
            case SEMAPHORE_FULL -> BufferPhase.WAIT_FULL;
            case SEMAPHORE_MUTEX -> BufferPhase.WAIT_MUTEX;
            default -> BufferProgram.phaseAt(process.definition(), process.pc());
        };
        return new BufferOperationSnapshot(process.id(), buffer.definition().id(), phase, countingPermit, mutexHeld,
                modified, networkStarted, networkAvailableAt, completedItems, waiting);
    }
}
