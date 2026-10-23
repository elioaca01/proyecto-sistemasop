package avilaos.tests;

import avilaos.config.*;
import avilaos.process.*;
import avilaos.simulation.ComputerConfig;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.sync.BufferDefinition;
import java.io.*;
import static avilaos.tests.TestSupport.*;

public final class ScenarioJsonTest {
    static final String EMPTY="""
        {"formatVersion":1,"cycleMillis":500,"networkLatency":2,
         "computers":[{"id":1,"ramCapacity":64,"policy":"FCFS","quantum":2},
                      {"id":2,"ramCapacity":64,"policy":"ROUND_ROBIN","quantum":1}],
         "buffers":[],"processes":[]}
        """;
    static final String PRODUCER="""
        {"name":"Productor remoto","computerId":2,"type":"PRODUCER","instructionCount":6,
         "memoryRequired":2,"priority":0,"deadlineCycles":100,"cpuBurst":0,"ioCycles":0,
         "bufferId":1,"operationInterval":1,"targetItems":1}
        """;
    static final String REMOTE=EMPTY.replace("\"buffers\":[]","\"buffers\":[{\"id\":1,\"hostId\":1,\"capacity\":1}]")
        .replace("\"processes\":[]","\"processes\":["+PRODUCER+"]");
    static SimulationConfig read(String text){try{return ScenarioJson.read(new StringReader(text));}catch(IOException e){throw new AssertionError(e);}}
    static String write(SimulationConfig config){try{StringWriter w=new StringWriter();ScenarioJson.write(w,config);return w.toString();}catch(IOException e){throw new AssertionError(e);}}
    static void equivalent(SimulationConfig a,SimulationConfig b){
        equal(a.cycleMillis(),b.cycleMillis());equal(a.networkLatency(),b.networkLatency());
        equal(a.computerCount(),b.computerCount());equal(a.bufferCount(),b.bufferCount());equal(a.processCount(),b.processCount());
        for(int i=0;i<a.computerCount();i++)equal(a.computerAt(i),b.computerAt(i));
        for(int i=0;i<a.bufferCount();i++)equal(a.bufferAt(i),b.bufferAt(i));
        for(int i=0;i<a.processCount();i++)equal(a.processAt(i),b.processAt(i));
    }
    private static void invalid(String text){
        try{ScenarioJson.read(new StringReader(text));throw new AssertionError("JSON inválido aceptado: "+text);}
        catch(ConfigurationException e){isTrue(e.getMessage().contains("$"));}
        catch(IOException e){throw new AssertionError("Error de formato sin contexto",e);}
    }
    static SimulationConfig mixed(){
        ComputerConfig[] cs=new ComputerConfig[4];int i=0;for(SchedulingPolicyType policy:SchedulingPolicyType.values())cs[i]=new ComputerConfig(++i,64,policy,i);
        ProcessDefinition[] ps={new ProcessDefinition("Ávila \"CPU\"\n雪\\",1,ProcessType.CPU_BOUND,3,2,0,Long.MAX_VALUE,0,0,0,0,0),
            new ProcessDefinition("IO",2,ProcessType.IO_BOUND,7,3,1,100,2,3,0,0,0),
            new ProcessDefinition("P",3,ProcessType.PRODUCER,6,2,2,100,0,0,1,1,1),
            new ProcessDefinition("C",4,ProcessType.CONSUMER,6,2,3,100,0,0,1,1,1)};
        return new SimulationConfig(Long.MAX_VALUE,2,cs,new BufferDefinition[]{new BufferDefinition(1,1,1)},ps);
    }
    public static void run(){
        equal(1,ScenarioJson.FORMAT_VERSION);SimulationConfig c=read(REMOTE);equal(500L,c.cycleMillis());equal(2,c.networkLatency());equal(2,c.computerCount());
        equal(new ComputerConfig(1,64,SchedulingPolicyType.FCFS,2),c.computerAt(0));equal(new ComputerConfig(2,64,SchedulingPolicyType.ROUND_ROBIN,1),c.computerAt(1));
        equal(new BufferDefinition(1,1,1),c.bufferAt(0));equal(BufferSimulationTest.definition(2,ProcessType.PRODUCER).type(),c.processAt(0).type());
        equal(new ProcessDefinition("Productor remoto",2,ProcessType.PRODUCER,6,2,0,100,0,0,1,1,1),c.processAt(0));
        equivalent(c,read(write(c)));equivalent(mixed(),read(write(mixed())));equivalent(read(EMPTY),read(write(read(EMPTY))));
        String shuffled=REMOTE.replace("\"formatVersion\":1,","").replace("\"processes\":[","\"formatVersion\":1,\"processes\":[");equivalent(c,read(shuffled));
        String[] malformed={"", " ", "null", "[]", "{}", REMOTE.substring(0,REMOTE.length()-4), "/*comment*/"+REMOTE, REMOTE+" {}", REMOTE+" true",
            EMPTY.replace("\"formatVersion\":1","\"formatVersion\":1,\"formatVersion\":1"),EMPTY.replace("\"formatVersion\":1","\"formatVersion\":null"),
            EMPTY.replace("\"formatVersion\":1","\"formatVersion\":0"),EMPTY.replace("\"formatVersion\":1","\"formatVersion\":2"),
            EMPTY.replace("\"formatVersion\":1,",""),EMPTY.replace("\"formatVersion\":1","\"typo\":1,\"formatVersion\":1"),
            EMPTY.replace("\"cycleMillis\":500","\"cycleMillis\":\"500\""),EMPTY.replace("\"cycleMillis\":500","\"cycleMillis\":true"),
            EMPTY.replace("\"cycleMillis\":500","\"cycleMillis\":1.0"),EMPTY.replace("\"cycleMillis\":500","\"cycleMillis\":1e2"),
            EMPTY.replace("\"cycleMillis\":500","\"cycleMillis\":9223372036854775808"),EMPTY.replace("\"cycleMillis\":500","\"cycleMillis\":0"),
            EMPTY.replace("\"networkLatency\":2","\"networkLatency\":2147483648"),EMPTY.replace("\"networkLatency\":2","\"networkLatency\":-1"),
            EMPTY.replace("\"buffers\":[]","\"buffers\":null"),EMPTY.replace("\"buffers\":[]","\"buffers\":[null]"),
            EMPTY.replace("\"processes\":[]","\"processes\":{}"),EMPTY.replace("\"policy\":\"FCFS\"","\"policy\":\"FCFS\",\"quantum\":1"),
            EMPTY.replace("\"id\":2","\"id\":1"),EMPTY.replace("\"id\":1","\"id\":0"),EMPTY.replace("\"policy\":\"FCFS\"","\"policy\":\"FIFO\""),
            EMPTY.replace("\"ramCapacity\":64","\"ramCapacity\":-1"),EMPTY.replace("\"quantum\":2","\"quantum\":0"),
            REMOTE.replace("\"hostId\":1","\"hostId\":99"),REMOTE.replace("\"computerId\":2","\"computerId\":99"),
            REMOTE.replace("\"bufferId\":1","\"bufferId\":99"),REMOTE.replace("\"capacity\":1","\"capacity\":65"),
            REMOTE.replace("\"memoryRequired\":2","\"memoryRequired\":65"),REMOTE.replace("\"instructionCount\":6","\"instructionCount\":5"),
            REMOTE.replace("\"type\":\"PRODUCER\"","\"type\":\"producer\""),REMOTE.replace("\"name\":\"Productor remoto\"","\"name\":null"),
            REMOTE.replace("\"name\":\"Productor remoto\"","\"name\":\" \""),REMOTE.replace("\"deadlineCycles\":100,",""),
            REMOTE.replace("\"capacity\":1","\"capacity\":1,\"capacity\":2"),REMOTE.replace("\"targetItems\":1","\"targetItems\":1,\"pc\":2"),
            EMPTY.replace("\"processes\":[]","\"processes\":[],"),EMPTY.replace('"','\'')};
        for(String text:malformed)invalid(text);
        invalid(EMPTY.replaceAll(",\\s*\\{\"id\":2[^}]+\\}",""));
        invalid(EMPTY.replace("\"computers\":[{\"id\":1,\"ramCapacity\":64,\"policy\":\"FCFS\",\"quantum\":2},\n              {\"id\":2,\"ramCapacity\":64,\"policy\":\"ROUND_ROBIN\",\"quantum\":1}]","\"computers\":[]"));
        invalid(REMOTE.replace("\"buffers\":[{\"id\":1,\"hostId\":1,\"capacity\":1}]","\"buffers\":[{\"id\":1,\"hostId\":1,\"capacity\":1},{\"id\":1,\"hostId\":1,\"capacity\":1}]"));
        invalid(REMOTE.replace("\"buffers\":[{\"id\":1,\"hostId\":1,\"capacity\":1}]","\"buffers\":[{\"id\":1,\"hostId\":1,\"capacity\":33},{\"id\":2,\"hostId\":1,\"capacity\":32}]"));
        equivalent(read(REMOTE.replace("\"ramCapacity\":64","\"ramCapacity\":2147483647").replace("\"capacity\":1","\"capacity\":2147483647")),read(write(read(REMOTE.replace("\"ramCapacity\":64","\"ramCapacity\":2147483647").replace("\"capacity\":1","\"capacity\":2147483647")))));
        class BorrowedReader extends StringReader{boolean closed;BorrowedReader(){super(REMOTE);}public void close(){closed=true;super.close();}}
        class BorrowedWriter extends StringWriter{boolean closed;public void close(){closed=true;}}
        BorrowedReader reader=new BorrowedReader();BorrowedWriter writer=new BorrowedWriter();
        try{ScenarioJson.read(reader);ScenarioJson.write(writer,c);}catch(IOException e){throw new AssertionError(e);}isTrue(!reader.closed);isTrue(!writer.closed);
        try{ScenarioJson.write(new Writer(){public void write(char[] b,int off,int len)throws IOException{throw new IOException("salida rota");}public void flush(){}public void close(){}},c);throw new AssertionError("Writer roto aceptado");}catch(IOException expected){isTrue(expected.getMessage().contains("rota"));}
        try{ScenarioJson.read(new Reader(){public int read(char[] b,int off,int len)throws IOException{throw new IOException("entrada rota");}public void close(){}});throw new AssertionError("Reader roto aceptado");}catch(IOException expected){isTrue(expected.getMessage().contains("rota"));}
    }
}
