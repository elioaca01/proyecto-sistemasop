package avilaos.ui.model;

import avilaos.metrics.MetricsSnapshot;
import avilaos.metrics.SemaphoreWaitMetrics;
import avilaos.simulation.SimulationSnapshot;
import javax.swing.table.AbstractTableModel;

public final class BufferMetricsTableModel extends AbstractTableModel {
    private static final String[] COLUMNS={"Buffer","Producidos","Consumidos","Ocupación","Episodios cerrados","Espera cerrada (ciclos)","Promedio (ciclos)","Episodios abiertos","Espera abierta (ciclos)"};
    private MetricsSnapshot snapshot;
    public void setSnapshot(SimulationSnapshot snapshot){this.snapshot=snapshot==null?null:snapshot.metrics();fireTableDataChanged();}
    public int getRowCount(){return snapshot==null?0:snapshot.bufferCount()+1;}
    public int getColumnCount(){return COLUMNS.length;}
    public String getColumnName(int column){return COLUMNS[column];}
    public Object getValueAt(int row,int column){
        MetricsSnapshot.BufferMetrics b=row==0?null:snapshot.bufferAt(row-1);
        SemaphoreWaitMetrics w=b==null?snapshot.waits():b.waits();
        return switch(column){
            case 0->b==null?"Todos":"B"+b.id();
            case 1,2,3->{long value=0;if(b!=null)value=counter(b,column);else for(int i=0;i<snapshot.bufferCount();i++)value=Math.addExact(value,counter(snapshot.bufferAt(i),column));yield value;}
            case 4->w.completedEpisodes();case 5->w.completedCycles();case 6->MetricsTableModel.format(w.averageWait(),false);
            case 7->w.openEpisodes();case 8->w.openCycles();default->throw new IndexOutOfBoundsException();
        };
    }
    private static long counter(MetricsSnapshot.BufferMetrics b,int column){return column==1?b.produced():column==2?b.consumed():b.occupancy();}
}
