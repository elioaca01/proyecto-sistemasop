package avilaos.ui.model;
import javax.swing.table.AbstractTableModel;
import avilaos.simulation.ProcessSnapshot;
import avilaos.ui.UiText;
public final class ProcessTableModel extends AbstractTableModel {
    private static final String[] C={"ID","Nombre","Tipo","Estado","PC","MAR","Prioridad","Plazo restante","RAM asignada","Bloqueo"};
    private ProcessSnapshot[] rows=new ProcessSnapshot[0];private long boundary;
    public void setRows(ProcessSnapshot[] value){setRows(value,boundary);}
    public void setRows(ProcessSnapshot[] value,long boundary){this.rows=value==null?new ProcessSnapshot[0]:value.clone();this.boundary=boundary;fireTableDataChanged();}
    public ProcessSnapshot processAt(int i){return rows[i];}
    public int rowOf(long id){for(int i=0;i<rows.length;i++)if(rows[i].id()==id)return i;return -1;}
    public int getRowCount(){return rows.length;}public int getColumnCount(){return C.length;}public String getColumnName(int c){return C[c];}
    public Class<?> getColumnClass(int c){return c==0||c==7?Long.class:c>=4&&c<=8?Integer.class:String.class;}
    public Object getValueAt(int r,int c){ProcessSnapshot p=rows[r];return switch(c){case 0->p.id();case 1->p.definition().name();case 2->UiText.label(p.definition().type());case 3->UiText.label(p.state());case 4->p.pc();case 5->p.mar();case 6->p.definition().priority();case 7->UiText.deadlineRemaining(p,boundary);case 8->p.assignedMemory();case 9->UiText.label(p.blockReason());default->throw new IndexOutOfBoundsException();};}
}
