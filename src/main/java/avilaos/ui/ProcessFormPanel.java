package avilaos.ui;
import avilaos.process.*;
import avilaos.simulation.ComputerConfig;
import avilaos.sync.BufferDefinition;
import java.awt.*;
import javax.swing.*;
public final class ProcessFormPanel extends JPanel {
    private final JTextField name=new JTextField("Proceso",18),instructions=new JTextField("12"),memory=new JTextField("4"),priority=new JTextField("0"),deadline=new JTextField("100");
    private final JTextField burst=new JTextField("2"),io=new JTextField("3"),interval=new JTextField("1"),quota=new JTextField("1");
    private final JComboBox<Integer> computer=new JComboBox<>(),buffer=new JComboBox<>();
    private final JComboBox<ProcessType> type=new JComboBox<>(ProcessType.values());
    private final JPanel specific=new JPanel(new CardLayout());private final JLabel error=FormGrid.error();
    public ProcessFormPanel(){
        super(new BorderLayout(0,12));JPanel common=FormGrid.panel();
        FormGrid.row(common,0,"Nombre",name,"process.name");FormGrid.row(common,1,"Computador",computer,"process.computer");FormGrid.row(common,2,"Tipo",type,"process.type");
        FormGrid.row(common,3,"Instrucciones totales",instructions,"process.instructions");FormGrid.row(common,4,"RAM requerida (unidades)",memory,"process.memory");FormGrid.row(common,5,"Prioridad (0 = mayor)",priority,"process.priority");FormGrid.row(common,6,"Deadline relativo (ciclos)",deadline,"process.deadline");
        JPanel ioForm=FormGrid.panel();FormGrid.row(ioForm,0,"Ráfaga de cómputo",burst,"process.cpuBurst");FormGrid.row(ioForm,1,"Duración E/S (ciclos)",io,"process.ioCycles");
        JPanel bufferForm=FormGrid.panel();FormGrid.row(bufferForm,0,"Buffer",buffer,"process.buffer");FormGrid.row(bufferForm,1,"Cómputo por elemento",interval,"process.interval");FormGrid.row(bufferForm,2,"Elementos objetivo",quota,"process.quota");
        specific.add(new JLabel("Solo ejecuta instrucciones de cómputo."),"CPU");specific.add(ioForm,"IO");specific.add(bufferForm,"BUFFER");
        type.addActionListener(e->showType());FormGrid.enumRenderer(type);showType();
        JPanel body=new JPanel(new BorderLayout(0,12));body.add(common,BorderLayout.NORTH);body.add(specific,BorderLayout.CENTER);add(body,BorderLayout.CENTER);add(error,BorderLayout.SOUTH);
    }
    private void showType(){ProcessType t=(ProcessType)type.getSelectedItem();((CardLayout)specific.getLayout()).show(specific,t==ProcessType.CPU_BOUND?"CPU":t==ProcessType.IO_BOUND?"IO":"BUFFER");revalidate();}
    public void setChoices(ComputerConfig[] computers,BufferDefinition[] buffers){Object old=computer.getSelectedItem(),oldBuffer=buffer.getSelectedItem();computer.removeAllItems();for(ComputerConfig c:computers)computer.addItem(c.id());if(old!=null)computer.setSelectedItem(old);buffer.removeAllItems();for(BufferDefinition b:buffers)buffer.addItem(b.id());if(oldBuffer!=null)buffer.setSelectedItem(oldBuffer);}
    public ProcessDefinition readDefinition(){
        Integer c=(Integer)computer.getSelectedItem();if(c==null)throw new IllegalArgumentException("Elige un computador");
        ProcessType t=(ProcessType)type.getSelectedItem();int cpu=0,device=0,b=0,between=0,count=0;
        if(t==ProcessType.IO_BOUND){cpu=InputParser.integer(burst.getText(),"Ráfaga",1);device=InputParser.integer(io.getText(),"Duración E/S",1);}
        if(t==ProcessType.PRODUCER||t==ProcessType.CONSUMER){Integer chosen=(Integer)buffer.getSelectedItem();if(chosen==null)throw new IllegalArgumentException("Buffer: agrega y confirma un buffer antes de crear este proceso");b=chosen;between=InputParser.integer(interval.getText(),"Cómputo por elemento",1);count=InputParser.integer(quota.getText(),"Elementos objetivo",1);}
        return new ProcessDefinition(name.getText(),c,t,InputParser.integer(instructions.getText(),"Instrucciones",1),InputParser.integer(memory.getText(),"RAM requerida",1),InputParser.integer(priority.getText(),"Prioridad",0),InputParser.longInteger(deadline.getText(),"Deadline",1),cpu,device,b,between,count);
    }
    public void showError(String text){error.setText(text==null?" ":text);}
}
