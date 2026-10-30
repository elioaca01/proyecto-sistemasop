package avilaos.ui;
import avilaos.simulation.*;
import avilaos.ui.model.EventTableModel;
import avilaos.ui.session.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
public final class EventPanel extends JPanel {
    private final EventTableModel events=new EventTableModel();private final JTable log=new JTable(events);private final ResultModel results=new ResultModel();private final NoticeModel notices=new NoticeModel();
    private final JLabel pending=new JLabel("Pendientes: 0");private final JScrollPane logScroll=new JScrollPane(log);private SimulationSnapshot snapshot;
    public EventPanel(){
        super(new BorderLayout(0,12));pending.setName("events.pending");log.setName("events.log");log.setRowHeight(25);log.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);int[] widths={70,100,80,900};for(int i=0;i<4;i++)log.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        JTable confirmations=new JTable(results),actions=new JTable(notices);confirmations.setName("events.results");actions.setName("events.actions");for(JTable t:new JTable[]{confirmations,actions}){t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);t.setRowHeight(25);for(int i=0;i<t.getColumnCount();i++)t.getColumnModel().getColumn(i).setPreferredWidth(i==t.getColumnCount()-1?640:120);}
        JTabbedPane status=new JTabbedPane();status.addTab("Resultados del motor",new JScrollPane(confirmations));status.addTab("Solicitudes de interfaz",new JScrollPane(actions));JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,logScroll,status);split.setResizeWeight(.68);split.setBorder(null);add(pending,BorderLayout.NORTH);add(split,BorderLayout.CENTER);
    }
    public void render(SimulationSnapshot value){
        if(snapshot==value)return;
        int selected=log.getSelectedRow();JScrollBar scroll=logScroll.getVerticalScrollBar();boolean follow=scroll.getValue()+scroll.getVisibleAmount()>=scroll.getMaximum()-30;
        snapshot=value;events.setSnapshot(value);results.setSnapshot(value);if(selected>=0&&selected<events.getRowCount())log.setRowSelectionInterval(selected,selected);
        if(follow&&events.getRowCount()>0)log.scrollRectToVisible(log.getCellRect(events.getRowCount()-1,0,true));
    }
    public void renderSession(SessionView view){
        render(view.simulation());notices.setView(view);int count=0;SimulationSnapshot s=view.simulation();
        for(int i=0;i<view.noticeCount();i++){long id=view.noticeAt(i).engineRequestId();if(id<0)continue;boolean applied=false;if(s!=null)for(int j=0;j<s.resultCount();j++)if(s.resultAt(j).requestId()==id)applied=true;if(!applied)count++;}
        pending.setText("Pendientes: "+count+" órdenes del motor · "+view.pendingActions()+" acciones de interfaz. Se aplican al avanzar un ciclo.");
    }
    private static final class ResultModel extends AbstractTableModel {
        private SimulationSnapshot snapshot;void setSnapshot(SimulationSnapshot s){snapshot=s;fireTableDataChanged();}
        public int getRowCount(){return snapshot==null?0:snapshot.resultCount();}public int getColumnCount(){return 5;}public String getColumnName(int c){return new String[]{"Solicitud","Ciclo","Resultado","Proceso","Detalle"}[c];}
        public Object getValueAt(int r,int c){CommandResult x=snapshot.resultAt(r);return switch(c){case 0->x.requestId();case 1->UiText.cycle(x.appliedCycle());case 2->x.accepted()?"Aplicada":"Rechazada";case 3->UiText.processId(x.processId());case 4->x.message();default->throw new IndexOutOfBoundsException();};}
    }
    private static final class NoticeModel extends AbstractTableModel {
        private SessionView view;void setView(SessionView v){view=v;fireTableDataChanged();}
        public int getRowCount(){return view==null?0:view.noticeCount();}public int getColumnCount(){return 4;}public String getColumnName(int c){return new String[]{"Acción","Orden del motor","Estado","Detalle"}[c];}
        public Object getValueAt(int r,int c){SessionNotice n=view.noticeAt(r);return switch(c){case 0->n.actionId();case 1->UiText.cycle(n.engineRequestId());case 2->n.error()?"Error":"Información";case 3->n.message();default->throw new IndexOutOfBoundsException();};}
    }
}
