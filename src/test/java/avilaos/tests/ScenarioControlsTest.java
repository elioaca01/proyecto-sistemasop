package avilaos.tests;

import avilaos.config.*;
import avilaos.simulation.*;
import avilaos.sync.BufferDefinition;
import avilaos.ui.*;
import avilaos.ui.session.*;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import static avilaos.tests.TestSupport.*;

public final class ScenarioControlsTest {
    static final class Picker implements ScenarioFilePicker {
        Path open,save;int opens,saves;boolean fail;
        public Path chooseOpen(Component p){opens++;if(fail)throw new IllegalStateException("Selector no disponible");return open;}
        public Path chooseSave(Component p){saves++;if(fail)throw new IllegalStateException("Selector no disponible");return save;}
    }
    static final class Harness implements AutoCloseable {
        final Picker picker=new Picker();final UiSession session;final DashboardPanel[] panel={null};final UiController[] controller={null};
        Harness(){this(new JsonConfigurationStore());}
        Harness(ConfigurationStore store){session=new UiSession(SimulationConfigTest.empty(),store);SessionTestSupport.await(session,v->v.phase()==SessionPhase.ACTIVE&&v.pendingActions()==0);SwingTestSupport.edt(()->{panel[0]=new DashboardPanel(picker);controller[0]=new UiController(session,panel[0]);panel[0].bind(controller[0]);controller[0].refresh();});}
        void click(String name){SwingTestSupport.edt(()->SwingTestSupport.find(panel[0],name,JButton.class).doClick(0));}
        SessionView done(){SessionView v=SessionTestSupport.await(session,x->x.phase()==SessionPhase.ACTIVE&&x.pendingActions()==0);SwingTestSupport.edt(controller[0]::refresh);return v;}
        SessionView step(){long cycle=session.view().simulation().nextCycle();click("toolbar.step");SessionView v=SessionTestSupport.await(session,x->x.pendingActions()==0&&x.simulation().nextCycle()==cycle+1);SwingTestSupport.edt(controller[0]::refresh);return v;}
        public void close(){SwingTestSupport.edt(controller[0]::requestClose);SessionTestSupport.close(session);SwingTestSupport.edt(controller[0]::refresh);}
    }
    public static void run(){cancelAndPickerError();draftAndLiveRoundTrip();loadingDiscardsActiveCellDraft();pendingControls();}
    private static void cancelAndPickerError(){
        try(Harness h=new Harness()){
            long generation=h.session.view().generation();h.click("scenario.load");h.click("scenario.save");equal(1,h.picker.opens);equal(1,h.picker.saves);equal(0,h.session.view().pendingActions());equal(generation,h.session.view().generation());equal(0L,h.session.view().simulation().nextCycle());
            h.picker.fail=true;h.click("scenario.load");SwingTestSupport.edt(()->isTrue(SwingTestSupport.find(h.panel[0],"dashboard.notice",JTextArea.class).getText().contains("Selector no disponible")));equal(SessionPhase.ACTIVE,h.session.view().phase());
        }
    }
    private static void draftAndLiveRoundTrip(){
        Path dir=null;
        try(Harness h=new Harness()){
            dir=Files.createTempDirectory("avilaos-controls-");Path file=dir.resolve("guardado.json"),loaded=dir.resolve("nuevo.json"),bad=dir.resolve("invalido.json");JsonConfigurationStore store=new JsonConfigurationStore();
            SwingTestSupport.edt(()->SwingTestSupport.find(h.panel[0],"config.cycleMillis",JTextField.class).setText("321"));
            h.picker.save=file;h.click("scenario.save");h.done();equal(500L,store.load(file).cycleMillis());
            h.click("config.apply");h.done();h.click("scenario.save");h.done();equal(321L,store.load(file).cycleMillis());
            SwingTestSupport.edt(()->h.controller[0].addProcess(SimulationTest.cpu(1,8)));h.done();h.step();
            SwingTestSupport.edt(()->{SwingTestSupport.find(h.panel[0],"computer.inspect",JButton.class).doClick(0);h.controller[0].addProcess(SimulationTest.cpu(2,3));h.controller[0].addBuffer(new BufferDefinition(1,1,1));});h.done();h.step();
            h.click("scenario.save");h.done();SimulationConfig saved=store.load(file);equal(1,saved.processCount());equal(0,saved.bufferCount());equal(321L,saved.cycleMillis());
            store.save(loaded,ScenarioJsonTest.read(ScenarioJsonTest.REMOTE));h.picker.open=loaded;long generation=h.session.view().generation();h.click("scenario.load");SessionView fresh=h.done();isTrue(fresh.generation()>generation);equal(0L,fresh.simulation().nextCycle());
            SwingTestSupport.edt(()->equal("Selecciona un proceso en la CPU o en una cola.",SwingTestSupport.find(h.panel[0],"pcb.details",JTextArea.class).getText()));
            h.step();long g=h.session.view().generation(),cycle=h.session.view().simulation().nextCycle();Files.writeString(bad,"{\"formatVersion\":2}");h.picker.open=bad;h.click("scenario.load");SessionView rejected=h.done();equal(g,rejected.generation());equal(cycle,rejected.simulation().nextCycle());equal(SessionPhase.ACTIVE,rejected.phase());
            SwingTestSupport.edt(()->{String msg=SwingTestSupport.find(h.panel[0],"dashboard.notice",JTextArea.class).getText();isTrue(msg.contains("invalido.json"));isTrue(msg.contains("$"));isTrue(SwingTestSupport.find(h.panel[0],"scenario.load",JButton.class).isEnabled());});
            h.click("toolbar.start");SessionTestSupport.await(h.session,v->v.simulation().state()==SimulationState.RUNNING);SwingTestSupport.edt(()->{h.controller[0].refresh();isTrue(!SwingTestSupport.find(h.panel[0],"scenario.load",JButton.class).isEnabled());isTrue(SwingTestSupport.find(h.panel[0],"scenario.save",JButton.class).isEnabled());});
            h.picker.save=file;h.click("scenario.save");h.done();ScenarioJsonTest.equivalent(fresh.initialConfig(),store.load(file));h.click("toolbar.pause");SessionTestSupport.await(h.session,v->v.pendingActions()==0&&v.simulation().state()==SimulationState.PAUSED);
        }catch(IOException e){throw new AssertionError(e);}
        finally{try{ConfigurationStoreTest.deleteTree(dir);}catch(IOException e){throw new AssertionError(e);}}
    }
    private static void loadingDiscardsActiveCellDraft(){
        Path dir=null;
        try(Harness h=new Harness()){
            dir=Files.createTempDirectory("avilaos-active-editor-");Path loaded=dir.resolve("cargado.json"),saved=dir.resolve("guardado.json");
            SimulationConfig expected=SimulationConfigTest.empty().withComputers(new ComputerConfig[]{new ComputerConfig(1,128,avilaos.scheduling.SchedulingPolicyType.FCFS,2),new ComputerConfig(2,64,avilaos.scheduling.SchedulingPolicyType.FCFS,2)});
            JsonConfigurationStore files=new JsonConfigurationStore();files.save(loaded,expected);
            SwingTestSupport.edt(()->{JTable table=SwingTestSupport.find(h.panel[0],"config.computers",JTable.class);isTrue(table.editCellAt(0,1));((JTextField)table.getEditorComponent()).setText("777");equal(10,table.getValueAt(0,1));});
            h.picker.open=loaded;h.click("scenario.load");SessionView fresh=h.done();equal(128,fresh.initialConfig().computerAt(0).ramCapacity());
            SwingTestSupport.edt(()->{JTable table=SwingTestSupport.find(h.panel[0],"config.computers",JTable.class);equal(false,table.isEditing());equal(128,table.getValueAt(0,1));});
            h.click("config.apply");equal(128,h.done().initialConfig().computerAt(0).ramCapacity());
            h.picker.save=saved;h.click("scenario.save");h.done();ScenarioJsonTest.equivalent(expected,files.load(saved));
        }catch(IOException e){throw new AssertionError(e);}
        finally{try{ConfigurationStoreTest.deleteTree(dir);}catch(IOException e){throw new AssertionError(e);}}
    }
    private static void pendingControls(){
        UiSessionPersistenceTest.Store store=new UiSessionPersistenceTest.Store();store.blockLoad=true;
        try(Harness h=new Harness(store)){
            try{
            h.picker.open=UiSessionPersistenceTest.FILE;h.click("scenario.load");isTrue(store.entered.tryAcquire(2,TimeUnit.SECONDS));
            SwingTestSupport.edt(()->{h.controller[0].refresh();isTrue(!SwingTestSupport.find(h.panel[0],"scenario.load",JButton.class).isEnabled());isTrue(!SwingTestSupport.find(h.panel[0],"scenario.save",JButton.class).isEnabled());isTrue(!SwingTestSupport.find(h.panel[0],"dashboard.notice",JTextArea.class).getText().contains("Escenario cargado"));});
            store.release.release();h.done();SwingTestSupport.edt(()->isTrue(SwingTestSupport.find(h.panel[0],"dashboard.notice",JTextArea.class).getText().contains("Escenario cargado")));
            }finally{store.release.release();}
        }catch(InterruptedException e){throw new AssertionError(e);}
        finally{store.release.release();}
    }
}
