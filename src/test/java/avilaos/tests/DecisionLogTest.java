package avilaos.tests;
import avilaos.simulation.*;
import avilaos.process.*;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.sync.BufferDefinition;
import static avilaos.tests.TestSupport.*;
public final class DecisionLogTest {
    private static void event(SimulationSnapshot s,long cycle,int computer,long process,String text){
        boolean found=false;for(int i=0;i<s.eventCount();i++){SimulationEvent e=s.eventAt(i);if(e.cycle()==cycle&&e.computerId()==computer&&e.processId()==process&&e.message().contains(text))found=true;}isTrue(found);
    }
    public static void run(){
        try(Simulation s=new Simulation(SimulationTest.configs(),100)){
            s.enqueue(new AddBufferCommand(new BufferDefinition(1,1,1)));s.enqueue(new AddProcessCommand(BufferSimulationTest.definition(1,ProcessType.CONSUMER)));s.enqueue(new AddProcessCommand(BufferSimulationTest.definition(1,ProcessType.PRODUCER)));
            for(int i=0;i<12;i++)BufferSimulationTest.step(s);
            SimulationSnapshot v=s.snapshot();event(v,0,1,1,"selecciona");event(v,0,1,1,"bloquea");event(v,4,1,2,"inserta");event(v,7,1,1,"desbloquea");event(v,8,1,1,"extrae");event(v,11,1,1,"termina");event(v,0,1,1,"admite");
            isTrue(v.eventCount()>12);int saved=v.eventCount();BufferSimulationTest.step(s);equal(saved,v.eventCount());
        }
        try(Simulation s=new Simulation(SimulationTest.configs(),100,2)){
            s.enqueue(new AddBufferCommand(new BufferDefinition(1,1,1)));s.enqueue(new AddProcessCommand(BufferSimulationTest.definition(2,ProcessType.PRODUCER)));
            for(int i=0;i<8;i++)BufferSimulationTest.step(s);
            event(s.snapshot(),1,2,1,"remoto");event(s.snapshot(),3,2,1,"desbloquea");event(s.snapshot(),7,2,1,"termina");
        }
        try(Simulation s=new Simulation(SimulationTest.configs(),100)){
            s.enqueue(new AddProcessCommand(SimulationTest.cpu(1,10)));BufferSimulationTest.step(s);
            s.enqueue(new ChangeSchedulingCommand(1,SchedulingPolicyType.EDF,1));
            s.enqueue(new AddProcessCommand(new ProcessDefinition("urgente",1,ProcessType.CPU_BOUND,1,1,0,2,0,0,0,0,0)));
            s.enqueue(new AddProcessCommand(SimulationTest.cpu(9,1)));BufferSimulationTest.step(s);
            event(s.snapshot(),1,1,1,"apropia");event(s.snapshot(),1,1,-1,"planificación");event(s.snapshot(),1,0,-1,"rechazada");
        }
        try(Simulation s=new Simulation(SimulationTest.configs(),100)){
            s.enqueue(new AddProcessCommand(new ProcessDefinition("io",1,ProcessType.IO_BOUND,3,1,0,20,1,2,0,0,0)));
            for(int i=0;i<5;i++)BufferSimulationTest.step(s);
            event(s.snapshot(),1,1,1,"IO");event(s.snapshot(),4,1,1,"desbloquea");
        }
    }
}
