package avilaos.ui.model;

import avilaos.metrics.PerformanceMetrics;
import avilaos.simulation.SimulationSnapshot;
import java.util.Locale;
import javax.swing.table.AbstractTableModel;

public final class MetricsTableModel extends AbstractTableModel {
    private static final String[] COLUMNS={"Ámbito","Completados","Vencidos","Pendientes","Throughput (proc/ciclo)","CPU (%)","Usuario (%)","Núcleo (%)","Respuesta (ciclos)","Deadlines (%)","Equidad (Jain)"};
    private SimulationSnapshot snapshot;
    public void setSnapshot(SimulationSnapshot snapshot){this.snapshot=snapshot;fireTableDataChanged();}
    public int getRowCount(){return snapshot==null?0:snapshot.metrics().computerCount()+1;}
    public int getColumnCount(){return COLUMNS.length;}
    public String getColumnName(int column){return COLUMNS[column];}
    public static String format(double value,boolean percent){return Double.isFinite(value)?String.format(Locale.ROOT,percent?"%.2f":"%.3f",percent?100*value:value):"Sin datos";}
    public Object getValueAt(int row,int column){
        PerformanceMetrics m=row==0?snapshot.metrics().global():snapshot.metrics().computerAt(row-1);
        return switch(column){
            case 0->row==0?"Sistema":"Computador "+snapshot.metrics().computerIdAt(row-1);
            case 1->m.completed();case 2->m.expired();case 3->m.pending();
            case 4->format(m.throughput(),false);case 5->format(m.utilization(),true);
            case 6->format(m.userUtilization(),true);case 7->format(m.kernelUtilization(),true);
            case 8->format(m.averageResponse(),false);case 9->format(m.deadlineCompliance(),true);
            case 10->format(m.fairness(),false);default->throw new IndexOutOfBoundsException();
        };
    }
}
