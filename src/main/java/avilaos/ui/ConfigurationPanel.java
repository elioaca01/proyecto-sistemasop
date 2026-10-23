package avilaos.ui;
import avilaos.config.SimulationConfig;
import avilaos.simulation.ComputerConfig;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.ui.model.ComputerConfigTableModel;
import java.awt.*;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.*;

/** Edición de un escenario inicial; aplicar solicita una sustitución validada al controlador. */
public final class ConfigurationPanel extends JPanel {
    private final JTextField count=new JTextField("2"),duration=new JTextField("500"),latency=new JTextField("2");
    private final ComputerConfigTableModel computers=new ComputerConfigTableModel(new ComputerConfig[0]);
    private final JTable table=new JTable(computers);
    private final InitialLoadModel processRows=new InitialLoadModel(false),bufferRows=new InitialLoadModel(true);
    private final JTable processes=new JTable(processRows),buffers=new JTable(bufferRows);
    private final JButton apply=new JButton("Aplicar configuración"),resize=new JButton("Actualizar computadores"),removeProcess=new JButton("Quitar proceso inicial"),removeBuffer=new JButton("Quitar buffer inicial");
    private final JLabel error=FormGrid.error();private SimulationConfig initial;private boolean dirty,loading;private Consumer<SimulationConfig> handler=c->{};
    public ConfigurationPanel(){
        super(new BorderLayout(0,14));JPanel top=FormGrid.panel();FormGrid.row(top,0,"Cantidad de computadores",count,"config.computerCount");FormGrid.row(top,1,"Duración inicial del ciclo (ms)",duration,"config.cycleMillis");FormGrid.row(top,2,"Latencia de red (ciclos)",latency,"config.latency");
        JPanel header=new JPanel(new BorderLayout(0,8));header.add(new JLabel("Escenario inicial · Reiniciar vuelve a estos parámetros y cargas."),BorderLayout.NORTH);header.add(top,BorderLayout.CENTER);header.add(resize,BorderLayout.SOUTH);add(header,BorderLayout.NORTH);
        table.setName("config.computers");table.setRowHeight(26);JComboBox<SchedulingPolicyType> policies=new JComboBox<>(SchedulingPolicyType.values());FormGrid.enumRenderer(policies);table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(policies));table.setDefaultRenderer(SchedulingPolicyType.class,new DefaultTableCellRenderer(){@Override public void setValue(Object value){setText(UiText.label(value));}});
        processes.setName("config.processes");buffers.setName("config.buffers");
        JPanel p=new JPanel(new BorderLayout(0,6));p.add(new JScrollPane(processes),BorderLayout.CENTER);p.add(removeProcess,BorderLayout.SOUTH);
        JPanel b=new JPanel(new BorderLayout(0,6));b.add(new JScrollPane(buffers),BorderLayout.CENTER);b.add(removeBuffer,BorderLayout.SOUTH);
        JTabbedPane loads=new JTabbedPane();loads.addTab("Procesos iniciales",p);loads.addTab("Buffers iniciales",b);
        JSplitPane body=new JSplitPane(JSplitPane.VERTICAL_SPLIT,new JScrollPane(table),loads);body.setResizeWeight(.45);body.setBorder(null);add(body,BorderLayout.CENTER);
        JPanel footer=new JPanel(new BorderLayout(8,0));footer.add(error,BorderLayout.CENTER);footer.add(apply,BorderLayout.EAST);add(footer,BorderLayout.SOUTH);apply.setName("config.apply");
        resize.addActionListener(e->guard(this::resizeComputers));apply.addActionListener(e->guard(()->handler.accept(readConfiguration())));
        removeProcess.addActionListener(e->guard(()->{int i=processes.getSelectedRow();if(i<0)throw new IllegalArgumentException("Selecciona un proceso inicial");handler.accept(readConfiguration().withoutProcess(i));}));
        removeBuffer.addActionListener(e->guard(()->{int i=buffers.getSelectedRow();if(i<0)throw new IllegalArgumentException("Selecciona un buffer inicial");handler.accept(readConfiguration().withoutBuffer(initial.bufferAt(i).id()));}));
        computers.addTableModelListener(e->{if(!loading&&e.getColumn()>0)dirty=true;if(!computers.validationError().isEmpty())error.setText(computers.validationError());});
        javax.swing.event.DocumentListener changes=new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){changed();}public void removeUpdate(javax.swing.event.DocumentEvent e){changed();}public void changedUpdate(javax.swing.event.DocumentEvent e){changed();}private void changed(){if(!loading)dirty=true;}};
        count.getDocument().addDocumentListener(changes);duration.getDocument().addDocumentListener(changes);latency.getDocument().addDocumentListener(changes);
    }
    public void setApplyHandler(Consumer<SimulationConfig> handler){this.handler=handler;}
    private void guard(Runnable action){try{action.run();error.setText(" ");}catch(IllegalArgumentException|IndexOutOfBoundsException e){error.setText(e.getMessage());}}
    public void load(SimulationConfig c){loading=true;initial=c;count.setText(Integer.toString(c.computerCount()));duration.setText(Long.toString(c.cycleMillis()));latency.setText(Integer.toString(c.networkLatency()));ComputerConfig[] cs=new ComputerConfig[c.computerCount()];for(int i=0;i<cs.length;i++)cs[i]=c.computerAt(i);computers.setConfigs(cs);processRows.setConfig(c);bufferRows.setConfig(c);error.setText(" ");loading=false;dirty=false;}
    public boolean hasDraftChanges(){return dirty;}
    public void updateInitialLoads(SimulationConfig c){initial=c;processRows.setConfig(c);bufferRows.setConfig(c);}
    private void resizeComputers(){
        if(table.isEditing()&&!table.getCellEditor().stopCellEditing())throw new IllegalArgumentException("Termina de editar los computadores");
        int n=InputParser.integer(count.getText(),"Cantidad de computadores",2);ComputerConfig[] old=computers.configs();if(n==old.length)return;ComputerConfig[] cs=new ComputerConfig[n];
        for(int i=0;i<n;i++){if(i<old.length)cs[i]=old[i];else{int id=1;boolean used;do{used=false;for(int j=0;j<i;j++)if(cs[j].id()==id){used=true;id++;break;}}while(used);cs[i]=new ComputerConfig(id,64,SchedulingPolicyType.FCFS,2);}}computers.setConfigs(cs);dirty=true;
    }
    public SimulationConfig readConfiguration(){if(initial==null)throw new IllegalArgumentException("Escenario inicial requerido");resizeComputers();if(!computers.validationError().isEmpty())throw new IllegalArgumentException(computers.validationError());return initial.withComputers(computers.configs()).withTiming(InputParser.longInteger(duration.getText(),"Duración del ciclo",1),InputParser.integer(latency.getText(),"Latencia",0));}
    public void setEditable(boolean enabled){count.setEnabled(enabled);duration.setEnabled(enabled);latency.setEnabled(enabled);computers.setEditable(enabled);apply.setEnabled(enabled);resize.setEnabled(enabled);removeProcess.setEnabled(enabled);removeBuffer.setEnabled(enabled);}
    private static final class InitialLoadModel extends AbstractTableModel {
        private SimulationConfig config;private final boolean buffer;
        InitialLoadModel(boolean buffer){this.buffer=buffer;}void setConfig(SimulationConfig c){config=c;fireTableDataChanged();}
        public int getRowCount(){return config==null?0:buffer?config.bufferCount():config.processCount();}public int getColumnCount(){return buffer?3:5;}
        public String getColumnName(int c){return (buffer?new String[]{"Buffer","Anfitrión","Capacidad"}:new String[]{"Nombre","Computador","Tipo","Instrucciones","RAM"})[c];}
        public Object getValueAt(int r,int c){if(buffer){var b=config.bufferAt(r);return switch(c){case 0->b.id();case 1->b.hostId();default->b.capacity();};}var p=config.processAt(r);return switch(c){case 0->p.name();case 1->p.computerId();case 2->UiText.label(p.type());case 3->p.instructionCount();default->p.memoryRequired();};}
    }
}
