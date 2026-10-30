package avilaos.tests;
import avilaos.ui.*;
import avilaos.ui.session.*;
import avilaos.config.SimulationConfig;
import avilaos.simulation.*;
import avilaos.process.*;
import avilaos.sync.*;
import avilaos.scheduling.SchedulingPolicyType;
import javax.swing.*;
import static avilaos.tests.TestSupport.*;

/** Recorre los controles de producción con la sesión y workers reales, sin JFrame. */
public final class UiIntegrationTest {
    private static final class Harness implements AutoCloseable {
        final UiSession session;final DashboardPanel[] panel={null};final UiController[] controller={null};
        Harness(SimulationConfig config){session=new UiSession(config);SessionTestSupport.await(session,v->v.phase()==SessionPhase.ACTIVE&&v.pendingActions()==0);SwingTestSupport.edt(()->{panel[0]=new DashboardPanel();controller[0]=new UiController(session,panel[0]);panel[0].bind(controller[0]);controller[0].refresh();});}
        SessionView step(){long cycle=session.view().simulation().nextCycle();SwingTestSupport.edt(()->SwingTestSupport.find(panel[0],"toolbar.step",JButton.class).doClick(0));SessionView v=SessionTestSupport.await(session,x->x.simulation().nextCycle()==cycle+1&&x.pendingActions()==0);SwingTestSupport.edt(controller[0]::refresh);return v;}
        public void close(){SwingTestSupport.edt(controller[0]::requestClose);SessionTestSupport.close(session);SwingTestSupport.edt(controller[0]::refresh);}
    }
    private static SimulationConfig config(SchedulingPolicyType policy,int latency){return new SimulationConfig(500,latency,new ComputerConfig[]{new ComputerConfig(1,10,policy,1),new ComputerConfig(2,10,policy,1)},new BufferDefinition[]{new BufferDefinition(1,1,1)},new ProcessDefinition[0]);}
    public static void run(){localTrace();remoteTrace();visiblePolicies();deadlineSelection();emptyAndLarge();}
    private static void localTrace(){
        SimulationConfig config=config(SchedulingPolicyType.FCFS,0).withAddedProcess(BufferSimulationTest.definition(1,ProcessType.CONSUMER)).withAddedProcess(BufferSimulationTest.definition(1,ProcessType.PRODUCER));
        try(Harness h=new Harness(config)){
            SwingTestSupport.edt(()->isTrue(SwingTestSupport.find(h.panel[0],"events.pending",JLabel.class).getText().contains("Pendientes: 3")));
            long[] who={1,2,2,2,2,2,2,1,1,1,1,1};
            for(int t=0;t<12;t++){SessionView v=h.step();equal(who[t],v.simulation().computerAt(0).lastProcessId());final int expected=t+1;SwingTestSupport.edt(()->equal("Ciclo "+expected,SwingTestSupport.find(h.panel[0],"dashboard.cycle",JLabel.class).getText()));}
            SwingTestSupport.edt(()->{JTable sem=SwingTestSupport.find(h.panel[0],"buffer.semaphores",JTable.class);equal(1,sem.getValueAt(0,1));equal(0,sem.getValueAt(1,1));equal(1,sem.getValueAt(2,1));equal(0,SwingTestSupport.find(h.panel[0],"buffer.items",JTable.class).getRowCount());equal(2,SwingTestSupport.find(h.panel[0],"computer.terminated",JTable.class).getRowCount());isTrue(SwingTestSupport.find(h.panel[0],"events.log",JTable.class).getRowCount()>12);});
        }
    }
    private static void remoteTrace(){
        try(Harness h=new Harness(config(SchedulingPolicyType.FCFS,2).withAddedProcess(BufferSimulationTest.definition(2,ProcessType.PRODUCER)))){
            SwingTestSupport.edt(()->SwingTestSupport.find(h.panel[0],"computer.selector",JComboBox.class).setSelectedItem(2));
            int[] pc={1,1,1,2,3,4,5,6};
            for(int t=0;t<8;t++){
                SessionView v=h.step();ComputerSnapshot c=v.simulation().computerAt(1);ProcessSnapshot p=c.running()!=null?c.running():c.blockedCount()>0?c.blockedAt(0):c.terminatedAt(0);equal(pc[t],p.pc());
                if(t==1||t==2)SwingTestSupport.edt(()->{JTable blocked=SwingTestSupport.find(h.panel[0],"computer.blocked",JTable.class);equal(1,blocked.getValueAt(0,4));equal("Red",blocked.getValueAt(0,9));});
            }
            equal(1L,h.session.view().simulation().bufferAt(0).produced());
        }
    }
    private static void visiblePolicies(){
        for(SchedulingPolicyType policy:SchedulingPolicyType.values()){
            SimulationConfig c=config(policy,0);
            c=c.withAddedProcess(new ProcessDefinition("A",1,ProcessType.CPU_BOUND,6,2,2,100,0,0,0,0,0));
            c=c.withAddedProcess(new ProcessDefinition("B",1,ProcessType.CPU_BOUND,6,2,0,10,0,0,0,0,0));
            c=c.withAddedProcess(new ProcessDefinition("C",1,ProcessType.CPU_BOUND,6,2,1,50,0,0,0,0,0));
            try(Harness h=new Harness(c)){
                SessionView first=h.step();boolean ordered=policy==SchedulingPolicyType.EDF||policy==SchedulingPolicyType.PREEMPTIVE_PRIORITY;equal(ordered?2L:1L,first.simulation().computerAt(0).lastProcessId());
                SwingTestSupport.edt(()->{JTable ready=SwingTestSupport.find(h.panel[0],"computer.ready",JTable.class);equal(ordered?3L:2L,ready.getValueAt(0,0));equal(ordered?1L:3L,ready.getValueAt(1,0));ready.setRowSelectionInterval(0,0);});
                if(policy==SchedulingPolicyType.ROUND_ROBIN){SessionView second=h.step();equal(2L,second.simulation().computerAt(0).lastProcessId());SwingTestSupport.edt(()->{String details=SwingTestSupport.find(h.panel[0],"pcb.details",JTextArea.class).getText();isTrue(details.contains("P2"));isTrue(details.contains("PC: 1"));});}
            }
        }
    }
    private static void deadlineSelection(){
        ProcessDefinition c=new ProcessDefinition("vence",1,ProcessType.CONSUMER,6,2,0,2,0,0,1,1,1);
        try(Harness h=new Harness(config(SchedulingPolicyType.FCFS,0).withAddedProcess(c))){
            h.step();SwingTestSupport.edt(()->SwingTestSupport.find(h.panel[0],"computer.blocked",JTable.class).setRowSelectionInterval(0,0));h.step();
            SwingTestSupport.edt(()->{equal(0,SwingTestSupport.find(h.panel[0],"computer.blocked",JTable.class).getRowCount());equal(1,SwingTestSupport.find(h.panel[0],"computer.terminated",JTable.class).getRowCount());String details=SwingTestSupport.find(h.panel[0],"pcb.details",JTextArea.class).getText();isTrue(details.contains("Deadline vencido"));isTrue(details.contains("PC: 1"));equal("—",SwingTestSupport.find(h.panel[0],"buffer.semaphores",JTable.class).getValueAt(1,2));});
        }
    }
    private static void emptyAndLarge(){
        try(Harness h=new Harness(SimulationConfigTest.empty())){SessionView v=h.step();equal(1L,v.simulation().nextCycle());equal(0,v.simulation().resultCount());}
        SimulationConfig large=new SimulationConfig(500,0,new ComputerConfig[]{new ComputerConfig(1,Integer.MAX_VALUE,SchedulingPolicyType.FCFS,1),new ComputerConfig(2,10,SchedulingPolicyType.FCFS,1)},new BufferDefinition[]{new BufferDefinition(1,1,Integer.MAX_VALUE)},new ProcessDefinition[0]);
        try(Harness h=new Harness(large)){h.step();SwingTestSupport.edt(()->{equal(Integer.MAX_VALUE,SwingTestSupport.find(h.panel[0],"buffer.occupancy",JProgressBar.class).getMaximum());equal(0,SwingTestSupport.find(h.panel[0],"buffer.items",JTable.class).getRowCount());});}
    }
}
