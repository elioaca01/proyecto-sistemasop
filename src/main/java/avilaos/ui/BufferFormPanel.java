package avilaos.ui;
import avilaos.simulation.ComputerConfig;
import avilaos.sync.BufferDefinition;
import java.awt.BorderLayout;
import javax.swing.*;
public final class BufferFormPanel extends JPanel {
    private final JTextField id=new JTextField("1",12),capacity=new JTextField("1");private final JComboBox<Integer> host=new JComboBox<>();private final JLabel error=FormGrid.error();
    public BufferFormPanel(){super(new BorderLayout(0,12));JPanel form=FormGrid.panel();FormGrid.row(form,0,"ID del buffer",id,"buffer.id");FormGrid.row(form,1,"Computador anfitrión",host,"buffer.host");FormGrid.row(form,2,"Capacidad (unidades RAM)",capacity,"buffer.capacity");add(form,BorderLayout.CENTER);add(error,BorderLayout.SOUTH);}
    public void setComputers(ComputerConfig[] cs){Object old=host.getSelectedItem();host.removeAllItems();for(ComputerConfig c:cs)host.addItem(c.id());if(old!=null)host.setSelectedItem(old);}
    public BufferDefinition readDefinition(){Integer computer=(Integer)host.getSelectedItem();if(computer==null)throw new IllegalArgumentException("Elige el computador anfitrión");return new BufferDefinition(InputParser.integer(id.getText(),"ID del buffer",1),computer,InputParser.integer(capacity.getText(),"Capacidad",1));}
    public void showError(String text){error.setText(text==null?" ":text);}
}
