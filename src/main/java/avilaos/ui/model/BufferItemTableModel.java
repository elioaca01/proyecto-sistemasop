package avilaos.ui.model;
import avilaos.sync.*;
import javax.swing.table.AbstractTableModel;
public final class BufferItemTableModel extends AbstractTableModel {
    private BufferSnapshot buffer;
    public void setBuffer(BufferSnapshot b){buffer=b;fireTableDataChanged();}
    public int getRowCount(){return buffer==null?0:buffer.size();}public int getColumnCount(){return 3;}
    public String getColumnName(int c){return new String[]{"Elemento","Productor","Computador"}[c];}
    public Class<?> getColumnClass(int c){return c==2?Integer.class:Long.class;}
    public Object getValueAt(int r,int c){BufferItem i=buffer.itemAt(r);return switch(c){case 0->i.id();case 1->i.producerId();case 2->i.producerComputerId();default->throw new IndexOutOfBoundsException();};}
}
