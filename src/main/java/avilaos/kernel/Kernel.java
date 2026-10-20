package avilaos.kernel;

import avilaos.hardware.*;
import avilaos.process.*;
import avilaos.scheduling.SchedulingPolicy;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.simulation.Tickable;
import avilaos.structures.LinkedList;
import avilaos.structures.OrderedProcessQueue;

/** Coordina un computador. Recibe ciclos consecutivos y es dueño de las transiciones. */
public final class Kernel implements Tickable {
    private final int computerId;
    private final Cpu cpu;
    private final Ram ram;
    private final IoController io;
    private final MemoryAdmission admission;
    private final Scheduler scheduler;
    private final LinkedList<ProcessControlBlock> incoming = new LinkedList<>();
    private final OrderedProcessQueue ready = new OrderedProcessQueue();
    private final LinkedList<ProcessControlBlock> blocked = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> terminated = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> registry = new LinkedList<>();
    private long nextCycle;
    private int quantum;
    private int sliceUsed;

    public Kernel(int computerId, Cpu cpu, Ram ram, IoController io, SchedulingPolicy policy) {
        this(computerId, cpu, ram, io, policy, 2);
    }
    public Kernel(int computerId, Cpu cpu, Ram ram, IoController io, SchedulingPolicy policy, int quantum) {
        if (computerId <= 0 || cpu == null || ram == null || io == null || policy == null || quantum <= 0) {
            throw new IllegalArgumentException("Computador y componentes requeridos");
        }
        this.computerId = computerId;
        this.cpu = cpu;
        this.ram = ram;
        this.io = io;
        this.quantum = quantum;
        admission = new MemoryAdmission(ram);
        scheduler = new Scheduler(ready, policy);
    }
    public void submit(ProcessControlBlock process) {
        if (process == null || process.state() != ProcessState.NEW
                || process.definition().computerId() != computerId
                || process.arrivalCycle() != nextCycle
                || process.definition().memoryRequired() > ram.capacity()
                || (process.definition().type() != ProcessType.CPU_BOUND
                    && process.definition().type() != ProcessType.IO_BOUND)) {
            throw new IllegalArgumentException("Proceso incompatible con este computador/ciclo");
        }
        for (int i = 0; i < registry.size(); i++) {
            if (registry.get(i).id() == process.id()) { throw new IllegalArgumentException("ID ya conocido"); }
        }
        registry.addLast(process);
        incoming.addLast(process);
    }
    @Override public void onTick(long cycle) {
        if (cycle < 0 || cycle != nextCycle || cycle == Long.MAX_VALUE) {
            throw new IllegalArgumentException("Ciclo del núcleo fuera de secuencia");
        }
        io.onTick(cycle);
        ProcessControlBlock completed;
        while ((completed = io.pollCompleted()) != null) {
            if (completed.remainingInstructions() == 0) {
                finish(completed, TerminationReason.COMPLETED, cycle);
            } else {
                removeIdentity(blocked, completed);
                completed.unblock();
                ready.addLast(completed);
            }
        }
        admitWaiting();
        scheduler.reorder();
        if (scheduler.shouldPreempt(cpu.current(), sliceUsed, quantum)) {
            ProcessControlBlock preempted = cpu.current();
            preempted.preempt();
            ready.addLast(preempted);
            cpu.clear();
            sliceUsed = 0;
            scheduler.reorder();
        }
        if (cpu.current() == null) {
            ProcessControlBlock selected = scheduler.select();
            if (selected != null) { selected.dispatch(cycle); cpu.load(selected); sliceUsed = 0; }
        }
        cpu.onTick(cycle);
        ProcessControlBlock running = cpu.current();
        if (running != null) {
            sliceUsed++;
            if (cpu.lastInstruction() == InstructionKind.IO_REQUEST) {
                io.request(running, cycle, running.definition().ioCycles());
                running.block(BlockReason.IO);
                blocked.addLast(running);
                cpu.clear();
                sliceUsed = 0;
            } else if (running.remainingInstructions() == 0) {
                finish(running, TerminationReason.COMPLETED, cycle);
            }
        }
        for (int i = 0; i < registry.size(); i++) {
            ProcessControlBlock process = registry.get(i);
            if (process.state() != ProcessState.TERMINATED && process.deadlineRemaining(cycle + 1) == 0) {
                finish(process, TerminationReason.DEADLINE_EXPIRED, cycle);
            }
        }
        // La admisión al cierre no concede una segunda instrucción en el mismo ciclo.
        admitWaiting();
        scheduler.reorder();
        nextCycle++;
    }
    private void admitWaiting() {
        for (int i = 0; i < incoming.size();) {
            ProcessControlBlock process = incoming.get(i);
            if (admission.admit(process)) {
                incoming.removeAt(i);
                ready.addLast(process);
            } else { i++; }
        }
    }
    private void finish(ProcessControlBlock process, TerminationReason reason, long cycle) {
        process.terminate(reason, cycle);
        if (cpu.current() == process) { cpu.clear(); sliceUsed = 0; }
        removeIdentity(incoming, process);
        ready.remove(process.id());
        removeIdentity(blocked, process);
        io.cancel(process);
        admission.releaseIfAllocated(process);
        terminated.addLast(process);
    }
    private static void removeIdentity(LinkedList<ProcessControlBlock> queue, ProcessControlBlock process) {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i) == process) { queue.removeAt(i); return; }
        }
    }
    public int quantum() { return quantum; }
    public int sliceUsed() { return sliceUsed; }
    public SchedulingPolicyType policyType() { return scheduler.policyType(); }
    public long nextCycle() { return nextCycle; }
    public int newCount() { return incoming.size(); }
    public int readyCount() { return ready.size(); }
    public int blockedCount() { return blocked.size(); }
    public int terminatedCount() { return terminated.size(); }
    public ProcessControlBlock newAt(int index) { return incoming.get(index); }
    public ProcessControlBlock readyAt(int index) { return ready.get(index); }
    public ProcessControlBlock blockedAt(int index) { return blocked.get(index); }
    public ProcessControlBlock terminatedAt(int index) { return terminated.get(index); }
}
