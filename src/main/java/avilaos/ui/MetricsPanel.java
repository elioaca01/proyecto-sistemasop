package avilaos.ui;

import avilaos.simulation.SimulationSnapshot;
import avilaos.ui.model.*;
import java.awt.*;
import javax.swing.*;

/** Las tablas y el gráfico observan la misma frontera confirmada del motor. */
public final class MetricsPanel extends JPanel {
    private final MetricsTableModel computers=new MetricsTableModel();
    private final BufferMetricsTableModel buffers=new BufferMetricsTableModel();
    private final UtilizationChart chart=new UtilizationChart();
    private final JTextArea summary=new JTextArea(2,40);
    public MetricsPanel(){
        super(new BorderLayout(0,8));setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        summary.setName("metrics.summary");summary.setEditable(false);summary.setOpaque(false);summary.setLineWrap(true);summary.setWrapStyleWord(true);summary.setForeground(UiTheme.INK);add(summary,BorderLayout.NORTH);
        JTabbedPane tables=new JTabbedPane();tables.addTab("CPU y procesos",table(computers,"metrics.computers","metrics.computerScroll"));tables.addTab("Buffers y esperas",table(buffers,"metrics.buffers","metrics.bufferScroll"));tables.setMinimumSize(new Dimension(240,100));
        JScrollPane plot=new JScrollPane(chart);plot.setBorder(null);plot.setMinimumSize(new Dimension(240,100));
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,plot,tables);split.setResizeWeight(.58);split.setBorder(null);add(split,BorderLayout.CENTER);render(null);
    }
    private static JScrollPane table(javax.swing.table.TableModel model,String name,String scrollName){
        JTable table=new JTable(model);table.setName(name);table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);table.setRowHeight(26);table.setFillsViewportHeight(true);
        for(int i=0;i<table.getColumnCount();i++)table.getColumnModel().getColumn(i).setPreferredWidth(i==0?160:i==4||i>=8?180:145);
        JScrollPane scroll=new JScrollPane(table);scroll.setColumnHeaderView(table.getTableHeader());scroll.setName(scrollName);scroll.setPreferredSize(new Dimension(760,150));scroll.setMinimumSize(new Dimension(220,75));return scroll;
    }
    public void render(SimulationSnapshot snapshot){
        computers.setSnapshot(snapshot);buffers.setSnapshot(snapshot);chart.setSnapshot(snapshot);
        summary.setText(snapshot==null?"Sin datos de una ejecución.":"Ciclos confirmados: "+snapshot.metrics().global().cycles()+" · Una muestra por ciclo. Utilización incluye usuario y núcleo.\nSin datos: todavía no existe el denominador. Las esperas abiertas se muestran aparte del promedio de episodios cerrados.");
    }
}
