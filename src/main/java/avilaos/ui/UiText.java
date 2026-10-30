package avilaos.ui;
import avilaos.process.*;
import avilaos.simulation.*;
import avilaos.hardware.CpuMode;
import avilaos.scheduling.SchedulingPolicyType;
/** Etiquetas de presentación; no decide estados ni planificación. */
public final class UiText {
    private UiText(){}
    public static String label(Object value){
        if(value==null)return "—";
        if(value instanceof ProcessState v)return switch(v){case NEW->"Nuevo";case READY->"Listo";case RUNNING->"En CPU";case BLOCKED->"Bloqueado";case TERMINATED->"Terminado";};
        if(value instanceof ProcessType v)return switch(v){case CPU_BOUND->"CPU bound";case IO_BOUND->"I/O bound";case PRODUCER->"Productor";case CONSUMER->"Consumidor";};
        if(value instanceof BlockReason v)return switch(v){case NONE->"—";case IO->"E/S";case NETWORK->"Red";case SEMAPHORE_EMPTY->"Vacíos";case SEMAPHORE_FULL->"Llenos";case SEMAPHORE_MUTEX->"Mutex";};
        if(value instanceof CpuMode v)return switch(v){case IDLE->"Inactiva";case USER->"Usuario";case KERNEL->"Núcleo";};
        if(value instanceof SchedulingPolicyType v)return switch(v){case FCFS->"FCFS";case EDF->"EDF";case ROUND_ROBIN->"Round Robin";case PREEMPTIVE_PRIORITY->"Prioridades";};
        if(value instanceof SimulationState v)return switch(v){case PAUSED->"Pausada";case RUNNING->"Ejecutando";case STOPPING->"Cerrando";case STOPPED->"Detenida";case FAILED->"Error";};
        if(value instanceof TerminationReason v)return v==TerminationReason.COMPLETED?"Completado":"Deadline vencido";
        if(value instanceof InstructionKind v)return switch(v){case COMPUTE->"Cómputo";case IO_REQUEST->"Solicitud E/S";case SEM_WAIT->"wait";case SEM_SIGNAL->"signal";case BUFFER_INSERT->"Insertar";case BUFFER_REMOVE->"Extraer";};
        if(value instanceof BufferPhase v)return switch(v){case COMPUTE->"Cómputo";case WAIT_EMPTY->"wait(vacíos)";case WAIT_FULL->"wait(llenos)";case WAIT_MUTEX->"wait(mutex)";case INSERT->"Insertar";case REMOVE->"Extraer";case SIGNAL_MUTEX->"signal(mutex)";case SIGNAL_FULL->"signal(llenos)";case SIGNAL_EMPTY->"signal(vacíos)";case COMPLETE->"Completo";};
        return value.toString();
    }
    public static long deadlineRemaining(ProcessSnapshot p,long boundaryCycle){long boundary=p.state()==ProcessState.TERMINATED?p.terminationCycle()+1:boundaryCycle;return Math.max(0,p.deadlineAt()-boundary);}
    public static String processId(long id){return id<0?"—":"P"+id;}
    public static String cycle(long value){return value<0?"—":Long.toString(value);}
}
