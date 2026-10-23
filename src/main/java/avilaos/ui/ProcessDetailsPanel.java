package avilaos.ui;
import avilaos.simulation.ProcessSnapshot;
import avilaos.process.*;
import avilaos.sync.BufferOperationSnapshot;
import java.awt.*;
import javax.swing.*;
public final class ProcessDetailsPanel extends JPanel {
    private final JTextArea text=new JTextArea();
    public ProcessDetailsPanel(){super(new BorderLayout(0,8));setBorder(BorderFactory.createTitledBorder("Contexto del proceso · PCB"));text.setName("pcb.details");text.setEditable(false);text.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));text.setLineWrap(true);text.setWrapStyleWord(true);add(new JScrollPane(text),BorderLayout.CENTER);render(null,0);}
    public void render(ProcessSnapshot p,long boundary){
        if(p==null){text.setText("Selecciona un proceso en la CPU o en una cola.");return;}
        ProcessDefinition d=p.definition();StringBuilder s=new StringBuilder();
        s.append("P").append(p.id()).append(" · ").append(d.name()).append('\n');
        line(s,"Computador",d.computerId());line(s,"Tipo",UiText.label(d.type()));line(s,"Estado",UiText.label(p.state()));line(s,"Motivo de bloqueo",UiText.label(p.blockReason()));
        s.append('\n');line(s,"PC",p.pc());line(s,"MAR",p.mar());line(s,"Instrucciones totales",d.instructionCount());line(s,"Instrucciones restantes",p.remainingInstructions());line(s,"Prioridad",d.priority());
        line(s,"Memoria requerida",d.memoryRequired());line(s,"Memoria asignada",p.assignedMemory());
        s.append('\n');line(s,"Llegada",p.arrivalCycle());line(s,"Deadline inicial",d.deadlineCycles());line(s,"Deadline absoluto",p.deadlineAt());line(s,p.state()==ProcessState.TERMINATED?"Plazo restante al finalizar":"Plazo restante",UiText.deadlineRemaining(p,boundary));
        line(s,"Primera ejecución",UiText.cycle(p.firstRunCycle()));line(s,"Terminación",UiText.cycle(p.terminationCycle()));line(s,"Resultado",UiText.label(p.terminationReason()));line(s,"Servicio usuario",p.userCycles());line(s,"Servicio núcleo",p.kernelCycles());
        s.append('\n');line(s,"Ráfaga de cómputo E/S",d.cpuBurst());line(s,"Duración E/S",d.ioCycles());line(s,"Buffer",d.bufferId());line(s,"Cómputo por elemento",d.operationInterval());line(s,"Elementos objetivo",d.targetItems());
        BufferOperationSnapshot o=p.operation();
        if(o!=null){s.append('\n');line(s,"Fase",UiText.label(o.phase()));line(s,"Permiso de conteo",yes(o.countingPermit()));line(s,"Mutex propio",yes(o.mutexHeld()));line(s,"Cambio pendiente de señal",yes(o.modified()));line(s,"Red iniciada",yes(o.networkStarted()));line(s,"Red disponible en",UiText.cycle(o.networkAvailableAt()));line(s,"Elementos completos",o.completedItems());line(s,"Espera de operación",UiText.label(o.waitingReason()));}
        int caret=text.getCaretPosition();text.setText(s.toString());text.setCaretPosition(Math.min(caret,text.getDocument().getLength()));
    }
    private static void line(StringBuilder s,String label,Object value){s.append(label).append(": ").append(value).append('\n');}
    private static String yes(boolean value){return value?"Sí":"No";}
}
