package avilaos.ui.model;
import avilaos.simulation.*;
import avilaos.ui.UiText;
import javax.swing.table.AbstractTableModel;
public final class EventTableModel extends AbstractTableModel {
    private SimulationSnapshot snapshot;
    public void setSnapshot(SimulationSnapshot s){snapshot=s;fireTableDataChanged();}
    public int getRowCount(){return snapshot==null?0:snapshot.eventCount();}public int getColumnCount(){return 4;}
    public String getColumnName(int c){return new String[]{"Ciclo","Computador","Proceso","Decisión"}[c];}
    public Class<?> getColumnClass(int c){return c==0?Long.class:String.class;}
    public Object getValueAt(int r,int c){SimulationEvent e=snapshot.eventAt(r);return switch(c){case 0->e.cycle();case 1->e.computerId()==0?"Global":Integer.toString(e.computerId());case 2->UiText.processId(e.processId());case 3->e.message();default->throw new IndexOutOfBoundsException();};}
}
