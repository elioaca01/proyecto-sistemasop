package avilaos.tests;
import avilaos.config.SimulationConfig;
import avilaos.simulation.*;
import avilaos.sync.*;
import avilaos.process.*;
import avilaos.scheduling.SchedulingPolicyType;
import static avilaos.tests.TestSupport.*;
public final class SimulationConfigTest {
    public static SimulationConfig empty() { return new SimulationConfig(500,2,SimulationTest.configs(),new BufferDefinition[0],new ProcessDefinition[0]); }
    private static ProcessDefinition cpu(int id,int memory) { return new ProcessDefinition("cpu",id,ProcessType.CPU_BOUND,3,memory,0,20,0,0,0,0,0); }
    public static void run() {
        ComputerConfig[] cs=SimulationTest.configs(); BufferDefinition[] bs={new BufferDefinition(1,1,3)}; ProcessDefinition[] ps={cpu(1,8)};
        SimulationConfig c=new SimulationConfig(500,2,cs,bs,ps);
        cs[0]=null;bs[0]=null;ps[0]=null;
        equal(2,c.computerCount());equal(3,c.bufferAt(0).capacity());equal(8,c.processAt(0).memoryRequired());
        SimulationConfig more=c.withAddedProcess(cpu(2,10));equal(2,more.processCount());equal(1,c.processCount());
        throwsType(IllegalArgumentException.class,()->c.withAddedBuffer(new BufferDefinition(2,1,8)));
        throwsType(IllegalArgumentException.class,()->c.withAddedBuffer(new BufferDefinition(1,2,1)));
        throwsType(IllegalArgumentException.class,()->c.withAddedBuffer(new BufferDefinition(2,9,1)));
        throwsType(IllegalArgumentException.class,()->c.withAddedProcess(cpu(9,1)));
        throwsType(IllegalArgumentException.class,()->c.withAddedProcess(cpu(1,11)));
        throwsType(IllegalArgumentException.class,()->c.withComputers(new ComputerConfig[]{new ComputerConfig(2,10,SchedulingPolicyType.FCFS,2),new ComputerConfig(3,10,SchedulingPolicyType.FCFS,2)}));
        SimulationConfig referenced=c.withAddedProcess(BufferSimulationTest.definition(1,ProcessType.PRODUCER));
        throwsType(IllegalArgumentException.class,()->referenced.withoutBuffer(1));
        equal(0,referenced.withoutProcess(1).withoutBuffer(1).bufferCount());
        throwsType(IndexOutOfBoundsException.class,()->c.withoutProcess(1));
        throwsType(IllegalArgumentException.class,()->c.withoutBuffer(99));
        throwsType(IllegalArgumentException.class,()->c.withTiming(0,0));
        throwsType(IllegalArgumentException.class,()->c.withTiming(1,-1));
        equal(1L,c.withTiming(1,0).cycleMillis());equal(0,c.withTiming(1,0).networkLatency());
        throwsType(IllegalArgumentException.class,()->new SimulationConfig(1,0,null,new BufferDefinition[0],new ProcessDefinition[0]));
        throwsType(IllegalArgumentException.class,()->new SimulationConfig(1,0,new ComputerConfig[1],new BufferDefinition[0],new ProcessDefinition[0]));
        ComputerConfig a=SimulationTest.configs()[0];
        throwsType(IllegalArgumentException.class,()->new SimulationConfig(1,0,new ComputerConfig[]{a,a},new BufferDefinition[0],new ProcessDefinition[0]));
        throwsType(IllegalArgumentException.class,()->empty().withAddedBuffer(null));
        throwsType(IllegalArgumentException.class,()->empty().withAddedProcess(null));
        throwsType(IllegalArgumentException.class,()->empty().withAddedProcess(BufferSimulationTest.definition(1,ProcessType.CONSUMER)));
        throwsType(IllegalArgumentException.class,()->new SimulationConfig(1,0,SimulationTest.configs(),new BufferDefinition[]{new BufferDefinition(1,1,6),new BufferDefinition(2,1,5)},new ProcessDefinition[0]));
    }
}
