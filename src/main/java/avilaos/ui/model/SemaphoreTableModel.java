package avilaos.ui.model;
import avilaos.sync.BufferSnapshot;
import javax.swing.table.AbstractTableModel;
public final class SemaphoreTableModel extends AbstractTableModel {
    private BufferSnapshot buffer;
    public void setBuffer(BufferSnapshot b){buffer=b;fireTableDataChanged();}
    public int getRowCount(){return buffer==null?0:3;}public int getColumnCount(){return 3;}
    public String getColumnName(int c){return new String[]{"Semáforo","Permisos libres","Espera FIFO"}[c];}
    public Class<?> getColumnClass(int c){return c==1?Integer.class:String.class;}
    public Object getValueAt(int r,int c){
        if(c==0)return new String[]{"Vacíos","Llenos","Mutex"}[r];
        if(c==1)return r==0?buffer.emptyValue():r==1?buffer.fullValue():buffer.mutexValue();
        int n=r==0?buffer.emptyWaitingCount():r==1?buffer.fullWaitingCount():buffer.mutexWaitingCount();StringBuilder s=new StringBuilder();
        for(int i=0;i<n;i++){if(i>0)s.append(" → ");s.append('P').append(r==0?buffer.emptyWaitingAt(i):r==1?buffer.fullWaitingAt(i):buffer.mutexWaitingAt(i));}
        return n==0?"—":s.toString();
    }
}
