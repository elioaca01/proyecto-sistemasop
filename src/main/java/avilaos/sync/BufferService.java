package avilaos.sync;

import avilaos.process.*;
import avilaos.simulation.SimulationEvent;
import avilaos.structures.LinkedList;
import avilaos.structures.LinkedQueue;
import java.util.concurrent.Semaphore;

/** Recursos compartidos. Nunca invoca al Kernel ni espera workers sosteniendo el mutex. */
public final class BufferService {
    private final BufferRegistry registry;
    private final Semaphore mutex = new Semaphore(1);
    private final LinkedList<BufferOperation> operations = new LinkedList<>();
    private final LinkedList<BufferWake> wakes = new LinkedList<>();
    private final LinkedList<SimulationEvent> events = new LinkedList<>();
    private long nextCycle, nextItemId = 1;

    public BufferService(BufferRegistry registry) {
        if (registry == null) { throw new IllegalArgumentException("Registro requerido"); }
        this.registry = registry;
    }
    private void validateDefinition(ProcessDefinition definition) {
        if (definition == null || (definition.type() != ProcessType.PRODUCER && definition.type() != ProcessType.CONSUMER)) {
            throw new IllegalArgumentException("Productor/consumidor requerido");
        }
        registry.get(definition.bufferId());
    }
    public void validate(ProcessDefinition definition) {
        mutex.acquireUninterruptibly();
        try { validateDefinition(definition); } finally { mutex.release(); }
    }
    public void registerProcess(ProcessControlBlock p) {
        mutex.acquireUninterruptibly();
        try {
            if (p == null || p.state() != ProcessState.NEW) { throw new IllegalArgumentException("PCB NEW requerido"); }
            validateDefinition(p.definition());
            for (int i = 0; i < operations.size(); i++) {
                if (operations.get(i).process.id() == p.id()) { throw new IllegalArgumentException("Operación duplicada"); }
            }
            operations.addLast(new BufferOperation(p, registry.get(p.definition().bufferId())));
        } finally { mutex.release(); }
    }
    private BufferOperation find(ProcessControlBlock p) {
        for (int i = 0; i < operations.size(); i++) { if (operations.get(i).process == p) { return operations.get(i); } }
        return null;
    }
    private BufferOperation require(ProcessControlBlock p) {
        BufferOperation op = find(p);
        if (op == null) { throw new IllegalArgumentException("Operación no registrada"); }
        return op;
    }
    private void requireCycle(long cycle) {
        if (nextCycle == 0 || cycle < 0 || cycle != nextCycle - 1) { throw new IllegalArgumentException("Ciclo de recursos inválido"); }
    }
    public BlockReason prepare(ProcessControlBlock p, long cycle) {
        mutex.acquireUninterruptibly();
        try { requireCycle(cycle); require(p); return BlockReason.NONE; }
        finally { mutex.release(); }
    }
    public BlockReason afterInstruction(ProcessControlBlock p, long cycle) {
        mutex.acquireUninterruptibly();
        try {
            requireCycle(cycle); BufferOperation op = require(p);
            if (p.state() != ProcessState.RUNNING || p.pc() != op.lastPc + 1 || op.waiting != BlockReason.NONE) {
                throw new IllegalStateException("Efecto repetido o proceso no ejecutable");
            }
            BufferPhase phase = BufferProgram.phaseAt(p.definition(), p.pc() - 1);
            switch (phase) {
                case COMPUTE -> { }
                case WAIT_EMPTY, WAIT_FULL -> {
                    if (op.countingPermit || op.modified || op.mutexHeld) { throw new IllegalStateException("Reserva de conteo duplicada"); }
                    SimulatedSemaphore sem = phase == BufferPhase.WAIT_EMPTY ? op.buffer.empty() : op.buffer.full();
                    if (sem.waitFor(p)) { op.countingPermit = true; }
                    else { op.waiting = phase == BufferPhase.WAIT_EMPTY ? BlockReason.SEMAPHORE_EMPTY : BlockReason.SEMAPHORE_FULL; }
                }
                case WAIT_MUTEX -> {
                    if (!op.countingPermit || op.mutexHeld) { throw new IllegalStateException("Conteo previo requerido"); }
                    if (op.buffer.mutex().waitFor(p)) { op.buffer.claimMutex(p); op.mutexHeld = true; }
                    else { op.waiting = BlockReason.SEMAPHORE_MUTEX; }
                }
                case INSERT, REMOVE -> {
                    if (!op.countingPermit || !op.mutexHeld || op.modified) { throw new IllegalStateException("Reservas incompletas"); }
                    if (phase == BufferPhase.INSERT) {
                        long following = Math.incrementExact(nextItemId);
                        op.buffer.insert(p, new BufferItem(nextItemId, p.id(), p.definition().computerId())); nextItemId = following;
                    } else { op.buffer.remove(p); }
                    op.countingPermit = false; op.modified = true;
                }
                case SIGNAL_MUTEX -> { releaseMutex(op, cycle); }
                case SIGNAL_FULL, SIGNAL_EMPTY -> {
                    if (!op.modified || op.mutexHeld) { throw new IllegalStateException("Modificación no disponible para señalizar"); }
                    op.modified = false;
                    grantCount((phase == BufferPhase.SIGNAL_FULL ? op.buffer.full() : op.buffer.empty()).signal(), cycle);
                    op.completedItems++; op.networkStarted = false; op.networkAvailableAt = -1;
                }
                default -> throw new IllegalStateException("Fase no ejecutable");
            }
            op.lastPc = p.pc(); return op.waiting;
        } finally { mutex.release(); }
    }
    private void releaseMutex(BufferOperation op, long cycle) {
        if (!op.mutexHeld) { throw new IllegalStateException("Mutex no adquirido"); }
        ProcessControlBlock granted = op.buffer.releaseMutex(op.process); op.mutexHeld = false;
        if (granted != null) {
            BufferOperation recipient = require(granted); recipient.mutexHeld = true;
            schedule(recipient, BlockReason.SEMAPHORE_MUTEX, cycle);
        }
    }
    private void grantCount(ProcessControlBlock granted, long cycle) {
        if (granted != null) {
            BufferOperation recipient = require(granted); recipient.countingPermit = true;
            schedule(recipient, recipient.producer() ? BlockReason.SEMAPHORE_EMPTY : BlockReason.SEMAPHORE_FULL, cycle);
        }
    }
    private void schedule(BufferOperation op, BlockReason reason, long cycle) {
        if (op.waiting != reason) { throw new IllegalStateException("Concesión sin espera correspondiente"); }
        op.waiting = BlockReason.NONE;
        wakes.addLast(new BufferWake(op.process, reason, Math.addExact(cycle, 1)));
    }
    public LinkedQueue<BufferWake> beginCycle(long cycle) {
        mutex.acquireUninterruptibly();
        try {
            if (cycle < 0 || cycle != nextCycle || cycle == Long.MAX_VALUE) { throw new IllegalArgumentException("Ciclo global fuera de secuencia"); }
            LinkedQueue<BufferWake> due = new LinkedQueue<>();
            for (int i = 0; i < wakes.size();) {
                if (wakes.get(i).availableCycle() <= cycle) { due.enqueue(wakes.removeAt(i)); } else { i++; }
            }
            nextCycle++; return due;
        } finally { mutex.release(); }
    }
    public void cleanup(ProcessControlBlock p, long cycle, TerminationReason reason) {
        mutex.acquireUninterruptibly();
        try {
            requireCycle(cycle);
            if (p == null || reason == null || p.state() != ProcessState.TERMINATED) { throw new IllegalArgumentException("Terminación requerida"); }
            BufferOperation op = find(p); if (op == null) { return; }
            op.buffer.empty().cancelWaiting(p); op.buffer.full().cancelWaiting(p); op.buffer.mutex().cancelWaiting(p);
            for (int i = 0; i < wakes.size();) { if (wakes.get(i).process() == p) { wakes.removeAt(i); } else { i++; } }
            if (op.mutexHeld) { releaseMutex(op, cycle); }
            if (op.countingPermit) {
                op.countingPermit = false; grantCount((op.producer() ? op.buffer.empty() : op.buffer.full()).signal(), cycle);
            }
            if (op.modified) {
                op.modified = false; grantCount((op.producer() ? op.buffer.full() : op.buffer.empty()).signal(), cycle);
            }
            events.addLast(new SimulationEvent(cycle, p.definition().computerId(), p.id(),
                    "Limpieza " + reason + " en buffer " + op.buffer.definition().id() + "; contenido confirmado conservado"));
            for (int i = 0; i < operations.size(); i++) { if (operations.get(i) == op) { operations.removeAt(i); break; } }
        } finally { mutex.release(); }
    }
    public BufferOperationSnapshot operationSnapshot(ProcessControlBlock p) {
        mutex.acquireUninterruptibly();
        try { BufferOperation op = find(p); return op == null ? null : op.snapshot(); }
        finally { mutex.release(); }
    }
    public int eventCount() { mutex.acquireUninterruptibly(); try { return events.size(); } finally { mutex.release(); } }
    public SimulationEvent eventAt(int index) { mutex.acquireUninterruptibly(); try { return events.get(index); } finally { mutex.release(); } }
}
