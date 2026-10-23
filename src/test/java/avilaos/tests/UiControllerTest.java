package avilaos.tests;
import avilaos.ui.*;
import avilaos.ui.session.*;
import avilaos.simulation.*;
import javax.swing.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import static avilaos.tests.TestSupport.*;
public final class UiControllerTest {
    public static void run(){
        UiSession session=new UiSession(SimulationConfigTest.empty().withAddedProcess(SimulationTest.cpu(1,3)));UiController[] controller={null};DashboardPanel[] panel={null};
        try{
            SessionTestSupport.await(session,v->v.phase()==SessionPhase.ACTIVE&&v.pendingActions()==0);
            SwingTestSupport.edt(()->{panel[0]=new DashboardPanel();controller[0]=new UiController(session,panel[0]);panel[0].bind(controller[0]);controller[0].start();controller[0].refresh();equal(4,SwingTestSupport.find(panel[0],"dashboard.tabs",JTabbedPane.class).getTabCount());isTrue(SwingTestSupport.find(panel[0],"toolbar.step",JButton.class).isEnabled());SwingTestSupport.find(panel[0],"toolbar.step",JButton.class).doClick();});
            SessionView old=SessionTestSupport.await(session,v->v.simulation().nextCycle()==1);
            SwingTestSupport.edt(()->{controller[0].refresh();isTrue(SwingTestSupport.find(panel[0],"dashboard.cycle",JLabel.class).getText().contains("1"));isTrue(!SwingTestSupport.find(panel[0],"config.apply",JButton.class).isEnabled());});
            long g=old.generation();Semaphore heartbeat=new Semaphore(0);
            SwingTestSupport.edt(()->{SwingTestSupport.find(panel[0],"toolbar.restart",JButton.class).doClick();SwingUtilities.invokeLater(heartbeat::release);});
            try{isTrue(heartbeat.tryAcquire(2,TimeUnit.SECONDS));}catch(InterruptedException e){throw new AssertionError(e);}
            SessionTestSupport.await(session,v->v.generation()>g&&v.phase()==SessionPhase.ACTIVE&&v.pendingActions()==0);
            SwingTestSupport.edt(()->{
                controller[0].refresh();controller[0].accept(old);equal("Ciclo 0",SwingTestSupport.find(panel[0],"dashboard.cycle",JLabel.class).getText());
                JTextField ms=SwingTestSupport.find(panel[0],"config.cycleMillis",JTextField.class);ms.setText("321");
            });
            session.addProcess(SimulationTest.cpu(2,2));SessionTestSupport.await(session,v->v.pendingActions()==0);
            SwingTestSupport.edt(()->{controller[0].refresh();equal("321",SwingTestSupport.find(panel[0],"config.cycleMillis",JTextField.class).getText());SwingTestSupport.find(panel[0],"toolbar.start",JButton.class).doClick();});
            SessionTestSupport.await(session,v->v.simulation().state()==SimulationState.RUNNING);
            SwingTestSupport.edt(()->{controller[0].refresh();isTrue(!SwingTestSupport.find(panel[0],"toolbar.step",JButton.class).isEnabled());SwingTestSupport.find(panel[0],"toolbar.pause",JButton.class).doClick();});
            SessionTestSupport.await(session,v->v.simulation().state()==SimulationState.PAUSED&&v.pendingActions()==0);
            session.addProcess(SimulationTest.cpu(9,1));SessionTestSupport.await(session,v->v.pendingActions()==0);session.step();SessionView rejected=SessionTestSupport.await(session,v->{SimulationSnapshot s=v.simulation();return s.resultCount()>0&&!s.resultAt(s.resultCount()-1).accepted();});
            SwingTestSupport.edt(()->{controller[0].refresh();isTrue(SwingTestSupport.find(panel[0],"events.results",JTable.class).getRowCount()>0);controller[0].requestClose();});
            try{isTrue(session.awaitClosed(3000));}catch(InterruptedException e){throw new AssertionError(e);}
            SwingTestSupport.edt(()->{controller[0].refresh();isTrue(!SwingTestSupport.find(panel[0],"toolbar.start",JButton.class).isEnabled());});
        }finally{if(controller[0]!=null)SwingTestSupport.edt(controller[0]::requestClose);SessionTestSupport.close(session);if(controller[0]!=null)SwingTestSupport.edt(controller[0]::refresh);}
    }
}
