package avilaos.tests;

import avilaos.config.SimulationConfig;
import avilaos.metrics.*;
import avilaos.process.*;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.simulation.*;
import avilaos.ui.*;
import avilaos.ui.session.*;
import java.awt.Component;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import static avilaos.tests.TestSupport.*;

public final class MetricsIntegrationTest {
    static void load(ScenarioControlsTest.Harness h,String name){h.picker.open=Path.of("examples",name+".json");h.click("scenario.load");h.done();}
    public static void run(){scenariosAndReset();ioAndDeadlines();sustained();}
    private static void scenariosAndReset(){
        try(ScenarioControlsTest.Harness h=new ScenarioControlsTest.Harness()){
            load(h,"local-producer-consumer");for(int n=0;n<12;n++)h.step();SimulationSnapshot end=h.session.view().simulation();PerformanceMetrics m=end.metrics().global();
            equal(12L,m.cycles());equal(2L,m.completed());equal(0L,m.expired());equal(2L,m.userCycles());equal(10L,m.kernelCycles());PerformanceMetricsTest.near(.5,m.utilization());PerformanceMetricsTest.near(2.0/12,m.throughput());PerformanceMetricsTest.near(.5,m.averageResponse());PerformanceMetricsTest.near(1,m.fairness());PerformanceMetricsTest.near(6,end.metrics().waits().averageWait());equal(12,end.sampleCount());
            SwingTestSupport.edt(()->h.controller[0].changeCycleDuration(7));h.done();SimulationSnapshot slower=h.step().simulation();equal(7L,slower.cycleMillis());PerformanceMetricsTest.near(12.0/26,slower.metrics().global().utilization());PerformanceMetricsTest.near(2.0/13,slower.metrics().global().throughput());
            h.click("toolbar.restart");SessionView reset=h.done();equal(0L,reset.simulation().metrics().global().cycles());equal(0,reset.simulation().sampleCount());equal(0L,reset.simulation().metrics().waits().completedEpisodes());
            load(h,"remote-producer");equal(0,h.session.view().simulation().sampleCount());for(int n=0;n<8;n++)h.step();SimulationSnapshot remote=h.session.view().simulation();m=remote.metrics().global();equal(1L,m.completed());equal(1L,m.userCycles());equal(5L,m.kernelCycles());PerformanceMetricsTest.near(.375,m.utilization());PerformanceMetricsTest.near(1.0/8,m.throughput());isTrue(Double.isNaN(remote.metrics().waits().averageWait()));
        }
    }
    private static void ioAndDeadlines(){
        try(Simulation s=SemaphoreWaitMetricsTest.create(SemaphoreWaitMetricsTest.example("io-and-scheduling"))){for(int n=1;n<=40;n++)SemaphoreWaitMetricsTest.step(s,n);PerformanceMetrics m=s.snapshot().metrics().global();equal(8L,m.completed());equal(0L,m.expired());equal(39L,m.userCycles()+m.kernelCycles());equal(8L,m.responded());equal(0L,s.snapshot().metrics().waits().completedEpisodes());PerformanceMetricsTest.near(39.0/160,m.utilization());}
        try(Simulation s=new Simulation(SimulationTest.configs(),500)){
            s.enqueue(new AddProcessCommand(new ProcessDefinition("vence",1,ProcessType.CPU_BOUND,3,4,0,2,0,0,0,0,0)));s.enqueue(new AddProcessCommand(SimulationTest.cpu(2,1)));SemaphoreWaitMetricsTest.step(s,1);PerformanceMetrics m=SemaphoreWaitMetricsTest.step(s,2).metrics().global();equal(1L,m.completed());equal(1L,m.expired());equal(0L,m.pending());PerformanceMetricsTest.near(.5,m.deadlineCompliance());PerformanceMetricsTest.near(.9,m.fairness());
        }
        try(Simulation fast=SemaphoreWaitMetricsTest.create(SemaphoreWaitMetricsTest.example("remote-producer").withTiming(1,2));Simulation slow=SemaphoreWaitMetricsTest.create(SemaphoreWaitMetricsTest.example("remote-producer").withTiming(900,2))){for(int n=1;n<=8;n++){SemaphoreWaitMetricsTest.step(fast,n);SemaphoreWaitMetricsTest.step(slow,n);}equal(fast.snapshot().metrics().global(),slow.snapshot().metrics().global());}
    }
    private static ProcessDefinition longCpu(int machine,String name){return new ProcessDefinition(name,machine,ProcessType.CPU_BOUND,300,4,0,2000,0,0,0,0,0);}
    private static void sustained(){
        ComputerConfig[] machines={new ComputerConfig(1,64,SchedulingPolicyType.FCFS,2),new ComputerConfig(2,64,SchedulingPolicyType.ROUND_ROBIN,2),new ComputerConfig(9,64,SchedulingPolicyType.EDF,2)};
        try(Simulation s=new Simulation(machines,1)){
            s.enqueue(new AddProcessCommand(longCpu(1,"A")));s.enqueue(new AddProcessCommand(longCpu(1,"B")));s.enqueue(new AddProcessCommand(longCpu(2,"C")));s.enqueue(new AddProcessCommand(longCpu(2,"D")));s.enqueue(new AddProcessCommand(longCpu(9,"E")));
            SimulationSnapshot retained=SemaphoreWaitMetricsTest.step(s,1);for(int n=2;n<=500;n++)SemaphoreWaitMetricsTest.step(s,n);SimulationSnapshot end=s.snapshot();equal(500,end.sampleCount());PerformanceMetrics m=end.metrics().global();equal(2L,m.completed());equal(3L,m.pending());equal(1300L,m.userCycles());PerformanceMetricsTest.near(1300.0/1500,m.utilization());PerformanceMetricsTest.near(1690000.0/1725000,m.fairness());equal(1,retained.sampleCount());equal(3L,retained.metrics().global().userCycles());
            SwingTestSupport.edt(()->{MetricsPanel panel=new MetricsPanel();panel.render(end);MetricsPanelTest.paint(panel,960,640);isTrue(SwingTestSupport.find(panel,"metrics.chart",UtilizationChart.class).getAccessibleContext().getAccessibleDescription().contains("Muestras: 500"));});
        }
    }
    /** Renderizado headless reproducible de los componentes reales; no es captura nativa. */
    public static void main(String[] args)throws Exception{
        System.setProperty("java.awt.headless","true");SwingTestSupport.edt(UiTheme::apply);Path dir=Path.of(args[0]);Files.createDirectories(dir);
        try(ScenarioControlsTest.Harness h=new ScenarioControlsTest.Harness()){
            load(h,"local-producer-consumer");h.step();SwingTestSupport.edt(()->write(SwingTestSupport.find(h.panel[0],"metrics.chart",UtilizationChart.class),760,300,dir.resolve("module8-one-cycle.png")));
            for(int n=1;n<12;n++)h.step();SwingTestSupport.edt(()->{JTabbedPane tabs=SwingTestSupport.find(h.panel[0],"dashboard.tabs",JTabbedPane.class);tabs.setSelectedIndex(tabs.indexOfTab("Métricas"));write(h.panel[0],1180,780,dir.resolve("module8-local-metrics.png"));write(h.panel[0],960,640,dir.resolve("module8-minimum-metrics.png"));});
        }
    }
    private static void write(Component component,int width,int height,Path path){try{ImageIO.write(MetricsPanelTest.paint(component,width,height),"png",path.toFile());}catch(java.io.IOException e){throw new AssertionError(e);}}
}
