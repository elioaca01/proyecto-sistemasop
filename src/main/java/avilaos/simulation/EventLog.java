package avilaos.simulation;
import avilaos.structures.LinkedList;
import java.util.concurrent.Semaphore;
/** Orden de emisión real; los lectores no reconstruyen decisiones a partir de muestras. */
public final class EventLog {
    private final LinkedList<SimulationEvent> events=new LinkedList<>();
    private final Semaphore mutex=new Semaphore(1);
    public void append(SimulationEvent e){if(e==null)throw new IllegalArgumentException("Evento requerido");mutex.acquireUninterruptibly();try{events.addLast(e);}finally{mutex.release();}}
    public int size(){mutex.acquireUninterruptibly();try{return events.size();}finally{mutex.release();}}
    public SimulationEvent at(int i){mutex.acquireUninterruptibly();try{return events.get(i);}finally{mutex.release();}}
    public SimulationEvent[] copy(){mutex.acquireUninterruptibly();try{SimulationEvent[] a=new SimulationEvent[events.size()];events.copyInto(a);return a;}finally{mutex.release();}}
}
