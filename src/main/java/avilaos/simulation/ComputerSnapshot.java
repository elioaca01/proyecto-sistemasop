package avilaos.simulation;
import avilaos.hardware.CpuMode;
import avilaos.sync.BufferService;
import avilaos.process.ProcessControlBlock;
import avilaos.process.InstructionKind;
import avilaos.scheduling.SchedulingPolicyType;
/** Se captura solo cuando el propietario ha estabilizado el computador. */
public final class ComputerSnapshot {
    private final int id;
    private final long nextCycle;
    private final int ramCapacity;
    private final int ramUsed;
    private final int ramFree;
    private final CpuMode mode;
    private final InstructionKind lastInstruction;
    private final long lastProcessId;
    private final SchedulingPolicyType policyType;
    private final int quantum;
    private final int sliceUsed;
    private final ProcessSnapshot running;
    private final ProcessSnapshot[] ready;
    private final ProcessSnapshot[] blocked;
    private final ProcessSnapshot[] terminated;
    private final ProcessSnapshot[] incoming;
    private ComputerSnapshot(Computer c, BufferService buffers) {
        id = c.id();
        nextCycle = c.kernel().nextCycle();
        ramCapacity = c.ram().capacity();
        ramUsed = c.ram().used();
        ramFree = c.ram().free();
        mode = c.cpu().mode();
        lastInstruction = c.cpu().lastInstruction();
        lastProcessId = c.cpu().lastProcessId();
        policyType = c.policyType();
        quantum = c.quantum();
        sliceUsed = c.sliceUsed();
        running = c.cpu().current() == null ? null : captureProcess(buffers, c.cpu().current());
        incoming = new ProcessSnapshot[c.kernel().newCount()];
        for (int i = 0; i < incoming.length; i++) { incoming[i] = captureProcess(buffers, c.kernel().newAt(i)); }
        ready = new ProcessSnapshot[c.kernel().readyCount()];
        for (int i = 0; i < ready.length; i++) { ready[i] = captureProcess(buffers, c.kernel().readyAt(i)); }
        blocked = new ProcessSnapshot[c.kernel().blockedCount()];
        for (int i = 0; i < blocked.length; i++) { blocked[i] = captureProcess(buffers, c.kernel().blockedAt(i)); }
        terminated = new ProcessSnapshot[c.kernel().terminatedCount()];
        for (int i = 0; i < terminated.length; i++) { terminated[i] = captureProcess(buffers, c.kernel().terminatedAt(i)); }
    }
    public static ComputerSnapshot capture(Computer c) {
        return capture(c, null);
    }
    public static ComputerSnapshot capture(Computer c, BufferService buffers) {
        if (c == null) { throw new IllegalArgumentException("Computador requerido"); }
        return new ComputerSnapshot(c, buffers);
    }
    private static ProcessSnapshot captureProcess(BufferService buffers, ProcessControlBlock p) {
        return ProcessSnapshot.capture(p, buffers == null ? null : buffers.operationSnapshot(p));
    }
    public int id() { return id; }
    public long nextCycle() { return nextCycle; }
    public int ramCapacity() { return ramCapacity; }
    public int ramUsed() { return ramUsed; }
    public int ramFree() { return ramFree; }
    public CpuMode mode() { return mode; }
    public InstructionKind lastInstruction() { return lastInstruction; }
    public long lastProcessId() { return lastProcessId; }
    public SchedulingPolicyType policyType() { return policyType; }
    public int quantum() { return quantum; }
    public int sliceUsed() { return sliceUsed; }
    public ProcessSnapshot running() { return running; }
    public int newCount() { return incoming.length; }
    public ProcessSnapshot newAt(int index) { return incoming[index]; }
    public int readyCount() { return ready.length; }
    public ProcessSnapshot readyAt(int index) { return ready[index]; }
    public int blockedCount() { return blocked.length; }
    public ProcessSnapshot blockedAt(int index) { return blocked[index]; }
    public int terminatedCount() { return terminated.length; }
    public ProcessSnapshot terminatedAt(int index) { return terminated[index]; }
}
