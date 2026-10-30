package avilaos.tests;
import avilaos.ui.EventPanel;
import avilaos.ui.session.*;
import avilaos.simulation.*;
import javax.swing.*;
import static avilaos.tests.TestSupport.*;
public final class EventPanelTest {
    public static void run(){
        try(Simulation sim=new Simulation(SimulationTest.configs(),100)){
            sim.enqueue(new AddProcessCommand(SimulationTest.cpu(1,1)));sim.enqueue(new AddProcessCommand(SimulationTest.cpu(9,1)));SimulationSnapshot v=BufferSimulationTest.step(sim);
            SwingTestSupport.edt(()->{EventPanel p=new EventPanel();p.render(v);JTable events=SwingTestSupport.find(p,"events.log",JTable.class);equal(v.eventCount(),events.getRowCount());events.setRowSelectionInterval(0,0);p.render(v);equal(v.eventCount(),events.getRowCount());equal(0,events.getSelectedRow());equal(2,SwingTestSupport.find(p,"events.results",JTable.class).getRowCount());});
        }
        UiSession session=new UiSession(SimulationConfigTest.empty());
        try{
            SessionTestSupport.await(session,v->v.phase()==SessionPhase.ACTIVE);session.addProcess(SimulationTest.cpu(1,3));SessionTestSupport.await(session,v->v.pendingActions()==0);session.step();SessionTestSupport.await(session,v->v.simulation().nextCycle()==1);
            session.addProcess(SimulationTest.cpu(2,1));SessionView pending=SessionTestSupport.await(session,v->v.pendingActions()==0);
            EventPanel[] panel={null};SwingTestSupport.edt(()->{panel[0]=new EventPanel();panel[0].renderSession(pending);isTrue(SwingTestSupport.find(panel[0],"events.pending",JLabel.class).getText().contains("Pendientes: 1"));});
            long g=pending.generation();session.restart();SessionView reset=SessionTestSupport.await(session,v->v.generation()>g&&v.phase()==SessionPhase.ACTIVE&&v.pendingActions()==0);
            SwingTestSupport.edt(()->{panel[0].renderSession(reset);isTrue(SwingTestSupport.find(panel[0],"events.pending",JLabel.class).getText().contains("Pendientes: 1"));equal(0,SwingTestSupport.find(panel[0],"events.results",JTable.class).getRowCount());});
        }finally{SessionTestSupport.close(session);}
    }
}
