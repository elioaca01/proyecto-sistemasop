package avilaos.tests;

import avilaos.config.*;
import avilaos.simulation.*;
import avilaos.ui.session.*;
import java.io.IOException;
import java.nio.file.*;
import javax.swing.*;
import static avilaos.tests.TestSupport.*;

/** Los mismos archivos que abrirá el estudiante se cargan por los botones reales. */
public final class ScenarioIntegrationTest {
    private static SessionView load(ScenarioControlsTest.Harness h,String file){h.picker.open=Path.of("examples",file+".json");h.click("scenario.load");return h.done();}
    public static void run(){localSavedAndReloaded();remote();ioAndPolicies();}
    private static void local(ScenarioControlsTest.Harness h){
        long[] who={1,2,2,2,2,2,2,1,1,1,1,1};
        for(int i=0;i<12;i++){SessionView v=h.step();equal(i+1L,v.simulation().nextCycle());equal(who[i],v.simulation().computerAt(0).lastProcessId());}
        SimulationSnapshot end=h.session.view().simulation();equal(2,end.computerAt(0).terminatedCount());equal(1,end.computerAt(0).ramUsed());equal(1L,end.bufferAt(0).produced());equal(1L,end.bufferAt(0).consumed());equal(0,end.bufferAt(0).size());
        SwingTestSupport.edt(()->{JTable sem=SwingTestSupport.find(h.panel[0],"buffer.semaphores",JTable.class);equal(1,sem.getValueAt(0,1));equal(0,sem.getValueAt(1,1));equal(1,sem.getValueAt(2,1));});
    }
    private static void localSavedAndReloaded(){
        Path dir=null;
        try(ScenarioControlsTest.Harness h=new ScenarioControlsTest.Harness()){
            SessionView initial=load(h,"local-producer-consumer");equal(0L,initial.simulation().nextCycle());equal(2,initial.initialConfig().processCount());local(h);
            dir=Files.createTempDirectory("avilaos-roundtrip-");Path saved=dir.resolve("otra ejecución.json");h.picker.save=saved;h.click("scenario.save");h.done();ScenarioJsonTest.equivalent(initial.initialConfig(),new JsonConfigurationStore().load(saved));
            Path invalid=dir.resolve("corrupto.json");Files.writeString(invalid,"{\"formatVersion\":1,\"computers\":[");h.picker.open=invalid;long generation=h.session.view().generation();h.click("scenario.load");SessionView error=h.done();equal(generation,error.generation());equal(12L,error.simulation().nextCycle());isTrue(error.noticeAt(error.noticeCount()-1).error());h.step();equal(13L,h.session.view().simulation().nextCycle());
            h.picker.open=saved;h.click("scenario.load");SessionView reset=h.done();isTrue(reset.generation()>generation);equal(0L,reset.simulation().nextCycle());local(h);
        }catch(IOException e){throw new AssertionError(e);}
        finally{try{ConfigurationStoreTest.deleteTree(dir);}catch(IOException e){throw new AssertionError(e);}}
    }
    private static void remote(){
        try(ScenarioControlsTest.Harness h=new ScenarioControlsTest.Harness()){
            load(h,"remote-producer");SwingTestSupport.edt(()->SwingTestSupport.find(h.panel[0],"computer.selector",JComboBox.class).setSelectedItem(2));
            int[] pc={1,1,1,2,3,4,5,6};
            for(int t=0;t<pc.length;t++){
                ComputerSnapshot c=h.step().simulation().computerAt(1);ProcessSnapshot p=c.running()!=null?c.running():c.blockedCount()>0?c.blockedAt(0):c.terminatedAt(0);equal(pc[t],p.pc());
                if(t==1||t==2)SwingTestSupport.edt(()->{JTable table=SwingTestSupport.find(h.panel[0],"computer.blocked",JTable.class);equal(1,table.getValueAt(0,4));equal("Red",table.getValueAt(0,9));});
            }
            SimulationSnapshot end=h.session.view().simulation();equal(1L,end.bufferAt(0).produced());equal(1,end.bufferAt(0).size());equal(0,end.computerAt(1).ramUsed());equal(1,end.computerAt(1).terminatedCount());
        }
    }
    private static void ioAndPolicies(){
        try(ScenarioControlsTest.Harness h=new ScenarioControlsTest.Harness()){
            SessionView initial=load(h,"io-and-scheduling");equal(4,initial.simulation().computerCount());equal(8,initial.initialConfig().processCount());
            SimulationSnapshot one=h.step().simulation();long[] first={1,4,5,8};for(int i=0;i<4;i++){equal(first[i],one.computerAt(i).lastProcessId());equal(initial.initialConfig().computerAt(i).policy(),one.computerAt(i).policyType());}
            equal(6L,h.step().simulation().computerAt(2).lastProcessId());for(int i=2;i<40;i++)h.step();
            SimulationSnapshot end=h.session.view().simulation();int completed=0;for(int i=0;i<end.computerCount();i++){completed+=end.computerAt(i).terminatedCount();equal(0,end.computerAt(i).blockedCount());equal(0,end.computerAt(i).ramUsed());}equal(8,completed);equal(null,end.error());
        }
    }
}
