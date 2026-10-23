package avilaos.ui;
import avilaos.simulation.*;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.ui.model.ProcessTableModel;
import java.awt.*;
import javax.swing.*;

/** Observa un computador. Su selección sigue al ID global al cambiar de cola o CPU. */
public final class ComputerPanel extends JPanel {
    @FunctionalInterface public interface SchedulingHandler {void apply(int computerId,SchedulingPolicyType policy,int quantum);}
    private final JLabel current=new JLabel("CPU disponible"),last=new JLabel("Último ciclo: —"),ramText=new JLabel(),applied=new JLabel(),error=FormGrid.error();
    private final JProgressBar ram=new JProgressBar();private final JComboBox<SchedulingPolicyType> policy=new JComboBox<>(SchedulingPolicyType.values());private final JTextField quantum=new JTextField("2",4);private final JButton apply=new JButton("Aplicar política/quantum"),inspect=new JButton("Inspeccionar CPU / última ejecución");
    private final ProcessTableModel[] models=new ProcessTableModel[4];private final JTable[] queues=new JTable[4];private final JTabbedPane tabs=new JTabbedPane();private final ProcessDetailsPanel details=new ProcessDetailsPanel();
    private ComputerSnapshot computer;private long boundary,selected=-1;private boolean rendering;private SchedulingPolicyType lastPolicy;private int lastQuantum=-1;private SchedulingHandler handler=(id,p,q)->{};
    public ComputerPanel(){
        super(new BorderLayout(0,12));tabs.setName("computer.queues");current.setName("computer.current");last.setName("computer.last");policy.setName("computer.policy");quantum.setName("computer.quantum");apply.setName("computer.applyScheduling");inspect.setName("computer.inspect");ram.setName("computer.ram");FormGrid.enumRenderer(policy);
        JPanel cpu=new JPanel(new GridLayout(0,1,0,5));cpu.add(current);cpu.add(last);cpu.add(ramText);cpu.add(ram);cpu.setBorder(BorderFactory.createEmptyBorder(4,0,6,0));
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEADING,8,0));controls.add(new JLabel("Política"));controls.add(policy);controls.add(new JLabel("Quantum"));controls.add(quantum);controls.add(apply);controls.add(inspect);
        JPanel top=new JPanel(new BorderLayout(0,8));top.add(cpu,BorderLayout.NORTH);top.add(controls,BorderLayout.CENTER);top.add(applied,BorderLayout.SOUTH);add(top,BorderLayout.NORTH);
        String[] names={"new","ready","blocked","terminated"};String[] labels={"Nuevos","Listos","Bloqueados","Terminados"};
        for(int i=0;i<4;i++){
            final int index=i;models[i]=new ProcessTableModel();queues[i]=new JTable(models[i]);queues[i].setName("computer."+names[i]);queues[i].setSelectionMode(ListSelectionModel.SINGLE_SELECTION);queues[i].setAutoResizeMode(JTable.AUTO_RESIZE_OFF);queues[i].setRowHeight(25);
            for(int col=0;col<models[i].getColumnCount();col++)queues[i].getColumnModel().getColumn(col).setPreferredWidth(col==1?140:col==2||col==7||col==9?110:70);
            queues[i].getSelectionModel().addListSelectionListener(e->{if(!rendering&&!e.getValueIsAdjusting()&&queues[index].getSelectedRow()>=0)selectFrom(index);});
            tabs.addTab(labels[i],new JScrollPane(queues[i]));
        }
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,tabs,details);split.setResizeWeight(.64);split.setBorder(null);details.setMinimumSize(new Dimension(260,180));tabs.setMinimumSize(new Dimension(300,180));add(split,BorderLayout.CENTER);add(error,BorderLayout.SOUTH);
        apply.addActionListener(e->{try{if(computer!=null)handler.apply(computer.id(),(SchedulingPolicyType)policy.getSelectedItem(),InputParser.integer(quantum.getText(),"Quantum",1));error.setText("Cambio enviado; pendiente del próximo ciclo");}catch(IllegalArgumentException|IllegalStateException failure){error.setText(failure.getMessage());}});
        inspect.addActionListener(e->{if(computer!=null){ProcessSnapshot p=computer.running()!=null?computer.running():find(computer.lastProcessId());if(p!=null){clearSelection();selected=p.id();details.render(p,boundary);}}});
    }
    public void setSchedulingHandler(SchedulingHandler handler){this.handler=handler;}
    private void selectFrom(int index){selected=models[index].processAt(queues[index].getSelectedRow()).id();rendering=true;try{for(int i=0;i<4;i++)if(i!=index)queues[i].clearSelection();}finally{rendering=false;}details.render(find(selected),boundary);}
    public void clearSelection(){selected=-1;rendering=true;try{for(JTable q:queues)q.clearSelection();details.render(null,boundary);}finally{rendering=false;}}
    public void render(ComputerSnapshot c,long boundaryCycle){
        if(computer==null||computer.id()!=c.id()){clearSelection();lastPolicy=null;lastQuantum=-1;}computer=c;boundary=boundaryCycle;
        ProcessSnapshot p=c.running();current.setText(p==null?"CPU disponible":"En CPU: P"+p.id()+" · "+p.definition().name()+"   PC "+p.pc()+" · MAR "+p.mar()+" · prioridad "+p.definition().priority()+" · plazo "+UiText.deadlineRemaining(p,boundary));
        last.setText("Último ciclo "+UiText.cycle(boundary-1)+" → "+UiText.processId(c.lastProcessId())+" · "+UiText.label(c.lastInstruction())+" · modo "+UiText.label(c.mode()));
        ram.setMaximum(c.ramCapacity());ram.setValue(c.ramUsed());ramText.setText("RAM: "+c.ramUsed()+" usadas / "+c.ramFree()+" libres · total "+c.ramCapacity()+" unidades");
        applied.setText("Aplicada: "+UiText.label(c.policyType())+" · quantum "+c.quantum()+" · instrucciones del turno "+c.sliceUsed());
        if(lastPolicy!=c.policyType()||lastQuantum!=c.quantum()){if(!policy.isFocusOwner())policy.setSelectedItem(c.policyType());if(!quantum.isFocusOwner())quantum.setText(Integer.toString(c.quantum()));lastPolicy=c.policyType();lastQuantum=c.quantum();}
        ProcessSnapshot[][] rows={new ProcessSnapshot[c.newCount()],new ProcessSnapshot[c.readyCount()],new ProcessSnapshot[c.blockedCount()],new ProcessSnapshot[c.terminatedCount()]};
        for(int i=0;i<rows[0].length;i++)rows[0][i]=c.newAt(i);for(int i=0;i<rows[1].length;i++)rows[1][i]=c.readyAt(i);for(int i=0;i<rows[2].length;i++)rows[2][i]=c.blockedAt(i);for(int i=0;i<rows[3].length;i++)rows[3][i]=c.terminatedAt(i);
        rendering=true;
        try{String[] titles={"Nuevos","Listos","Bloqueados","Terminados"};for(int i=0;i<4;i++){models[i].setRows(rows[i],boundary);tabs.setTitleAt(i,titles[i]+" ("+rows[i].length+")");int row=models[i].rowOf(selected);if(row>=0){queues[i].setRowSelectionInterval(row,row);}}}finally{rendering=false;}
        ProcessSnapshot chosen=find(selected);if(chosen==null)selected=-1;details.render(chosen,boundary);inspect.setEnabled(p!=null||find(c.lastProcessId())!=null);
    }
    private ProcessSnapshot find(long id){if(computer==null||id<0)return null;if(computer.running()!=null&&computer.running().id()==id)return computer.running();for(ProcessTableModel m:models){int row=m.rowOf(id);if(row>=0)return m.processAt(row);}return null;}
    public void setControlsEnabled(boolean enabled){policy.setEnabled(enabled);quantum.setEnabled(enabled);apply.setEnabled(enabled);}
}
