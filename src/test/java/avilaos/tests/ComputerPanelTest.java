package avilaos.tests;
import avilaos.ui.*;
import avilaos.simulation.*;
import avilaos.process.*;
import avilaos.sync.BufferDefinition;
import avilaos.scheduling.SchedulingPolicyType;
import javax.swing.*;
import static avilaos.tests.TestSupport.*;
public final class ComputerPanelTest {
    public static void run(){
        try(Simulation sim=new Simulation(SimulationTest.configs(),100)){
            sim.enqueue(new AddProcessCommand(SimulationTest.cpu(1,3)));sim.enqueue(new AddProcessCommand(SimulationTest.cpu(1,2)));sim.enqueue(new AddProcessCommand(SimulationTest.cpu(1,1)));
            sim.enqueue(new AddProcessCommand(new ProcessDefinition("io",2,ProcessType.IO_BOUND,3,1,0,20,1,2,0,0,0)));
            BufferSimulationTest.step(sim);SimulationSnapshot before=BufferSimulationTest.step(sim);ComputerPanel[] panel={null};
            SwingTestSupport.edt(()->{
                panel[0]=new ComputerPanel();panel[0].render(before.computerAt(0),before.nextCycle());
                JTable ready=SwingTestSupport.find(panel[0],"computer.ready",JTable.class);equal(1,ready.getRowCount());
                JTable incoming=SwingTestSupport.find(panel[0],"computer.new",JTable.class);equal(1,incoming.getRowCount());
                ready.setRowSelectionInterval(0,0);String detail=SwingTestSupport.find(panel[0],"pcb.details",JTextArea.class).getText();isTrue(detail.contains("P2"));isTrue(detail.contains("Memoria requerida"));
                JTabbedPane tabs=SwingTestSupport.find(panel[0],"computer.queues",JTabbedPane.class);tabs.setSelectedIndex(2);
                panel[0].render(before.computerAt(0),before.nextCycle());equal(2,tabs.getSelectedIndex());
                isTrue(SwingTestSupport.find(panel[0],"pcb.details",JTextArea.class).getText().contains("P2"));
                SwingTestSupport.find(panel[0],"computer.inspect",JButton.class).doClick();equal(-1,ready.getSelectedRow());
                isTrue(SwingTestSupport.find(panel[0],"pcb.details",JTextArea.class).getText().contains("P1"));
                ready.setRowSelectionInterval(0,0);
            });
            sim.enqueue(new ChangeSchedulingCommand(1,SchedulingPolicyType.ROUND_ROBIN,1));BufferSimulationTest.step(sim);SimulationSnapshot after=BufferSimulationTest.step(sim);
            SwingTestSupport.edt(()->{
                panel[0].render(after.computerAt(0),after.nextCycle());String detail=SwingTestSupport.find(panel[0],"pcb.details",JTextArea.class).getText();isTrue(detail.contains("P2"));isTrue(detail.contains("PC: 1"));
                long[] changes={0};panel[0].setSchedulingHandler((id,policy,q)->{equal(1,id);equal(SchedulingPolicyType.EDF,policy);equal(3,q);changes[0]++;});
                SwingTestSupport.find(panel[0],"computer.policy",JComboBox.class).setSelectedItem(SchedulingPolicyType.EDF);SwingTestSupport.find(panel[0],"computer.quantum",JTextField.class).setText("3");
                panel[0].render(after.computerAt(0),after.nextCycle());equal("3",SwingTestSupport.find(panel[0],"computer.quantum",JTextField.class).getText());
                SwingTestSupport.find(panel[0],"computer.applyScheduling",JButton.class).doClick();equal(1L,changes[0]);
                panel[0].render(before.computerAt(1),before.nextCycle());equal(1,SwingTestSupport.find(panel[0],"computer.blocked",JTable.class).getRowCount());
                JTable blocked=SwingTestSupport.find(panel[0],"computer.blocked",JTable.class);blocked.setRowSelectionInterval(0,0);String io=SwingTestSupport.find(panel[0],"pcb.details",JTextArea.class).getText();isTrue(io.contains("E/S"));isTrue(io.contains("Ráfaga"));
                panel[0].clearSelection();equal("Selecciona un proceso en la CPU o en una cola.",SwingTestSupport.find(panel[0],"pcb.details",JTextArea.class).getText());
            });
            for(int i=0;i<6;i++)BufferSimulationTest.step(sim);SimulationSnapshot end=sim.snapshot();
            SwingTestSupport.edt(()->{panel[0].render(end.computerAt(0),end.nextCycle());equal(3,SwingTestSupport.find(panel[0],"computer.terminated",JTable.class).getRowCount());isTrue(SwingTestSupport.find(panel[0],"computer.current",JLabel.class).getText().contains("disponible"));});
        }
        try(Simulation sim=new Simulation(SimulationTest.configs(),100)){
            sim.enqueue(new AddBufferCommand(new BufferDefinition(1,1,1)));sim.enqueue(new AddProcessCommand(BufferSimulationTest.definition(1,ProcessType.CONSUMER)));SimulationSnapshot v=BufferSimulationTest.step(sim);
            SwingTestSupport.edt(()->{ComputerPanel p=new ComputerPanel();p.render(v.computerAt(0),1);JTable blocked=SwingTestSupport.find(p,"computer.blocked",JTable.class);blocked.setRowSelectionInterval(0,0);String details=SwingTestSupport.find(p,"pcb.details",JTextArea.class).getText();isTrue(details.contains("Llenos"));isTrue(details.contains("wait(llenos)"));isTrue(details.contains("Buffer: 1"));});
        }
    }
}
