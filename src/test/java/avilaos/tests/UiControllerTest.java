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
        minimumWindowKeepsQueuesUsable();
        delayedCloseRetainsOwnership();
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
    private static void minimumWindowKeepsQueuesUsable(){
        UiSession session=new UiSession(SimulationConfigTest.empty());
        try{
            SessionView view=SessionTestSupport.await(session,v->v.phase()==SessionPhase.ACTIVE&&v.pendingActions()==0);
            SwingTestSupport.edt(()->{
                UiTheme.apply();DashboardPanel panel=new DashboardPanel();panel.render(view);panel.setSize(960,612);
                layoutTree(panel);
                JTable queue=SwingTestSupport.find(panel,"computer.new",JTable.class);JViewport viewport=(JViewport)queue.getParent();
                if(viewport.getHeight()<queue.getRowHeight()*3)throw new AssertionError("Cola sin espacio en ventana mínima: "+viewport.getHeight());
                isTrue(viewport.getHeight()>=queue.getRowHeight()*3);
                ComputerPanel computer=(ComputerPanel)SwingUtilities.getAncestorOfClass(ComputerPanel.class,queue);
                JViewport outer=(JViewport)computer.getParent();JScrollPane scroll=(JScrollPane)outer.getParent();
                isTrue(scroll.getVerticalScrollBar().isVisible());outer.setViewPosition(new java.awt.Point(0,computer.getHeight()));
                isTrue(outer.getViewPosition().y>0);
                panel.setSize(1180,752);layoutTree(panel);isTrue(viewport.getHeight()>=queue.getRowHeight()*3);
            });
        }finally{SessionTestSupport.close(session);}
    }
    private static void layoutTree(java.awt.Container parent){parent.doLayout();for(java.awt.Component c:parent.getComponents())if(c instanceof java.awt.Container child)layoutTree(child);}
    private static void delayedCloseRetainsOwnership(){
        UiSession session=new UiSession(SimulationConfigTest.empty());Semaphore gate=null;UiController[] controller={null};boolean[] closed={false};
        try{
            SessionTestSupport.await(session,v->v.phase()==SessionPhase.ACTIVE&&v.pendingActions()==0);
            // Retener la publicación final reproduce una demora real sin depender del rendimiento.
            var engineField=UiSession.class.getDeclaredField("engine");engineField.setAccessible(true);Simulation engine=(Simulation)engineField.get(session);
            var eventsField=Simulation.class.getDeclaredField("events");eventsField.setAccessible(true);EventLog events=(EventLog)eventsField.get(engine);
            var mutexField=EventLog.class.getDeclaredField("mutex");mutexField.setAccessible(true);gate=(Semaphore)mutexField.get(events);gate.acquire();
            Semaphore heartbeat=new Semaphore(0);
            SwingTestSupport.edt(()->{controller[0]=new UiController(session,new UiView(){public void render(SessionView v){}public void showNotice(SessionNotice n){}public void closed(){closed[0]=true;}});controller[0].requestClose();SwingUtilities.invokeLater(heartbeat::release);});
            isTrue(heartbeat.tryAcquire(1,TimeUnit.SECONDS));
            isTrue(!new Semaphore(0).tryAcquire(2200,TimeUnit.MILLISECONDS));
            equal(SessionPhase.CLOSING,session.view().phase());isTrue(!session.awaitClosed(20));
            SwingTestSupport.edt(()->{controller[0].requestClose();controller[0].refresh();isTrue(!closed[0]);});
            gate.release();gate=null;isTrue(session.awaitClosed(3000));
            SwingTestSupport.edt(()->{controller[0].refresh();isTrue(closed[0]);});equal(SessionPhase.CLOSED,session.view().phase());
        }catch(ReflectiveOperationException|InterruptedException e){throw new AssertionError(e);}
        finally{if(gate!=null)gate.release();SessionTestSupport.close(session);}
    }
}
