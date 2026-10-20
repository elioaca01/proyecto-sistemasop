package avilaos.simulation;

import avilaos.hardware.*;
import avilaos.kernel.Kernel;
import avilaos.process.ProcessControlBlock;
import avilaos.scheduling.SchedulingPolicies;
import avilaos.scheduling.SchedulingPolicyType;

/** Unidad reutilizable: cada instancia tiene su propio hardware, núcleo y colas. */
public final class Computer implements Tickable {
    private final int id;
    private final Cpu cpu;
    private final Ram ram;
    private final IoController io;
    private final Kernel kernel;

    public Computer(int id, int ramCapacity) {
        this(id, ramCapacity, SchedulingPolicyType.FCFS, 2);
    }
    public Computer(int id, int ramCapacity, SchedulingPolicyType policy, int quantum) {
        if (id <= 0) { throw new IllegalArgumentException("ID de computador positivo requerido"); }
        this.id = id;
        cpu = new Cpu();
        ram = new Ram(ramCapacity);
        io = new IoController();
        kernel = new Kernel(id, cpu, ram, io, SchedulingPolicies.create(policy), quantum);
    }
    public void configureScheduling(SchedulingPolicyType policy, int quantum) { kernel.configureScheduling(policy, quantum); }
    public SchedulingPolicyType policyType() { return kernel.policyType(); }
    public int quantum() { return kernel.quantum(); }
    public int sliceUsed() { return kernel.sliceUsed(); }
    public int id() { return id; }
    public Cpu cpu() { return cpu; }
    public Ram ram() { return ram; }
    public IoController io() { return io; }
    public Kernel kernel() { return kernel; }
    public void submit(ProcessControlBlock process) { kernel.submit(process); }
    @Override public void onTick(long cycle) { kernel.onTick(cycle); }
}
