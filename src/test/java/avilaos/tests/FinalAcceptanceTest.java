package avilaos.tests;

import avilaos.config.*;
import avilaos.metrics.*;
import avilaos.process.*;
import avilaos.scheduling.SchedulingPolicyType;
import avilaos.simulation.*;
import avilaos.sync.BufferDefinition;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonWriter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static avilaos.tests.TestSupport.*;

/** Experimentos del informe: misma carga/horizonte, resultados del motor, sin ventanas. */
public final class FinalAcceptanceTest {
    private record Measurement(String name,String kind,SimulationConfig config,int horizon,long allTerminatedAt,SimulationSnapshot result) { }
    private static ProcessDefinition cpu(String name,int machine,int instructions,long deadline,int priority){return new ProcessDefinition(name,machine,ProcessType.CPU_BOUND,instructions,4,priority,deadline,0,0,0,0,0);}
    private static ComputerConfig[] machines(SchedulingPolicyType policy){return new ComputerConfig[]{new ComputerConfig(1,64,policy,2),new ComputerConfig(2,64,policy,2)};}
    private static Measurement measure(String name,String kind,SimulationConfig config,int horizon){
        try(Simulation sim=SemaphoreWaitMetricsTest.create(config)){
            long finished=-1;SimulationSnapshot end=sim.snapshot();
            for(int cycle=1;cycle<=horizon;cycle++){
                end=SemaphoreWaitMetricsTest.step(sim,cycle);
                if(finished<0&&end.metrics().global().pending()==0)finished=cycle;
            }
            return new Measurement(name,kind,config,horizon,finished,end);
        }
    }
    private static Measurement[] measureAll(){
        Measurement[] data=new Measurement[8];SchedulingPolicyType[] policies={SchedulingPolicyType.FCFS,SchedulingPolicyType.EDF,SchedulingPolicyType.ROUND_ROBIN,SchedulingPolicyType.PREEMPTIVE_PRIORITY};
        ProcessDefinition[] jobs={cpu("A",1,8,9,2),cpu("B",1,2,3,0),cpu("C",1,4,7,1),cpu("D",2,6,20,1)};
        for(int i=0;i<policies.length;i++)data[i]=measure(policies[i].name(),"policy",new SimulationConfig(500,2,machines(policies[i]),new BufferDefinition[0],jobs),12);
        for(int variant=0;variant<4;variant++){
            boolean remote=variant%2==1,pair=variant>=2;ProcessDefinition producer=SemaphoreWaitMetricsTest.participant("P",remote?2:1,ProcessType.PRODUCER,100);
            ProcessDefinition[] participants=pair?new ProcessDefinition[]{SemaphoreWaitMetricsTest.participant("C",1,ProcessType.CONSUMER,100),producer}:new ProcessDefinition[]{producer};
            data[4+variant]=measure((pair?"pair-":"producer-")+(remote?"remote":"local"),"producerConsumer",new SimulationConfig(500,2,machines(SchedulingPolicyType.FCFS),new BufferDefinition[]{new BufferDefinition(1,1,1)},participants),pair?16:8);
        }
        return data;
    }
    private static void verify(Measurement[] data){
        long[] complete={2,3,1,3},service={14,15,15,15},responded={2,4,4,4},finished={8,9,9,9};double[] response={0,2,1.25,2},jain={.49,225.0/260,225.0/308,225.0/260};
        for(int i=0;i<4;i++){
            PerformanceMetrics m=data[i].result().metrics().global();equal(complete[i],m.completed());equal(4-complete[i],m.expired());equal(0L,m.pending());equal(service[i],m.userCycles());equal(0L,m.kernelCycles());equal(responded[i],m.responded());equal(finished[i],data[i].allTerminatedAt());equal(12,data[i].result().sampleCount());
            PerformanceMetricsTest.near(response[i],m.averageResponse());PerformanceMetricsTest.near(jain[i],m.fairness());PerformanceMetricsTest.near(complete[i]/12.0,m.throughput());PerformanceMetricsTest.near(service[i]/24.0,m.utilization());PerformanceMetricsTest.near(complete[i]/4.0,m.deadlineCompliance());
        }
        for(int i=4;i<8;i++){
            Measurement r=data[i];boolean pair=i>=6;PerformanceMetrics m=r.result().metrics().global();equal(pair?2L:1L,m.completed());equal(0L,m.expired());equal(pair?12L:6L,m.userCycles()+m.kernelCycles());equal(1L,r.result().metrics().bufferAt(0).produced());equal(pair?1L:0L,r.result().metrics().bufferAt(0).consumed());equal(pair?0:1,r.result().metrics().bufferAt(0).occupancy());PerformanceMetricsTest.near(.375,m.utilization());PerformanceMetricsTest.near(.125,m.throughput());equal(r.horizon(),r.result().sampleCount());
        }
        equal(6L,data[4].allTerminatedAt());equal(8L,data[5].allTerminatedAt());equal(12L,data[6].allTerminatedAt());equal(13L,data[7].allTerminatedAt());PerformanceMetricsTest.near(6,data[6].result().metrics().waits().averageWait());PerformanceMetricsTest.near(7,data[7].result().metrics().waits().averageWait());PerformanceMetricsTest.near(.5,data[6].result().metrics().global().averageResponse());PerformanceMetricsTest.near(0,data[7].result().metrics().global().averageResponse());
    }
    public static void run(){verify(measureAll());}
    private static void number(JsonWriter w,String name,double value)throws IOException{w.name(name);if(Double.isFinite(value))w.value(value);else w.nullValue();}
    private static void metrics(JsonWriter w,PerformanceMetrics m)throws IOException{
        w.beginObject().name("cycles").value(m.cycles()).name("processors").value(m.processors()).name("completed").value(m.completed()).name("expired").value(m.expired()).name("pending").value(m.pending()).name("responded").value(m.responded()).name("admitted").value(m.admitted()).name("userCycles").value(m.userCycles()).name("kernelCycles").value(m.kernelCycles()).name("responseCycles").value(m.responseCycles()).name("serviceSquared").value(m.serviceSquared());
        number(w,"throughput",m.throughput());number(w,"utilization",m.utilization());number(w,"averageResponse",m.averageResponse());number(w,"deadlineCompliance",m.deadlineCompliance());number(w,"fairness",m.fairness());w.endObject();
    }
    private static void waits(JsonWriter w,SemaphoreWaitMetrics m)throws IOException{w.beginObject().name("completedEpisodes").value(m.completedEpisodes()).name("completedCycles").value(m.completedCycles()).name("openEpisodes").value(m.openEpisodes()).name("openCycles").value(m.openCycles());number(w,"averageWait",m.averageWait());w.endObject();}
    public static void main(String[] args)throws IOException{
        if(args.length!=1)throw new IllegalArgumentException("Indica la ruta JSON de resultados");Measurement[] data=measureAll();verify(data);Path file=Path.of(args[0]).toAbsolutePath();Files.createDirectories(file.getParent());
        try(Writer output=Files.newBufferedWriter(file,StandardCharsets.UTF_8);JsonWriter w=new JsonWriter(output)){
            w.setStrictness(Strictness.STRICT);w.setIndent("  ");w.beginObject().name("dataVersion").value(1).name("coreBaseline").value("e2c489d").name("units").value("Ciclos lógicos; utilización/cumplimiento/Jain como fracciones; null = sin denominador").name("experiments").beginArray();
            for(Measurement r:data){
                StringWriter config=new StringWriter();ScenarioJson.write(config,r.config());w.beginObject().name("name").value(r.name()).name("kind").value(r.kind()).name("horizonCycles").value(r.horizon()).name("allTerminatedAtBoundary").value(r.allTerminatedAt()).name("configuration").jsonValue(config.toString()).name("globalMetrics");metrics(w,r.result().metrics().global());w.name("semaphoreWaits");waits(w,r.result().metrics().waits());w.name("computers").beginArray();
                for(int i=0;i<r.result().metrics().computerCount();i++){w.beginObject().name("id").value(r.result().metrics().computerIdAt(i)).name("metrics");metrics(w,r.result().metrics().computerAt(i));w.endObject();}w.endArray().name("buffers").beginArray();
                for(int i=0;i<r.result().metrics().bufferCount();i++){MetricsSnapshot.BufferMetrics b=r.result().metrics().bufferAt(i);w.beginObject().name("id").value(b.id()).name("produced").value(b.produced()).name("consumed").value(b.consumed()).name("occupancy").value(b.occupancy()).name("waits");waits(w,b.waits());w.endObject();}w.endArray().endObject();
            }
            w.endArray().endObject();
        }
        System.out.println("Comparaciones verificadas y exportadas: "+file);
    }
}
