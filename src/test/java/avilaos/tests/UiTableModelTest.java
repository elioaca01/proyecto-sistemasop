package avilaos.tests;
import avilaos.ui.model.*;
import avilaos.ui.UiText;
import avilaos.simulation.*;
import avilaos.process.*;
import avilaos.sync.*;
import avilaos.hardware.Ram;
import avilaos.scheduling.SchedulingPolicyType;
import static avilaos.tests.TestSupport.*;
public final class UiTableModelTest {
    public static void run(){
        ProcessControlBlock a=new ProcessControlBlock(1,SimulationTest.cpu(1,1),0),b=new ProcessControlBlock(2,SimulationTest.cpu(1,2),0);a.admit();b.admit();
        ProcessSnapshot pa=ProcessSnapshot.capture(a),pb=ProcessSnapshot.capture(b);
        a.dispatch(0);a.executeInstruction(avilaos.hardware.CpuMode.USER);a.terminate(TerminationReason.COMPLETED,0);ProcessSnapshot done=ProcessSnapshot.capture(a);
        BufferRegistry r=new BufferRegistry();r.registerRam(1,new Ram(Integer.MAX_VALUE));r.create(new BufferDefinition(1,1,Integer.MAX_VALUE));BufferSnapshot large=new BufferService(r).snapshotAt(0);
        final SimulationSnapshot snapshot;
        try(Simulation sim=new Simulation(SimulationTest.configs(),100)){
            sim.enqueue(new AddBufferCommand(new BufferDefinition(1,1,1)));sim.enqueue(new AddProcessCommand(BufferSimulationTest.definition(1,ProcessType.PRODUCER)));sim.enqueue(new AddProcessCommand(BufferSimulationTest.definition(2,ProcessType.CONSUMER)));
            for(int i=0;i<4;i++)BufferSimulationTest.step(sim);snapshot=sim.snapshot();
        }
        SwingTestSupport.edt(()->{
            ProcessTableModel m=new ProcessTableModel();ProcessSnapshot[] rows={pa,pb};m.setRows(rows);rows[0]=null;equal(1L,m.processAt(0).id());
            m.setRows(new ProcessSnapshot[]{pb,pa});equal(1,m.rowOf(1));equal(2L,m.getValueAt(0,0));isTrue(!m.isCellEditable(0,1));equal(-1,m.rowOf(9));
            equal(17L,UiText.deadlineRemaining(pb,3));equal(19L,UiText.deadlineRemaining(done,100));equal("—",UiText.label(BlockReason.NONE));
            BufferItemTableModel items=new BufferItemTableModel();items.setBuffer(large);equal(0,items.getRowCount());items.setBuffer(snapshot.bufferAt(0));equal(1,items.getRowCount());equal(1L,items.getValueAt(0,0));equal(1L,items.getValueAt(0,1));equal(1,items.getValueAt(0,2));
            SemaphoreTableModel sem=new SemaphoreTableModel();sem.setBuffer(snapshot.bufferAt(0));equal(3,sem.getRowCount());equal(0,sem.getValueAt(1,1));isTrue(sem.getValueAt(1,2).toString().contains("P2"));
            BufferOperationTableModel ops=new BufferOperationTableModel();ops.setBuffer(snapshot.bufferAt(0));equal(2,ops.getRowCount());
            EventTableModel log=new EventTableModel();log.setSnapshot(snapshot);int count=log.getRowCount();isTrue(count>0);log.setSnapshot(snapshot);equal(count,log.getRowCount());log.setSnapshot(null);equal(0,log.getRowCount());
            ComputerConfig[] cs=SimulationTest.configs();ComputerConfigTableModel config=new ComputerConfigTableModel(cs);cs[0]=null;config.setValueAt("24",0,1);equal(24,config.configs()[0].ramCapacity());
            config.setValueAt("1.5",0,1);equal(24,config.configs()[0].ramCapacity());isTrue(!config.validationError().isEmpty());config.setValueAt(SchedulingPolicyType.EDF,0,2);equal(SchedulingPolicyType.EDF,config.configs()[0].policy());
            ComputerConfig[] escaped=config.configs();escaped[0]=null;isTrue(config.configs()[0]!=null);
        });
    }
}
