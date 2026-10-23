package avilaos.kernel;

import avilaos.hardware.*;
import avilaos.process.*;
import avilaos.scheduling.SchedulingPolicy;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.scheduling.SchedulingPolicies;
import avilaos.simulation.Tickable;
import avilaos.simulation.EventLog;
import avilaos.simulation.SimulationEvent;
import avilaos.structures.LinkedList;
import avilaos.structures.OrderedProcessQueue;
import avilaos.sync.BufferService;

/** Coordina un computador. Recibe ciclos consecutivos y es dueño de las transiciones. */
public final class Kernel implements Tickable {
    private final int computerId;
    private final Cpu cpu;
    private final Ram ram;
    private final IoController io;
    private final MemoryAdmission admission;
    private final Scheduler scheduler;
    private final BufferService buffers;
    private final EventLog events;
    private final LinkedList<ProcessControlBlock> incoming = new LinkedList<>();
    private final OrderedProcessQueue ready = new OrderedProcessQueue();
    private final LinkedList<ProcessControlBlock> blocked = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> terminated = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> registry = new LinkedList<>();
    private long nextCycle;
    private int quantum;
    private int sliceUsed;
    private record SchedulingChange(SchedulingPolicy policy, int quantum) { }
    private SchedulingChange pendingScheduling;

    public Kernel(int computerId, Cpu cpu, Ram ram, IoController io, SchedulingPolicy policy) {
        this(computerId, cpu, ram, io, policy, 2);
    }
    public Kernel(int computerId, Cpu cpu, Ram ram, IoController io, SchedulingPolicy policy, int quantum) {
        this(computerId, cpu, ram, io, policy, quantum, null);
    }
    public Kernel(int computerId, Cpu cpu, Ram ram, IoController io, SchedulingPolicy policy, int quantum, BufferService buffers) {
        this(computerId,cpu,ram,io,policy,quantum,buffers,null);
    }
    public Kernel(int computerId, Cpu cpu, Ram ram, IoController io, SchedulingPolicy policy, int quantum, BufferService buffers, EventLog events) {
        if (computerId <= 0 || cpu == null || ram == null || io == null || policy == null || quantum <= 0) {
            throw new IllegalArgumentException("Computador y componentes requeridos");
        }
        this.computerId = computerId;
        this.cpu = cpu;
        this.ram = ram;
        this.io = io;
        this.quantum = quantum;
        this.buffers = buffers; this.events = events;
        admission = new MemoryAdmission(ram);
        scheduler = new Scheduler(ready, policy);
    }
    public void submit(ProcessControlBlock process) {
        if (process == null || process.state() != ProcessState.NEW
                || process.definition().computerId() != computerId
                || process.arrivalCycle() != nextCycle
                || process.definition().memoryRequired() > ram.capacity()
                || (isBufferProcess(process) && buffers == null)) {
            throw new IllegalArgumentException("Proceso incompatible con este computador/ciclo");
        }
        for (int i = 0; i < registry.size(); i++) {
            if (registry.get(i).id() == process.id()) { throw new IllegalArgumentException("ID ya conocido"); }
        }
        if (isBufferProcess(process)) { buffers.registerProcess(process); }
        registry.addLast(process);
        incoming.addLast(process);
    }
    /** Solicita un cambio atómico para el próximo tick válido de este núcleo. */
    public void configureScheduling(SchedulingPolicyType type, int quantum) {
        if (quantum <= 0) { throw new IllegalArgumentException("Quantum positivo requerido"); }
        SchedulingPolicy policy = SchedulingPolicies.create(type);
        pendingScheduling = new SchedulingChange(policy, quantum);
    }
    @Override public void onTick(long cycle) {
        if (cycle < 0 || cycle != nextCycle || cycle == Long.MAX_VALUE) {
            throw new IllegalArgumentException("Ciclo del núcleo fuera de secuencia");
        }
        if (pendingScheduling != null) {
            scheduler.setPolicy(pendingScheduling.policy());
            quantum = pendingScheduling.quantum();
            sliceUsed = 0;
            pendingScheduling = null;
            log(cycle,null,"Cambia planificación a " + scheduler.policyType() + "; quantum " + quantum);
        }
        io.onTick(cycle);
        ProcessControlBlock completed;
        while ((completed = io.pollCompleted()) != null) {
            if (completed.remainingInstructions() == 0) {
                finish(completed, TerminationReason.COMPLETED, cycle);
            } else {
                removeIdentity(blocked, completed);
                completed.unblock();
                log(cycle,completed,"Se desbloquea de IO");
                ready.addLast(completed);
            }
        }
        admitWaiting();
        scheduler.reorder();
        if (scheduler.shouldPreempt(cpu.current(), sliceUsed, quantum)) {
            ProcessControlBlock preempted = cpu.current();
            preempted.preempt();
            log(cycle,preempted,"Se apropia CPU; vuelve a listos");
            ready.addLast(preempted);
            cpu.clear();
            sliceUsed = 0;
            scheduler.reorder();
        }
        if (cpu.current() != null && isBufferProcess(cpu.current()) && buffers.prepare(cpu.current(), cycle) == BlockReason.NETWORK) {
            blockRunning(cpu.current(), BlockReason.NETWORK);
        }
        while (cpu.current() == null) {
            ProcessControlBlock selected = scheduler.select();
            if (selected == null) { break; }
            if (isBufferProcess(selected) && buffers.prepare(selected, cycle) == BlockReason.NETWORK) {
                selected.blockBeforeDispatch(BlockReason.NETWORK); blocked.addLast(selected);
                log(cycle,selected,"Acceso remoto al buffer B" + selected.definition().bufferId() + "; se bloquea en NETWORK");
            } else { selected.dispatch(cycle); cpu.load(selected); sliceUsed = 0; log(cycle,selected,"Computador " + computerId + " selecciona P" + selected.id() + " con " + scheduler.policyType()); }
        }
        cpu.onTick(cycle);
        ProcessControlBlock running = cpu.current();
        if (running != null) {
            sliceUsed++;
            BlockReason bufferBlock = isBufferProcess(running) ? buffers.afterInstruction(running, cycle) : BlockReason.NONE;
            if (cpu.lastInstruction() == InstructionKind.BUFFER_INSERT) { log(cycle,running,"Se inserta un elemento en B" + running.definition().bufferId()); }
            if (cpu.lastInstruction() == InstructionKind.BUFFER_REMOVE) { log(cycle,running,"Se extrae un elemento de B" + running.definition().bufferId()); }
            if (bufferBlock != BlockReason.NONE) {
                blockRunning(running, bufferBlock);
            } else if (cpu.lastInstruction() == InstructionKind.IO_REQUEST) {
                io.request(running, cycle, running.definition().ioCycles());
                running.block(BlockReason.IO);
                log(cycle,running,"Se bloquea en IO");
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
                log(nextCycle,process,"Se admite en RAM y pasa a listos");
            } else { i++; }
        }
    }
    private void finish(ProcessControlBlock process, TerminationReason reason, long cycle) {
        process.terminate(reason, cycle);
        log(cycle,process,"P" + process.id() + " termina: " + reason);
        if (isBufferProcess(process)) { buffers.cleanup(process, cycle, reason); }
        if (cpu.current() == process) { cpu.clear(); sliceUsed = 0; }
        removeIdentity(incoming, process);
        ready.remove(process.id());
        removeIdentity(blocked, process);
        io.cancel(process);
        admission.releaseIfAllocated(process);
        terminated.addLast(process);
    }
    private static boolean isBufferProcess(ProcessControlBlock p) {
        return p.definition().type() == ProcessType.PRODUCER || p.definition().type() == ProcessType.CONSUMER;
    }
    private void blockRunning(ProcessControlBlock p, BlockReason reason) {
        p.block(reason); blocked.addLast(p); cpu.clear(); sliceUsed = 0;
        log(nextCycle,p,(reason==BlockReason.NETWORK ? "Acceso remoto al buffer B" + p.definition().bufferId() + "; " : "") + "Se bloquea en " + reason + (isBufferProcess(p)?" del buffer B"+p.definition().bufferId():""));
    }
    /** Solo el coordinador entrega avisos, entre ciclos y fuera del mutex compartido. */
    public boolean unblockExternal(ProcessControlBlock p, BlockReason reason) {
        if (p == null || reason == null || reason == BlockReason.NONE || reason == BlockReason.IO
                || p.state() != ProcessState.BLOCKED || p.blockReason() != reason) { return false; }
        for (int i = 0; i < blocked.size(); i++) {
            if (blocked.get(i) == p) {
                blocked.removeAt(i); p.unblock(); ready.addLast(p); log(nextCycle,p,"Se desbloquea de " + reason); return true;
            }
        }
        return false;
    }
    private static void removeIdentity(LinkedList<ProcessControlBlock> queue, ProcessControlBlock process) {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i) == process) { queue.removeAt(i); return; }
        }
    }
    private void log(long cycle, ProcessControlBlock p, String message) {
        if(events!=null) events.append(new SimulationEvent(cycle,computerId,p==null?-1:p.id(),message));
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
