package avilaos.ui.model;
import avilaos.simulation.ComputerConfig;
import avilaos.scheduling.SchedulingPolicyType;
import javax.swing.table.AbstractTableModel;
public final class ComputerConfigTableModel extends AbstractTableModel {
    private ComputerConfig[] rows;private boolean editable=true;private String error="";
    public ComputerConfigTableModel(ComputerConfig[] rows){setConfigs(rows);}
    public void setConfigs(ComputerConfig[] cs){if(cs==null)throw new IllegalArgumentException("Computadores requeridos");rows=cs.clone();error="";fireTableDataChanged();}
    public ComputerConfig[] configs(){return rows.clone();}public String validationError(){return error;}public void setEditable(boolean enabled){editable=enabled;fireTableDataChanged();}
    public int getRowCount(){return rows.length;}public int getColumnCount(){return 4;}
    public String getColumnName(int c){return new String[]{"Computador","RAM (unidades)","Política inicial","Quantum"}[c];}
    public Class<?> getColumnClass(int c){return c==2?SchedulingPolicyType.class:Integer.class;}
    public boolean isCellEditable(int r,int c){return editable&&c>0;}
    public Object getValueAt(int r,int c){ComputerConfig x=rows[r];return switch(c){case 0->x.id();case 1->x.ramCapacity();case 2->x.policy();case 3->x.quantum();default->throw new IndexOutOfBoundsException();};}
    public void setValueAt(Object value,int r,int c){
        if(!isCellEditable(r,c))return;ComputerConfig x=rows[r];
        try{
            rows[r]=new ComputerConfig(x.id(),c==1?Integer.parseInt(value.toString().strip()):x.ramCapacity(),c==2?(SchedulingPolicyType)value:x.policy(),c==3?Integer.parseInt(value.toString().strip()):x.quantum());error="";
        }catch(RuntimeException invalid){error="Revisa "+getColumnName(c)+": usa un entero positivo o una política válida";}
        fireTableCellUpdated(r,c);
    }
}
