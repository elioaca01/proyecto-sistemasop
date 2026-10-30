package avilaos.sync;
import avilaos.process.*;
public record BufferOperationSnapshot(long processId, int bufferId, BufferPhase phase, boolean countingPermit,
        boolean mutexHeld, boolean modified, boolean networkStarted, long networkAvailableAt,
        int completedItems, BlockReason waitingReason) { }
