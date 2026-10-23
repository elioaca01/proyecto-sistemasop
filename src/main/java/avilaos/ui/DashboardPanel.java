package avilaos.ui;
import avilaos.config.SimulationConfig;
import avilaos.simulation.*;
import avilaos.sync.BufferDefinition;
import avilaos.ui.session.*;
import java.awt.*;
import javax.swing.*;
/** Superficie de la ventana, reutilizable sin JFrame para probar controles reales en EDT. */
public final class DashboardPanel extends JPanel implements UiView {
    private final JLabel cycle=new JLabel("Ciclo 0"),state=new JLabel("Preparando"),metadata=new JLabel();private final JTextArea notice=new JTextArea("Configura la carga inicial o agrega procesos y avanza un ciclo.",2,50);
    private final JButton start=new JButton("Iniciar / continuar"),pause=new JButton("Pausar"),step=new JButton("Paso"),restart=new JButton("Reiniciar"),addProcess=new JButton("Agregar proceso"),addBuffer=new JButton("Agregar buffer"),applyDuration=new JButton("Aplicar duración");
    private final JTextField duration=new JTextField("500",6);private final JComboBox<Integer> computers=new JComboBox<>();
    private final ComputerPanel computerPanel=new ComputerPanel();private final BufferPanel bufferPanel=new BufferPanel();private final EventPanel eventPanel=new EventPanel();private final ConfigurationPanel configuration=new ConfigurationPanel();private final JTabbedPane tabs=new JTabbedPane();
    private UiController controller;private SessionView latest;private SimulationConfig shownConfig;private long generation=-1,lastDuration=-1;private boolean rendering,durationEdited,updatingDuration;
    public DashboardPanel(){
        super(new BorderLayout(0,14));setBorder(BorderFactory.createEmptyBorder(16,20,12,20));setBackground(UiTheme.BACKGROUND);
        JLabel title=new JLabel("ÁvilaOS");title.setFont(new Font(Font.DIALOG,Font.BOLD,24));title.setForeground(UiTheme.INK);JLabel subtitle=new JLabel("Simulador de sistemas operativos distribuidos");subtitle.setForeground(new Color(0x557080));
        JPanel brand=new JPanel(new GridLayout(0,1));brand.setOpaque(false);brand.add(title);brand.add(subtitle);JPanel clock=new JPanel(new GridLayout(0,1));clock.setOpaque(false);cycle.setFont(new Font(Font.MONOSPACED,Font.BOLD,22));cycle.setName("dashboard.cycle");state.setName("dashboard.state");clock.add(cycle);clock.add(state);
        JPanel heading=new JPanel(new BorderLayout());heading.setOpaque(false);heading.add(brand,BorderLayout.WEST);heading.add(clock,BorderLayout.EAST);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEADING,6,4));buttons.setOpaque(false);JButton[] all={start,pause,step,restart,addProcess,addBuffer};String[] names={"start","pause","step","restart","addProcess","addBuffer"};for(int i=0;i<all.length;i++){all[i].setName("toolbar."+names[i]);buttons.add(all[i]);}
        restart.setToolTipText("Vuelve al escenario inicial; las altas y cambios de esta ejecución no se conservan.");
        JPanel timing=new JPanel(new FlowLayout(FlowLayout.LEADING,6,0));timing.setOpaque(false);timing.add(new JLabel("Ciclo (ms)"));timing.add(duration);timing.add(applyDuration);timing.add(metadata);duration.setName("toolbar.duration");applyDuration.setName("toolbar.applyDuration");
        JPanel tools=new JPanel(new BorderLayout(0,6));tools.setOpaque(false);tools.add(buttons,BorderLayout.NORTH);tools.add(timing,BorderLayout.SOUTH);JPanel header=new JPanel(new BorderLayout(0,12));header.setOpaque(false);header.add(heading,BorderLayout.NORTH);header.add(tools,BorderLayout.SOUTH);add(header,BorderLayout.NORTH);
        JPanel machine=new JPanel(new BorderLayout(0,10));JPanel choose=new JPanel(new FlowLayout(FlowLayout.LEADING,8,6));choose.add(new JLabel("Computador"));choose.add(computers);computers.setName("computer.selector");machine.add(choose,BorderLayout.NORTH);machine.add(computerPanel,BorderLayout.CENTER);
        tabs.setName("dashboard.tabs");tabs.addTab("Computadores",machine);tabs.addTab("Buffers y semáforos",bufferPanel);tabs.addTab("Configuración inicial",new JScrollPane(configuration));tabs.addTab("Registro",eventPanel);add(tabs,BorderLayout.CENTER);
        notice.setName("dashboard.notice");notice.setEditable(false);notice.setLineWrap(true);notice.setWrapStyleWord(true);notice.setOpaque(false);notice.setForeground(UiTheme.INK);add(notice,BorderLayout.SOUTH);
        computers.addActionListener(e->{if(!rendering)renderComputer();});
        duration.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){changed();}public void removeUpdate(javax.swing.event.DocumentEvent e){changed();}public void changedUpdate(javax.swing.event.DocumentEvent e){changed();}private void changed(){if(!updatingDuration)durationEdited=true;}});
    }
    public void bind(UiController controller){
        this.controller=controller;start.addActionListener(e->controller.resume());pause.addActionListener(e->controller.pause());step.addActionListener(e->controller.step());restart.addActionListener(e->controller.restart());
        addProcess.addActionListener(e->processDialog());addBuffer.addActionListener(e->bufferDialog());configuration.setApplyHandler(controller::replaceScenario);computerPanel.setSchedulingHandler(controller::changeScheduling);
        applyDuration.addActionListener(e->{try{long ms=InputParser.longInteger(duration.getText(),"Duración del ciclo",1);durationEdited=false;controller.changeCycleDuration(ms);}catch(IllegalArgumentException failure){showNotice(new SessionNotice(-1,-1,true,failure.getMessage()));}});
    }
    public void render(SessionView view){
        boolean newGeneration=generation!=view.generation();latest=view;
        if(newGeneration){generation=view.generation();computerPanel.clearSelection();durationEdited=false;lastDuration=-1;}
        if(newGeneration||shownConfig!=view.initialConfig()){
            if(newGeneration||!configuration.hasDraftChanges())configuration.load(view.initialConfig());else configuration.updateInitialLoads(view.initialConfig());shownConfig=view.initialConfig();
        }
        SimulationSnapshot s=view.simulation();Object selected=computers.getSelectedItem();rendering=true;
        try{computers.removeAllItems();if(s!=null)for(int i=0;i<s.computerCount();i++)computers.addItem(s.computerAt(i).id());if(selected!=null)for(int i=0;i<computers.getItemCount();i++)if(selected.equals(computers.getItemAt(i)))computers.setSelectedIndex(i);}finally{rendering=false;}
        if(s!=null){cycle.setText("Ciclo "+s.nextCycle());state.setText(view.phase()==SessionPhase.ACTIVE?UiText.label(s.state()):phaseLabel(view.phase()));metadata.setText("Aplicada: "+s.cycleMillis()+" ms · red "+s.networkLatency()+" ciclos");if(lastDuration!=s.cycleMillis()){if(!durationEdited){updatingDuration=true;try{duration.setText(Long.toString(s.cycleMillis()));}finally{updatingDuration=false;}}lastDuration=s.cycleMillis();}renderComputer();}
        else{state.setText(phaseLabel(view.phase()));}
        bufferPanel.render(s);eventPanel.renderSession(view);
        boolean active=view.phase()==SessionPhase.ACTIVE&&s!=null&&s.state()!=SimulationState.FAILED&&s.state()!=SimulationState.STOPPED&&s.state()!=SimulationState.STOPPING;
        boolean idle=active&&view.pendingActions()==0;
        start.setEnabled(idle&&s.state()==SimulationState.PAUSED);pause.setEnabled(active&&s.state()==SimulationState.RUNNING);step.setEnabled(idle&&s.state()==SimulationState.PAUSED);
        restart.setEnabled((view.phase()==SessionPhase.ACTIVE||view.phase()==SessionPhase.FAILED)&&view.pendingActions()==0);addProcess.setEnabled(idle);addBuffer.setEnabled(idle);duration.setEnabled(idle);applyDuration.setEnabled(idle);configuration.setEditable(idle&&!view.started());computerPanel.setControlsEnabled(idle);
        if(s!=null&&s.error()!=null){notice.setForeground(UiTheme.ERROR);notice.setText("Error del motor: "+s.error()+". Puedes reiniciar el escenario.");}
    }
    private static String phaseLabel(SessionPhase phase){return switch(phase){case PREPARING->"Preparando";case ACTIVE->"Disponible";case REPLACING->"Reiniciando";case CLOSING->"Cerrando hilos";case CLOSED->"Cerrada";case FAILED->"Error de sesión";};}
    private void renderComputer(){if(latest==null||latest.simulation()==null)return;Integer id=(Integer)computers.getSelectedItem();SimulationSnapshot s=latest.simulation();if(id!=null)for(int i=0;i<s.computerCount();i++)if(s.computerAt(i).id()==id)computerPanel.render(s.computerAt(i),s.nextCycle());}
    public void showNotice(SessionNotice n){notice.setForeground(n.error()?UiTheme.ERROR:UiTheme.INK);notice.setText(n.message());}
    public void closed(){for(JButton b:new JButton[]{start,pause,step,restart,addProcess,addBuffer,applyDuration})b.setEnabled(false);state.setText("Cerrada");}
    private ComputerConfig[] initialComputers(){ComputerConfig[] cs=new ComputerConfig[latest.initialConfig().computerCount()];for(int i=0;i<cs.length;i++)cs[i]=latest.initialConfig().computerAt(i);return cs;}
    private void processDialog(){
        if(latest==null)return;ProcessFormPanel form=new ProcessFormPanel();BufferDefinition[] bs;
        if(!latest.started()){bs=new BufferDefinition[latest.initialConfig().bufferCount()];for(int i=0;i<bs.length;i++)bs[i]=latest.initialConfig().bufferAt(i);}else{SimulationSnapshot s=latest.simulation();bs=new BufferDefinition[s.bufferCount()];for(int i=0;i<bs.length;i++)bs[i]=s.bufferAt(i).definition();}
        form.setChoices(initialComputers(),bs);JDialog dialog=dialog("Agregar proceso",form);JButton save=new JButton("Agregar proceso"),cancel=new JButton("Cancelar");JPanel actions=new JPanel(new FlowLayout(FlowLayout.TRAILING));actions.add(cancel);actions.add(save);dialog.add(actions,BorderLayout.SOUTH);cancel.addActionListener(e->dialog.dispose());save.addActionListener(e->{try{controller.addProcess(form.readDefinition());dialog.dispose();}catch(IllegalArgumentException failure){form.showError(failure.getMessage());}});showDialog(dialog);
    }
    private void bufferDialog(){
        if(latest==null)return;BufferFormPanel form=new BufferFormPanel();form.setComputers(initialComputers());JDialog dialog=dialog("Agregar buffer",form);JButton save=new JButton("Agregar buffer"),cancel=new JButton("Cancelar");JPanel actions=new JPanel(new FlowLayout(FlowLayout.TRAILING));actions.add(cancel);actions.add(save);dialog.add(actions,BorderLayout.SOUTH);cancel.addActionListener(e->dialog.dispose());save.addActionListener(e->{try{controller.addBuffer(form.readDefinition());dialog.dispose();}catch(IllegalArgumentException failure){form.showError(failure.getMessage());}});showDialog(dialog);
    }
    private JDialog dialog(String title,JPanel form){JDialog dialog=new JDialog(SwingUtilities.getWindowAncestor(this),title+(!latest.started()?" · Escenario inicial":" · Ejecución actual"),Dialog.ModalityType.APPLICATION_MODAL);dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);form.setBorder(BorderFactory.createEmptyBorder(16,18,12,18));dialog.setLayout(new BorderLayout());dialog.add(form,BorderLayout.CENTER);return dialog;}
    private void showDialog(JDialog dialog){dialog.pack();dialog.setSize(Math.max(570,dialog.getWidth()),dialog.getHeight());dialog.setLocationRelativeTo(this);dialog.setVisible(true);}
}
