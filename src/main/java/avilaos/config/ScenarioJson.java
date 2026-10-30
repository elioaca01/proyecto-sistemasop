package avilaos.config;

import avilaos.process.*;
import avilaos.simulation.ComputerConfig;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.structures.LinkedList;
import avilaos.sync.BufferDefinition;
import com.google.gson.Strictness;
import com.google.gson.stream.*;
import java.io.*;

/** Formato explícito: solo parámetros iniciales, sin reflexión ni colecciones JSON. */
public final class ScenarioJson {
    public static final int FORMAT_VERSION=1;
    private enum Kind {INT,LONG,STRING,COMPUTERS,BUFFERS,PROCESSES}
    private static final String[] ROOT={"formatVersion","cycleMillis","networkLatency","computers","buffers","processes"};
    private static final Kind[] ROOT_TYPES={Kind.INT,Kind.LONG,Kind.INT,Kind.COMPUTERS,Kind.BUFFERS,Kind.PROCESSES};
    private static final String[] COMPUTER={"id","ramCapacity","policy","quantum"};
    private static final Kind[] COMPUTER_TYPES={Kind.INT,Kind.INT,Kind.STRING,Kind.INT};
    private static final String[] BUFFER={"id","hostId","capacity"};
    private static final Kind[] BUFFER_TYPES={Kind.INT,Kind.INT,Kind.INT};
    private static final String[] PROCESS={"name","computerId","type","instructionCount","memoryRequired","priority","deadlineCycles","cpuBurst","ioCycles","bufferId","operationInterval","targetItems"};
    private static final Kind[] PROCESS_TYPES={Kind.STRING,Kind.INT,Kind.STRING,Kind.INT,Kind.INT,Kind.INT,Kind.LONG,Kind.INT,Kind.INT,Kind.INT,Kind.INT,Kind.INT};
    private ScenarioJson(){}

    public static SimulationConfig read(Reader input)throws IOException{
        if(input==null)throw new IllegalArgumentException("Lector requerido");
        JsonReader r=new JsonReader(input);r.setStrictness(Strictness.STRICT);
        try{
            Object[] v=fields(r,ROOT,ROOT_TYPES);require(r,JsonToken.END_DOCUMENT);
            if((int)v[0]!=FORMAT_VERSION)throw new ConfigurationException("$.formatVersion: versión no admitida: "+v[0]);
            return new SimulationConfig((long)v[1],(int)v[2],(ComputerConfig[])v[3],(BufferDefinition[])v[4],(ProcessDefinition[])v[5]);
        }catch(ConfigurationException e){throw e;}
        catch(MalformedJsonException|EOFException e){throw new ConfigurationException(r.getPath()+": JSON incompleto o inválido: "+e.getMessage(),e);}
        catch(IllegalArgumentException|IllegalStateException e){throw new ConfigurationException(r.getPath()+": "+e.getMessage(),e);}
    }
    private static void require(JsonReader r,JsonToken token)throws IOException{
        if(r.peek()!=token)throw new ConfigurationException(r.getPath()+": se esperaba "+token+", recibido "+r.peek());
    }
    private static Object[] fields(JsonReader r,String[] names,Kind[] kinds)throws IOException{
        require(r,JsonToken.BEGIN_OBJECT);String path=r.getPath();r.beginObject();Object[] values=new Object[names.length];int seen=0;
        while(r.hasNext()){
            String name=r.nextName();int index=-1;for(int i=0;i<names.length;i++)if(names[i].equals(name)){index=i;break;}
            if(index<0)throw new ConfigurationException(path+"."+name+": campo desconocido");
            int bit=1<<index;if((seen&bit)!=0)throw new ConfigurationException(path+"."+name+": campo repetido");
            seen|=bit;values[index]=value(r,kinds[index]);
        }
        r.endObject();
        for(int i=0;i<names.length;i++)if((seen&(1<<i))==0)throw new ConfigurationException(path+"."+names[i]+": campo obligatorio ausente");
        return values;
    }
    private static Object value(JsonReader r,Kind kind)throws IOException{
        return switch(kind){
            case INT->{long n=number(r);if(n<Integer.MIN_VALUE||n>Integer.MAX_VALUE)throw new ConfigurationException(r.getPreviousPath()+": entero fuera de rango int");yield (int)n;}
            case LONG->number(r);
            case STRING->{require(r,JsonToken.STRING);yield r.nextString();}
            case COMPUTERS->computers(r);
            case BUFFERS->buffers(r);
            case PROCESSES->processes(r);
        };
    }
    private static long number(JsonReader r)throws IOException{
        require(r,JsonToken.NUMBER);String path=r.getPath(),text=r.nextString();
        if(text.indexOf('.')>=0||text.indexOf('e')>=0||text.indexOf('E')>=0)throw new ConfigurationException(path+": se requiere un entero decimal sin fracción ni exponente");
        try{return Long.parseLong(text);}catch(NumberFormatException e){throw new ConfigurationException(path+": entero fuera de rango long",e);}
    }
    private static ComputerConfig[] computers(JsonReader r)throws IOException{
        require(r,JsonToken.BEGIN_ARRAY);r.beginArray();LinkedList<ComputerConfig> list=new LinkedList<>();
        while(r.hasNext()){
            String path=r.getPath();Object[] v=fields(r,COMPUTER,COMPUTER_TYPES);SchedulingPolicyType policy;
            try{policy=SchedulingPolicyType.valueOf((String)v[2]);}catch(IllegalArgumentException e){throw new ConfigurationException(path+".policy: política desconocida: "+v[2],e);}
            try{list.addLast(new ComputerConfig((int)v[0],(int)v[1],policy,(int)v[3]));}catch(IllegalArgumentException e){throw new ConfigurationException(path+": "+e.getMessage(),e);}
        }
        r.endArray();ComputerConfig[] result=new ComputerConfig[list.size()];list.copyInto(result);return result;
    }
    private static BufferDefinition[] buffers(JsonReader r)throws IOException{
        require(r,JsonToken.BEGIN_ARRAY);r.beginArray();LinkedList<BufferDefinition> list=new LinkedList<>();
        while(r.hasNext()){
            String path=r.getPath();Object[] v=fields(r,BUFFER,BUFFER_TYPES);
            try{list.addLast(new BufferDefinition((int)v[0],(int)v[1],(int)v[2]));}catch(IllegalArgumentException e){throw new ConfigurationException(path+": "+e.getMessage(),e);}
        }
        r.endArray();BufferDefinition[] result=new BufferDefinition[list.size()];list.copyInto(result);return result;
    }
    private static ProcessDefinition[] processes(JsonReader r)throws IOException{
        require(r,JsonToken.BEGIN_ARRAY);r.beginArray();LinkedList<ProcessDefinition> list=new LinkedList<>();
        while(r.hasNext()){
            String path=r.getPath();Object[] v=fields(r,PROCESS,PROCESS_TYPES);ProcessType type;
            try{type=ProcessType.valueOf((String)v[2]);}catch(IllegalArgumentException e){throw new ConfigurationException(path+".type: tipo desconocido: "+v[2],e);}
            try{list.addLast(new ProcessDefinition((String)v[0],(int)v[1],type,(int)v[3],(int)v[4],(int)v[5],(long)v[6],(int)v[7],(int)v[8],(int)v[9],(int)v[10],(int)v[11]));}
            catch(IllegalArgumentException e){throw new ConfigurationException(path+": "+e.getMessage(),e);}
        }
        r.endArray();ProcessDefinition[] result=new ProcessDefinition[list.size()];list.copyInto(result);return result;
    }
    public static void write(Writer output,SimulationConfig c)throws IOException{
        if(output==null||c==null)throw new IllegalArgumentException("Escritor y escenario requeridos");
        JsonWriter w=new JsonWriter(output);w.setStrictness(Strictness.STRICT);w.setIndent("  ");
        w.beginObject().name("formatVersion").value(FORMAT_VERSION).name("cycleMillis").value(c.cycleMillis()).name("networkLatency").value(c.networkLatency());
        w.name("computers").beginArray();
        for(int i=0;i<c.computerCount();i++){ComputerConfig v=c.computerAt(i);w.beginObject().name("id").value(v.id()).name("ramCapacity").value(v.ramCapacity()).name("policy").value(v.policy().name()).name("quantum").value(v.quantum()).endObject();}
        w.endArray().name("buffers").beginArray();
        for(int i=0;i<c.bufferCount();i++){BufferDefinition v=c.bufferAt(i);w.beginObject().name("id").value(v.id()).name("hostId").value(v.hostId()).name("capacity").value(v.capacity()).endObject();}
        w.endArray().name("processes").beginArray();
        for(int i=0;i<c.processCount();i++){
            ProcessDefinition v=c.processAt(i);w.beginObject().name("name").value(v.name()).name("computerId").value(v.computerId()).name("type").value(v.type().name());
            w.name("instructionCount").value(v.instructionCount()).name("memoryRequired").value(v.memoryRequired()).name("priority").value(v.priority()).name("deadlineCycles").value(v.deadlineCycles());
            w.name("cpuBurst").value(v.cpuBurst()).name("ioCycles").value(v.ioCycles()).name("bufferId").value(v.bufferId()).name("operationInterval").value(v.operationInterval()).name("targetItems").value(v.targetItems()).endObject();
        }
        w.endArray().endObject();w.flush();
    }
}
