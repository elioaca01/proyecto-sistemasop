package avilaos.tests;
import avilaos.ui.BufferPanel;
import avilaos.simulation.*;
import avilaos.process.*;
import avilaos.sync.*;
import javax.swing.*;
import static avilaos.tests.TestSupport.*;
public final class BufferPanelTest {
    public static void run(){
        try(Simulation sim=new Simulation(SimulationTest.configs(),100)){
            sim.enqueue(new AddBufferCommand(new BufferDefinition(1,1,1)));sim.enqueue(new AddProcessCommand(BufferSimulationTest.definition(1,ProcessType.PRODUCER)));sim.enqueue(new AddProcessCommand(BufferSimulationTest.definition(2,ProcessType.CONSUMER)));
            for(int i=0;i<4;i++)BufferSimulationTest.step(sim);SimulationSnapshot inserted=sim.snapshot();BufferPanel[] panel={null};
            SwingTestSupport.edt(()->{panel[0]=new BufferPanel();panel[0].render(inserted);equal(1,SwingTestSupport.find(panel[0],"buffer.items",JTable.class).getRowCount());equal(3,SwingTestSupport.find(panel[0],"buffer.semaphores",JTable.class).getRowCount());isTrue(SwingTestSupport.find(panel[0],"buffer.summary",JLabel.class).getText().contains("P1"));});
            for(int i=0;i<4;i++)BufferSimulationTest.step(sim);SimulationSnapshot removed=sim.snapshot();
            SwingTestSupport.edt(()->{panel[0].render(removed);equal(0,SwingTestSupport.find(panel[0],"buffer.items",JTable.class).getRowCount());equal(1,SwingTestSupport.find(panel[0],"buffer.selector",JComboBox.class).getSelectedItem());panel[0].render(new SimulationSnapshot(0,0,SimulationState.PAUSED,100,null,new ComputerSnapshot[0],new CommandResult[0]));equal(0,SwingTestSupport.find(panel[0],"buffer.semaphores",JTable.class).getRowCount());});
        }
        try(Simulation sim=new Simulation(new ComputerConfig[]{new ComputerConfig(1,Integer.MAX_VALUE,avilaos.scheduling.SchedulingPolicyType.FCFS,1),new ComputerConfig(2,10,avilaos.scheduling.SchedulingPolicyType.FCFS,1)},100)){
            sim.enqueue(new AddBufferCommand(new BufferDefinition(1,1,Integer.MAX_VALUE)));SimulationSnapshot v=BufferSimulationTest.step(sim);
            SwingTestSupport.edt(()->{BufferPanel p=new BufferPanel();p.render(v);equal(0,SwingTestSupport.find(p,"buffer.items",JTable.class).getRowCount());equal(Integer.MAX_VALUE,SwingTestSupport.find(p,"buffer.occupancy",JProgressBar.class).getMaximum());});
        }
    }
}
