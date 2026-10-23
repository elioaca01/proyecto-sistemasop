package avilaos.config;
import avilaos.simulation.ComputerConfig;
import avilaos.sync.BufferDefinition;
import avilaos.process.*;

/** Escenario inicial por valor; no es un checkpoint del motor en ejecución. */
public final class SimulationConfig {
    private final long cycleMillis;
    private final int networkLatency;
    private final ComputerConfig[] computers;
    private final BufferDefinition[] buffers;
    private final ProcessDefinition[] processes;
    public SimulationConfig(long cycleMillis,int networkLatency,ComputerConfig[] computers,BufferDefinition[] buffers,ProcessDefinition[] processes) {
        if(cycleMillis<=0||networkLatency<0||computers==null||computers.length<2||buffers==null||processes==null) throw new IllegalArgumentException("Se requieren al menos dos computadores, duración positiva y latencia no negativa");
        this.cycleMillis=cycleMillis;this.networkLatency=networkLatency;
        this.computers=computers.clone();this.buffers=buffers.clone();this.processes=processes.clone();
        for(int i=0;i<this.computers.length;i++) {
            ComputerConfig c=this.computers[i];if(c==null) throw new IllegalArgumentException("Computador requerido");
            for(int j=0;j<i;j++) if(this.computers[j].id()==c.id()) throw new IllegalArgumentException("ID de computador duplicado: "+c.id());
        }
        for(int i=0;i<this.buffers.length;i++) {
            BufferDefinition b=this.buffers[i];if(b==null) throw new IllegalArgumentException("Buffer requerido");
            computer(b.hostId());
            for(int j=0;j<i;j++) if(this.buffers[j].id()==b.id()) throw new IllegalArgumentException("ID de buffer duplicado: "+b.id());
        }
        for(ComputerConfig c:this.computers) {
            long reserved=0;
            for(BufferDefinition b:this.buffers) if(b.hostId()==c.id()) reserved+=b.capacity();
            if(reserved>c.ramCapacity()) throw new IllegalArgumentException("Los buffers exceden la RAM del computador "+c.id());
        }
        for(ProcessDefinition p:this.processes) {
            if(p==null) throw new IllegalArgumentException("Proceso requerido");
            if(p.memoryRequired()>computer(p.computerId()).ramCapacity()) throw new IllegalArgumentException("La memoria del proceso excede la RAM total de su computador");
            if(p.type()==ProcessType.PRODUCER||p.type()==ProcessType.CONSUMER) {
                boolean found=false;for(BufferDefinition b:this.buffers) if(b.id()==p.bufferId()) found=true;
                if(!found) throw new IllegalArgumentException("Buffer inexistente: "+p.bufferId());
            }
        }
    }
    private ComputerConfig computer(int id) {
        for(ComputerConfig c:computers) if(c.id()==id) return c;
        throw new IllegalArgumentException("Computador inexistente: "+id);
    }
    public long cycleMillis(){return cycleMillis;} public int networkLatency(){return networkLatency;}
    public int computerCount(){return computers.length;} public ComputerConfig computerAt(int i){return computers[i];}
    public int bufferCount(){return buffers.length;} public BufferDefinition bufferAt(int i){return buffers[i];}
    public int processCount(){return processes.length;} public ProcessDefinition processAt(int i){return processes[i];}
    public SimulationConfig withComputers(ComputerConfig[] cs){return new SimulationConfig(cycleMillis,networkLatency,cs,buffers,processes);}
    public SimulationConfig withTiming(long ms,int latency){return new SimulationConfig(ms,latency,computers,buffers,processes);}
    public SimulationConfig withAddedBuffer(BufferDefinition b){
        BufferDefinition[] copy=new BufferDefinition[buffers.length+1];System.arraycopy(buffers,0,copy,0,buffers.length);copy[buffers.length]=b;
        return new SimulationConfig(cycleMillis,networkLatency,computers,copy,processes);
    }
    public SimulationConfig withAddedProcess(ProcessDefinition p){
        ProcessDefinition[] copy=new ProcessDefinition[processes.length+1];System.arraycopy(processes,0,copy,0,processes.length);copy[processes.length]=p;
        return new SimulationConfig(cycleMillis,networkLatency,computers,buffers,copy);
    }
    public SimulationConfig withoutBuffer(int id){
        int index=-1;for(int i=0;i<buffers.length;i++) if(buffers[i].id()==id) index=i;
        if(index<0) throw new IllegalArgumentException("Buffer inexistente: "+id);
        BufferDefinition[] copy=new BufferDefinition[buffers.length-1];
        for(int i=0,j=0;i<buffers.length;i++) if(i!=index) copy[j++]=buffers[i];
        return new SimulationConfig(cycleMillis,networkLatency,computers,copy,processes);
    }
    public SimulationConfig withoutProcess(int index){
        if(index<0||index>=processes.length) throw new IndexOutOfBoundsException("Proceso inicial fuera de rango");
        ProcessDefinition[] copy=new ProcessDefinition[processes.length-1];
        for(int i=0,j=0;i<processes.length;i++) if(i!=index) copy[j++]=processes[i];
        return new SimulationConfig(cycleMillis,networkLatency,computers,buffers,copy);
    }
}
