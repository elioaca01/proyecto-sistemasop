package avilaos.tests;

import avilaos.metrics.*;
import avilaos.simulation.*;
import avilaos.sync.BufferSnapshot;
import avilaos.ui.*;
import avilaos.ui.model.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import static avilaos.tests.TestSupport.*;

public final class MetricsPanelTest {
    static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container container)layout(container);}
    static BufferedImage paint(Component c,int width,int height){c.setSize(width,height);if(c instanceof Container container)for(int i=0;i<4;i++)layout(container);BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();try{c.paint(g);}finally{g.dispose();}return image;}
    public static void run(){models();dashboardAndLayout();chart();distinctLegendsForSevenComputers();}
    private static void models(){
        try(Simulation s=SemaphoreWaitMetricsTest.create(SemaphoreWaitMetricsTest.example("local-producer-consumer"))){
            MetricsTableModel machines=new MetricsTableModel();machines.setSnapshot(s.snapshot());equal(3,machines.getRowCount());equal("Sin datos",machines.getValueAt(0,4));equal("Sin datos",machines.getValueAt(0,5));isTrue(!machines.isCellEditable(0,0));
            SimulationSnapshot first=SemaphoreWaitMetricsTest.step(s,1),second=SemaphoreWaitMetricsTest.step(s,2);
            BufferMetricsTableModel buffers=new BufferMetricsTableModel();buffers.setSnapshot(second);equal(2,buffers.getRowCount());equal(1L,buffers.getValueAt(1,8));
            SimulationSnapshot failed=new SimulationSnapshot(99,1,SimulationState.FAILED,500,"parcial",new ComputerSnapshot[]{second.computerAt(0),second.computerAt(1)},new CommandResult[0],2,new BufferSnapshot[]{second.bufferAt(0)},new SimulationEvent[0],first.metrics(),new UtilizationSample[]{first.sampleAt(0)});
            buffers.setSnapshot(failed);equal(0L,buffers.getValueAt(1,8));
            for(int n=3;n<=12;n++)SemaphoreWaitMetricsTest.step(s,n);SimulationSnapshot end=s.snapshot();machines.setSnapshot(end);equal("Sistema",machines.getValueAt(0,0));equal("Computador 1",machines.getValueAt(1,0));equal(2L,machines.getValueAt(0,1));equal("0.167",machines.getValueAt(0,4));equal("50.00",machines.getValueAt(0,5));equal("100.00",machines.getValueAt(1,5));equal("0.00",machines.getValueAt(2,5));equal("0.500",machines.getValueAt(0,8));equal("1.000",machines.getValueAt(0,10));
            buffers.setSnapshot(end);equal(1L,buffers.getValueAt(1,1));equal(1L,buffers.getValueAt(1,2));equal(0L,buffers.getValueAt(1,3));equal("6.000",buffers.getValueAt(1,6));equal(0L,buffers.getValueAt(1,7));isTrue(!buffers.isCellEditable(1,1));machines.setSnapshot(null);buffers.setSnapshot(null);equal(0,machines.getRowCount());equal(0,buffers.getRowCount());
        }
    }
    private static void dashboardAndLayout(){
        try(ScenarioControlsTest.Harness h=new ScenarioControlsTest.Harness()){
            h.picker.open=java.nio.file.Path.of("examples/local-producer-consumer.json");h.click("scenario.load");h.done();h.step();
            SwingTestSupport.edt(()->{
                JTabbedPane tabs=SwingTestSupport.find(h.panel[0],"dashboard.tabs",JTabbedPane.class);int index=tabs.indexOfTab("Métricas");isTrue(index>=0);tabs.setSelectedIndex(index);paint(h.panel[0],960,640);
                JTable table=SwingTestSupport.find(h.panel[0],"metrics.computers",JTable.class);equal(3,table.getRowCount());JScrollPane scroll=SwingTestSupport.find(h.panel[0],"metrics.computerScroll",JScrollPane.class);isTrue(scroll.getViewport().getHeight()>=40);isTrue(scroll.getHorizontalScrollBar().getMaximum()>scroll.getHorizontalScrollBar().getVisibleAmount());
                UtilizationChart chart=SwingTestSupport.find(h.panel[0],"metrics.chart",UtilizationChart.class);isTrue(chart.getAccessibleContext().getAccessibleDescription().contains("Muestras: 1"));
            });
            h.click("toolbar.restart");h.done();SwingTestSupport.edt(()->{JTable table=SwingTestSupport.find(h.panel[0],"metrics.computers",JTable.class);equal("Sin datos",table.getValueAt(0,5));isTrue(SwingTestSupport.find(h.panel[0],"metrics.chart",UtilizationChart.class).getAccessibleContext().getAccessibleDescription().contains("Muestras: 0"));});
        }
    }
    private static void distinctLegendsForSevenComputers(){
        ComputerConfig[] configs=new ComputerConfig[7];for(int i=0;i<configs.length;i++)configs[i]=new ComputerConfig(i+1,10,avilaos.scheduling.SchedulingPolicyType.FCFS,2);
        try(Simulation sim=new Simulation(configs,500)){
            for(int i=0;i<7;i++)sim.enqueue(new AddProcessCommand(SimulationTest.cpu(i+1,i+1)));
            for(int n=1;n<=8;n++)SemaphoreWaitMetricsTest.step(sim,n);SimulationSnapshot snapshot=sim.snapshot();
            SwingTestSupport.edt(()->{
                UtilizationChart chart=new UtilizationChart();chart.setSnapshot(snapshot);BufferedImage image=paint(chart,760,300);
                // A 760px la leyenda tiene cuatro columnas: P1 y P7 deben tener marcas distintas.
                boolean same=true;for(int x=0;x<24;x++)if(image.getRGB(64+x,39)!=image.getRGB(374+x,59)){same=false;break;}
                equal(false,same);
            });
        }
    }
    private static void chart(){
        SwingTestSupport.edt(()->{
            UtilizationChart chart=new UtilizationChart();paint(chart,760,300);ComputerSnapshot[] cs={ComputerSnapshot.capture(new Computer(2,10)),ComputerSnapshot.capture(new Computer(9,10))};UtilizationHistory history=new UtilizationHistory();
            for(int n=1;n<=500;n++)history.record(MetricsSnapshot.calculate(n,cs,new BufferSnapshot[0]));MetricsSnapshot m=MetricsSnapshot.calculate(500,cs,new BufferSnapshot[0]);
            chart.setSnapshot(new SimulationSnapshot(1,500,SimulationState.PAUSED,1,null,cs,new CommandResult[0],0,new BufferSnapshot[0],new SimulationEvent[0],m,history.copy()));paint(chart,760,300);String description=chart.getAccessibleContext().getAccessibleDescription();isTrue(description.contains("2, 9"));isTrue(description.contains("Muestras: 500"));chart.setSnapshot(null);paint(chart,300,180);isTrue(chart.getAccessibleContext().getAccessibleDescription().contains("Muestras: 0"));
        });
    }
}
