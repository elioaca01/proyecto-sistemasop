package avilaos.ui;
import avilaos.config.SimulationConfig;
import avilaos.simulation.ComputerConfig;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.process.ProcessDefinition;
import avilaos.sync.BufferDefinition;
import avilaos.ui.session.*;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
/** Ensambla la vista y delega acciones; el cierre del motor ocurre fuera del EDT. */
public final class MainWindow extends JFrame implements UiView {
    private final DashboardPanel dashboard;private final UiController controller;
    public MainWindow(){this(new SimulationConfig(500,2,new ComputerConfig[]{new ComputerConfig(1,64,SchedulingPolicyType.FCFS,2),new ComputerConfig(2,64,SchedulingPolicyType.FCFS,2)},new BufferDefinition[0],new ProcessDefinition[0]));}
    public MainWindow(SimulationConfig config){
        super("ÁvilaOS · Simulador de sistemas operativos");UiTheme.apply();dashboard=new DashboardPanel();controller=new UiController(new UiSession(config),this);dashboard.bind(controller);setContentPane(dashboard);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);setSize(1180,780);setMinimumSize(new Dimension(960,640));setLocationRelativeTo(null);addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){controller.requestClose();}});controller.start();
    }
    public void render(SessionView view){dashboard.render(view);}public void showNotice(SessionNotice notice){dashboard.showNotice(notice);}public void closed(){dashboard.closed();dispose();}
}
