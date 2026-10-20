package avilaos.simulation;

import avilaos.hardware.*;
import avilaos.kernel.Kernel;
import avilaos.process.ProcessControlBlock;
import avilaos.scheduling.FcfsPolicy;

/** Unidad reutilizable: cada instancia tiene su propio hardware, núcleo y colas. */
public final class Computer implements Tickable {
    private final int id;
    private final Cpu cpu;
    private final Ram ram;
    private final IoController io;
    private final Kernel kernel;

    public Computer(int id, int ramCapacity) {
        if (id <= 0) { throw new IllegalArgumentException("ID de computador positivo requerido"); }
        this.id = id;
        cpu = new Cpu();
        ram = new Ram(ramCapacity);
        io = new IoController();
        kernel = new Kernel(id, cpu, ram, io, new FcfsPolicy());
    }
    public int id() { return id; }
    public Cpu cpu() { return cpu; }
    public Ram ram() { return ram; }
    public IoController io() { return io; }
    public Kernel kernel() { return kernel; }
    public void submit(ProcessControlBlock process) { kernel.submit(process); }
    @Override public void onTick(long cycle) { kernel.onTick(cycle); }
}
