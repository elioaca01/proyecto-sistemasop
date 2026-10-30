package avilaos.ui.model;
import avilaos.sync.*;
import avilaos.ui.UiText;
import javax.swing.table.AbstractTableModel;
public final class BufferOperationTableModel extends AbstractTableModel {
    private BufferSnapshot buffer;
    private static final String[] C={"Proceso","Fase","Permiso de conteo","Mutex propio","Cambio sin señal","Red disponible","Completados","Espera"};
    public void setBuffer(BufferSnapshot b){buffer=b;fireTableDataChanged();}
    public int getRowCount(){return buffer==null?0:buffer.operationCount();}public int getColumnCount(){return C.length;}public String getColumnName(int c){return C[c];}
    public Class<?> getColumnClass(int c){return c>=2&&c<=4?Boolean.class:c==6?Integer.class:String.class;}
    public Object getValueAt(int r,int c){BufferOperationSnapshot o=buffer.operationAt(r);return switch(c){case 0->UiText.processId(o.processId());case 1->UiText.label(o.phase());case 2->o.countingPermit();case 3->o.mutexHeld();case 4->o.modified();case 5->UiText.cycle(o.networkAvailableAt());case 6->o.completedItems();case 7->UiText.label(o.waitingReason());default->throw new IndexOutOfBoundsException();};}
}
