package avilaos.simulation;

import avilaos.process.ProcessControlBlock;
import avilaos.process.ProcessDefinition;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.structures.LinkedList;
import avilaos.structures.LinkedQueue;
import java.util.concurrent.Semaphore;
import avilaos.sync.*;
import avilaos.metrics.*;

/** Propietario del clúster. La API externa solo entrega órdenes o lee valores inmutables. */
public final class Simulation implements ClockDriver, AutoCloseable {
    private record Request(long id, SimulationCommand command) { }
    private final Computer[] computers;
    private final ComputerWorker[] workers;
    private final GlobalClock clock;
    private final SnapshotStore snapshots;
    private final BufferService buffers;
    private final EventLog events=new EventLog();
    private final UtilizationHistory utilizationHistory = new UtilizationHistory();
    private MetricsSnapshot metrics;
    private final Semaphore commandsMutex = new Semaphore(1), registryMutex = new Semaphore(1);
    private LinkedQueue<Request> pending = new LinkedQueue<>();
    private LinkedQueue<Request> batch = new LinkedQueue<>();
    private final LinkedList<CommandResult> results = new LinkedList<>();
    private final LinkedList<ProcessControlBlock> registry = new LinkedList<>();
    private boolean accepting = true;
    private long nextRequestId = 1, nextProcessId = 1, version, applyingCycle, cycleMillis;

    public Simulation(ComputerConfig[] configs, long cycleMillis) {
        this(configs, cycleMillis, 0);
    }
    public Simulation(ComputerConfig[] configs, long cycleMillis, int networkLatency) {
        if (configs == null || configs.length < 2 || cycleMillis <= 0 || networkLatency < 0) { throw new IllegalArgumentException("Configuración o latencia inválida"); }
        ComputerConfig[] ordered = configs.clone();
        for (int i = 0; i < ordered.length; i++) {
            if (ordered[i] == null) { throw new IllegalArgumentException("Configuración nula"); }
            for (int j = 0; j < i; j++) {
                if (ordered[i].id() == ordered[j].id()) { throw new IllegalArgumentException("ID de computador duplicado"); }
            }
        }
        // Inserción propia por ID; no modificar el arreglo del llamador.
        for (int i = 1; i < ordered.length; i++) {
            ComputerConfig value = ordered[i]; int j = i;
            while (j > 0 && ordered[j - 1].id() > value.id()) { ordered[j] = ordered[j - 1]; j--; }
            ordered[j] = value;
        }
        this.cycleMillis = cycleMillis;
        buffers = new BufferService(new BufferRegistry(), networkLatency, events);
        computers = new Computer[ordered.length]; workers = new ComputerWorker[ordered.length];
        for (int i = 0; i < ordered.length; i++) {
            ComputerConfig c = ordered[i];
            computers[i] = new Computer(c.id(), c.ramCapacity(), c.policy(), c.quantum(), buffers, events);
            buffers.registerRam(c.id(), computers[i].ram());
            workers[i] = new ComputerWorker(c.id(), computers[i]);
        }
        snapshots = new SnapshotStore(capture(0, 0, SimulationState.PAUSED, null));
        clock = new GlobalClock(this, cycleMillis);
        try { for (ComputerWorker worker : workers) { worker.start(); } clock.start(); }
        catch (Throwable failure) {
            for (ComputerWorker worker : workers) {
                try { worker.close(); } catch (Throwable cleanup) { failure.addSuppressed(cleanup); }
            }
            throw failure;
        }
    }
    public long enqueue(SimulationCommand command) {
        if (command == null) { throw new IllegalArgumentException("Orden requerida"); }
        commandsMutex.acquireUninterruptibly();
        try {
            if (!accepting) { throw new IllegalStateException("Simulación cerrada"); }
            long id = nextRequestId, following = Math.incrementExact(id);
            pending.enqueue(new Request(id, command)); nextRequestId = following; return id;
        } finally { commandsMutex.release(); }
    }
    private void requireCoordinator() {
        if (!clock.ownsCurrentThread()) { throw new IllegalStateException("Solo el coordinador modifica la simulación"); }
    }
    private Computer computer(int id) {
        for (Computer c : computers) { if (c.id() == id) { return c; } }
        throw new IllegalArgumentException("Computador inexistente: " + id);
    }
    long applyProcess(ProcessDefinition definition) {
        requireCoordinator(); Computer target = computer(definition.computerId());
        registryMutex.acquireUninterruptibly();
        try {
            long following = Math.incrementExact(nextProcessId);
            ProcessControlBlock process = new ProcessControlBlock(nextProcessId, definition, applyingCycle);
            target.submit(process); events.append(new SimulationEvent(applyingCycle,definition.computerId(),process.id(),"Alta de P"+process.id()+" ("+definition.name()+")")); registry.addLast(process); nextProcessId = following; return process.id();
        } finally { registryMutex.release(); }
    }
    long applyScheduling(int id, SchedulingPolicyType policy, int quantum) {
        requireCoordinator(); computer(id).configureScheduling(policy, quantum); return -1;
    }
    long applyBuffer(BufferDefinition definition) {
        requireCoordinator(); buffers.createBuffer(definition); events.append(new SimulationEvent(applyingCycle,definition.hostId(),-1,"Buffer B"+definition.id()+" creado; capacidad "+definition.capacity())); return -1;
    }
    long applyDuration(long millis) { requireCoordinator(); cycleMillis = millis; events.append(new SimulationEvent(applyingCycle,0,-1,"Duración del ciclo: "+millis+" ms")); return -1; }
    @Override public long advanceCycle(long cycle) {
        requireCoordinator(); applyingCycle = cycle;
        commandsMutex.acquireUninterruptibly();
        try { batch = pending; pending = new LinkedQueue<>(); }
        finally { commandsMutex.release(); }
        Request request;
        while ((request = batch.dequeue()) != null) {
            try {
                long id = request.command().apply(this);
                String message = request.command() instanceof AddBufferCommand added ? "Buffer " + added.definition().id() + " creado" : "Aplicada";
                result(new CommandResult(request.id(), cycle, true, id, message));
            } catch (IllegalArgumentException | IllegalStateException rejected) {
                result(new CommandResult(request.id(), cycle, false, -1, rejected.getMessage()));
                events.append(new SimulationEvent(cycle,0,-1,"Orden "+request.id()+" rechazada: "+rejected.getMessage()));
            } catch (Throwable unexpected) {
                result(new CommandResult(request.id(), cycle, false, -1, unexpected.toString()));
                throw unexpected;
            }
        }
        LinkedQueue<BufferWake> due = buffers.beginCycle(cycle);
        while (!due.isEmpty()) {
            BufferWake wake = due.dequeue(); computer(wake.process().definition().computerId()).unblockExternal(wake.process(), wake.reason());
        }
        for (ComputerWorker worker : workers) {
            try { worker.executeCycle(cycle); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Coordinación interrumpida", e); }
            catch (IllegalStateException e) { throw new IllegalStateException("Fallo del computador " + worker.computerId(), e); }
        }
        MetricsSnapshot confirmed = MetricsSnapshot.calculate(Math.addExact(cycle, 1), captureComputers(), captureBuffers());
        utilizationHistory.record(confirmed); metrics = confirmed;
        return cycleMillis;
    }
    private void result(CommandResult result) {
        commandsMutex.acquireUninterruptibly();
        try { results.addLast(result); } finally { commandsMutex.release(); }
    }
    private SimulationSnapshot capture(long version, long nextCycle, SimulationState state, String error) {
        CommandResult[] copies;
        commandsMutex.acquireUninterruptibly();
        try {
            copies = new CommandResult[results.size()];
            results.copyInto(copies);
        } finally { commandsMutex.release(); }
        ComputerSnapshot[] machineCopies = captureComputers();
        BufferSnapshot[] bufferCopies = captureBuffers();
        if (metrics == null) { metrics = MetricsSnapshot.calculate(0, machineCopies, bufferCopies); }
        SimulationEvent[] eventCopies = events.copy();
        return new SimulationSnapshot(version, nextCycle, state, cycleMillis, error, machineCopies, copies,
                buffers.latency(), bufferCopies, eventCopies, metrics, utilizationHistory.copy());
    }
    private ComputerSnapshot[] captureComputers() {
        ComputerSnapshot[] result = new ComputerSnapshot[computers.length];
        registryMutex.acquireUninterruptibly();
        try { for (int i = 0; i < computers.length; i++) { result[i] = ComputerSnapshot.capture(computers[i], buffers); } }
        finally { registryMutex.release(); }
        return result;
    }
    private BufferSnapshot[] captureBuffers() {
        BufferSnapshot[] result = new BufferSnapshot[buffers.bufferCount()];
        for (int i = 0; i < result.length; i++) { result[i] = buffers.snapshotAt(i); }
        return result;
    }
    @Override public void publishState(long nextCycle, SimulationState state, long duration, String error) {
        requireCoordinator();
        long following = Math.incrementExact(version);
        snapshots.publish(capture(following, nextCycle, state, error)); version = following;
    }
    @Override public void shutdown() {
        requireCoordinator();
        commandsMutex.acquireUninterruptibly();
        try { accepting = false; } finally { commandsMutex.release(); }
        cancelBatch(batch); cancelBatch(pending);
        RuntimeException failure = null;
        for (ComputerWorker worker : workers) {
            try { worker.close(); }
            catch (RuntimeException e) { if (failure == null) { failure = e; } else { failure.addSuppressed(e); } }
        }
        if (failure != null) { throw failure; }
    }
    private void cancelBatch(LinkedQueue<Request> queue) {
        Request request;
        while ((request = queue.dequeue()) != null) {
            result(new CommandResult(request.id(), -1, false, -1, "Cerrada antes de aplicar"));
        }
    }
    public SimulationSnapshot snapshot() { return snapshots.get(); }
    public SimulationSnapshot awaitNextSnapshot(long afterVersion, long timeoutMillis) throws InterruptedException {
        return snapshots.awaitNewer(afterVersion, timeoutMillis);
    }
    private void control(Runnable action) {
        commandsMutex.acquireUninterruptibly();
        try { if (!accepting) { throw new IllegalStateException("Simulación cerrada"); } action.run(); }
        finally { commandsMutex.release(); }
    }
    public void resume() { control(clock::resume); }
    public void pause() { control(clock::pause); }
    public void step() { control(clock::step); }
    public void stop() {
        commandsMutex.acquireUninterruptibly();
        try { accepting = false; clock.stop(); } finally { commandsMutex.release(); }
    }
    public boolean awaitStopped(long timeoutMillis) throws InterruptedException { return clock.awaitStopped(timeoutMillis); }
    @Override public void close() { stop(); clock.close(); }
}
