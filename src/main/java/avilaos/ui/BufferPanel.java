package avilaos.ui;
import avilaos.simulation.SimulationSnapshot;
import avilaos.sync.BufferSnapshot;
import avilaos.ui.model.*;
import java.awt.*;
import javax.swing.*;
public final class BufferPanel extends JPanel {
    private final JComboBox<Integer> selector=new JComboBox<>();private final JLabel summary=new JLabel(),counts=new JLabel();private final JProgressBar occupancy=new JProgressBar();
    private final BufferItemTableModel items=new BufferItemTableModel();private final SemaphoreTableModel semaphores=new SemaphoreTableModel();private final BufferOperationTableModel operations=new BufferOperationTableModel();
    private SimulationSnapshot snapshot;private boolean rendering;
    public BufferPanel(){
        super(new BorderLayout(0,12));selector.setName("buffer.selector");summary.setName("buffer.summary");occupancy.setName("buffer.occupancy");occupancy.setStringPainted(true);
        JPanel choose=new JPanel(new FlowLayout(FlowLayout.LEADING,8,0));choose.add(new JLabel("Buffer"));choose.add(selector);
        JPanel top=new JPanel(new GridLayout(0,1,0,6));top.add(choose);top.add(summary);top.add(counts);top.add(occupancy);add(top,BorderLayout.NORTH);
        JTable tokens=new JTable(items),sems=new JTable(semaphores),ops=new JTable(operations);tokens.setName("buffer.items");sems.setName("buffer.semaphores");ops.setName("buffer.operations");
        for(JTable t:new JTable[]{tokens,sems,ops}){t.setRowHeight(25);t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);for(int i=0;i<t.getColumnCount();i++)t.getColumnModel().getColumn(i).setPreferredWidth(t==sems&&i==2?260:t==ops?140:130);}
        JPanel tokenBox=new JPanel(new BorderLayout());tokenBox.setBorder(BorderFactory.createTitledBorder("Elementos · orden FIFO"));tokenBox.add(new JScrollPane(tokens));
        JPanel semBox=new JPanel(new BorderLayout());semBox.setBorder(BorderFactory.createTitledBorder("Semáforos simulados"));semBox.add(new JScrollPane(sems));
        JSplitPane upper=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,tokenBox,semBox);upper.setResizeWeight(.45);upper.setBorder(null);
        JPanel operationBox=new JPanel(new BorderLayout());operationBox.setBorder(BorderFactory.createTitledBorder("Operaciones activas y reservas"));operationBox.add(new JScrollPane(ops));
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,upper,operationBox);split.setResizeWeight(.55);split.setBorder(null);add(split,BorderLayout.CENTER);
        selector.addActionListener(e->{if(!rendering)showSelected();});showSelected();
    }
    public void render(SimulationSnapshot value){
        snapshot=value;Object selected=selector.getSelectedItem();rendering=true;
        try{selector.removeAllItems();if(value!=null)for(int i=0;i<value.bufferCount();i++)selector.addItem(value.bufferAt(i).definition().id());if(selected!=null)for(int i=0;i<selector.getItemCount();i++)if(selected.equals(selector.getItemAt(i)))selector.setSelectedIndex(i);}
        finally{rendering=false;}showSelected();
    }
    private void showSelected(){
        BufferSnapshot b=null;Integer id=(Integer)selector.getSelectedItem();if(snapshot!=null&&id!=null)for(int i=0;i<snapshot.bufferCount();i++)if(snapshot.bufferAt(i).definition().id()==id)b=snapshot.bufferAt(i);
        items.setBuffer(b);semaphores.setBuffer(b);operations.setBuffer(b);
        if(b==null){summary.setText("Aún no hay buffers confirmados. Agrega uno y avanza un ciclo.");counts.setText(" ");occupancy.setMaximum(1);occupancy.setValue(0);occupancy.setString("Sin buffer");return;}
        summary.setText("B"+b.definition().id()+" · anfitrión "+b.definition().hostId()+" · capacidad "+b.definition().capacity()+" · dueño del mutex "+UiText.processId(b.mutexOwnerId()));counts.setText("Producidos: "+b.produced()+"     Consumidos: "+b.consumed()+"     Ocupación: "+b.size());occupancy.setMaximum(b.definition().capacity());occupancy.setValue(b.size());occupancy.setString(b.size()+" / "+b.definition().capacity());
    }
}
