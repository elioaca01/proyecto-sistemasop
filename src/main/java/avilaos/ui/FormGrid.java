package avilaos.ui;
import java.awt.*;
import javax.swing.*;
/** Alineación y etiquetas comunes de formularios, sin estado del simulador. */
final class FormGrid {
    private FormGrid(){}
    static JPanel panel(){return new JPanel(new GridBagLayout());}
    static void row(JPanel panel,int row,String text,JComponent field,String name){
        field.setName(name);field.getAccessibleContext().setAccessibleName(text);JLabel label=new JLabel(text);label.setLabelFor(field);
        GridBagConstraints c=new GridBagConstraints();c.gridy=row;c.gridx=0;c.anchor=GridBagConstraints.LINE_START;c.insets=new Insets(6,0,6,14);panel.add(label,c);
        c.gridx=1;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(6,0,6,0);panel.add(field,c);
    }
    static void enumRenderer(JComboBox<?> combo){combo.setRenderer(new DefaultListCellRenderer(){@Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus){super.getListCellRendererComponent(list,value,index,selected,focus);setText(UiText.label(value));return this;}});}
    static JLabel error(){JLabel l=new JLabel(" ");l.setForeground(new Color(0xA34529));return l;}
}
