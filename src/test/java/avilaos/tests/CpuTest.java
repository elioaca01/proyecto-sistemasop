package avilaos.tests;

import avilaos.hardware.*;
import avilaos.process.*;
import avilaos.simulation.Tickable;

public final class CpuTest {
    static ProcessControlBlock io(long id, int instructions, int burst, int duration) {
        return new ProcessControlBlock(id, new ProcessDefinition("IO" + id, 1,
                ProcessType.IO_BOUND, instructions, 1, 0, 10, burst, duration, 0, 0, 0), 0);
    }
    public static void run() {
        Cpu cpu = new Cpu();
        Tickable clockTarget = cpu;
        clockTarget.onTick(0);
        TestSupport.equal(CpuMode.IDLE, cpu.mode());
        TestSupport.equal(null, cpu.lastInstruction());
        TestSupport.equal(-1L, cpu.lastProcessId());
        ProcessControlBlock p = SchedulingTest.ready(1, 0);
        TestSupport.throwsType(IllegalArgumentException.class, () -> cpu.load(null));
        TestSupport.throwsType(IllegalArgumentException.class, () -> cpu.load(SchedulingTest.process(9, 0)));
        p.dispatch(1); cpu.load(p);
        TestSupport.throwsType(IllegalStateException.class, () -> cpu.load(p));
        cpu.onTick(1);
        TestSupport.equal(1, p.pc()); TestSupport.equal(1, p.mar());
        TestSupport.equal(1L, p.userCycles());
        TestSupport.equal(CpuMode.USER, cpu.mode());
        TestSupport.equal(1L, cpu.lastProcessId());
        for (long bad : new long[]{-1, 0, 1, 3, Long.MAX_VALUE}) {
            TestSupport.throwsType(IllegalArgumentException.class, () -> cpu.onTick(bad));
            TestSupport.equal(1, p.pc());
        }
        cpu.onTick(2); cpu.onTick(3);
        TestSupport.equal(3, p.pc()); TestSupport.equal(3, p.mar());
        TestSupport.equal(3L, p.userCycles());
        TestSupport.equal(ProcessState.RUNNING, p.state());
        cpu.clear();
        TestSupport.equal(null, cpu.current());
        TestSupport.equal(InstructionKind.COMPUTE, cpu.lastInstruction());
        TestSupport.throwsType(IllegalArgumentException.class, () -> cpu.load(p));
        cpu.onTick(4);
        TestSupport.equal(CpuMode.IDLE, cpu.mode());
        TestSupport.equal(-1L, cpu.lastProcessId());
        Cpu ioCpu = new Cpu();
        ProcessControlBlock q = io(2, 3, 1, 2); q.admit(); q.dispatch(0); ioCpu.load(q);
        TestSupport.equal(InstructionKind.COMPUTE, LinearProgram.nextInstruction(q));
        TestSupport.equal(0, q.pc());
        ioCpu.onTick(0);
        TestSupport.equal(CpuMode.USER, ioCpu.mode());
        ioCpu.onTick(1);
        TestSupport.equal(InstructionKind.IO_REQUEST, ioCpu.lastInstruction());
        TestSupport.equal(CpuMode.KERNEL, ioCpu.mode());
        TestSupport.equal(ProcessState.RUNNING, q.state());
        ioCpu.onTick(2);
        TestSupport.equal(CpuMode.USER, ioCpu.mode());
        TestSupport.equal(2L, q.userCycles()); TestSupport.equal(1L, q.kernelCycles());
        TestSupport.throwsType(IllegalArgumentException.class, () -> LinearProgram.nextInstruction(q));
        TestSupport.throwsType(IllegalArgumentException.class, () -> LinearProgram.nextInstruction(null));
        ProcessControlBlock producer = new ProcessControlBlock(3, new ProcessDefinition("Prod", 1,
                ProcessType.PRODUCER, 6, 1, 0, 10, 0, 0, 1, 1, 1), 0);
        producer.admit(); producer.dispatch(0);
        TestSupport.throwsType(IllegalArgumentException.class, () -> new Cpu().load(producer));
        ProcessControlBlock maxBurst = io(4, 2, Integer.MAX_VALUE, 1);
        TestSupport.equal(InstructionKind.COMPUTE, LinearProgram.nextInstruction(maxBurst));
    }
}
