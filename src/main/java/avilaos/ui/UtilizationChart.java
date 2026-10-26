package avilaos.ui;

import avilaos.metrics.UtilizationSample;
import avilaos.simulation.SimulationSnapshot;
import java.awt.*;
import java.awt.geom.Path2D;
import javax.swing.*;

/** Java2D: usa todas las muestras conservadas por el motor, no un muestreo del Timer. */
public final class UtilizationChart extends JPanel implements Scrollable {
    private static final Color[] COLORS={new Color(0x087F8C),new Color(0xB44D32),new Color(0x4C59A6),new Color(0x8A5B12),new Color(0x7D4690),new Color(0x22603D)};
    private SimulationSnapshot snapshot;
    public UtilizationChart(){setName("metrics.chart");setBackground(Color.WHITE);setSnapshot(null);}
    public void setSnapshot(SimulationSnapshot snapshot){
        this.snapshot=snapshot;StringBuilder ids=new StringBuilder();
        if(snapshot!=null)for(int i=0;i<snapshot.metrics().computerCount();i++){if(i>0)ids.append(", ");ids.append(snapshot.metrics().computerIdAt(i));}
        String text="Ciclo global; utilización acumulada (%). Computadores: "+ids+". Muestras: "+(snapshot==null?0:snapshot.sampleCount())+".";
        getAccessibleContext().setAccessibleDescription(text);setToolTipText(text);revalidate();repaint();
    }
    private int minimumHeight(){return 140+20*((snapshot==null?0:snapshot.metrics().computerCount()+3)/4);}
    @Override public Dimension getPreferredSize(){return new Dimension(760,Math.max(300,minimumHeight()));}
    @Override public Dimension getMinimumSize(){return new Dimension(250,minimumHeight());}
    public Dimension getPreferredScrollableViewportSize(){return getPreferredSize();}
    public int getScrollableUnitIncrement(Rectangle visible,int orientation,int direction){return 24;}
    public int getScrollableBlockIncrement(Rectangle visible,int orientation,int direction){return Math.max(24,(orientation==SwingConstants.VERTICAL?visible.height:visible.width)-24);}
    public boolean getScrollableTracksViewportWidth(){return true;}
    public boolean getScrollableTracksViewportHeight(){return getParent() instanceof JViewport v&&v.getHeight()>=minimumHeight();}
    private static Stroke stroke(int series){return series%3==0?new BasicStroke(2):new BasicStroke(2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,10,series%3==1?new float[]{7,4}:new float[]{2,3},0);}
    @Override protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setFont(getFont().deriveFont(12f));
            int count=snapshot==null?0:snapshot.metrics().computerCount(),columns=Math.max(1,(getWidth()-72)/155),rows=(count+columns-1)/columns;
            g.setColor(UiTheme.INK);g.setFont(getFont().deriveFont(Font.BOLD,14f));g.drawString("Utilización acumulada por computador",16,21);g.setFont(getFont().deriveFont(12f));
            for(int i=0;i<count;i++){
                int x=64+(i%columns)*155,y=43+(i/columns)*20;g.setColor(COLORS[i%COLORS.length]);g.setStroke(stroke(i));g.drawLine(x,y-4,x+23,y-4);g.drawString("Computador "+snapshot.metrics().computerIdAt(i),x+30,y);
            }
            int left=64,top=58+rows*20,right=Math.max(left+1,getWidth()-24),bottom=Math.max(top+1,getHeight()-48),width=right-left,height=bottom-top;
            g.setStroke(new BasicStroke(1));g.setColor(UiTheme.INK);g.drawString("Utilización acumulada (%)",left,top-10);
            for(int percent=0;percent<=100;percent+=(height<100?50:25)){int y=bottom-(int)Math.round(height*percent/100.0);g.setColor(UiTheme.BORDER);g.drawLine(left,y,right,y);g.setColor(UiTheme.INK);g.drawString(Integer.toString(percent),left-34,y+4);}
            long last=snapshot==null||snapshot.sampleCount()==0?1:snapshot.sampleAt(snapshot.sampleCount()-1).cycle();
            int ticks=(int)Math.min(4,last);
            for(int tick=0;tick<=ticks;tick++){long cycle=Math.round(last*(tick/(double)ticks));int x=left+(int)Math.round(width*(cycle/(double)last));g.setColor(UiTheme.BORDER);g.drawLine(x,top,x,bottom);g.setColor(UiTheme.INK);String label=Long.toString(cycle);g.drawString(label,x-g.getFontMetrics().stringWidth(label)/2,bottom+18);}
            g.setColor(UiTheme.INK);g.drawLine(left,top,left,bottom);g.drawLine(left,bottom,right,bottom);String axis="Ciclo global";g.drawString(axis,left+(width-g.getFontMetrics().stringWidth(axis))/2,getHeight()-8);
            if(snapshot==null||snapshot.sampleCount()==0){g.drawString("Sin ciclos confirmados",left+16,top+height/2);return;}
            for(int machine=0;machine<count;machine++){
                int id=snapshot.metrics().computerIdAt(machine);Path2D path=new Path2D.Double();boolean begun=false;double lastX=0,lastY=0;
                for(int n=0;n<snapshot.sampleCount();n++){
                    UtilizationSample sample=snapshot.sampleAt(n);int index=-1;for(int j=0;j<sample.computerCount();j++)if(sample.computerIdAt(j)==id){index=j;break;}
                    if(index<0){begun=false;continue;}
                    double x=left+width*(sample.cycle()/(double)last),y=bottom-height*sample.utilizationAt(index);
                    if(!begun){path.moveTo(x,y);begun=true;}else path.lineTo(x,y);lastX=x;lastY=y;
                }
                g.setColor(COLORS[machine%COLORS.length]);g.setStroke(stroke(machine));g.draw(path);if(begun)g.fillOval((int)Math.round(lastX)-3,(int)Math.round(lastY)-3,6,6);
            }
        }finally{g.dispose();}
    }
}
