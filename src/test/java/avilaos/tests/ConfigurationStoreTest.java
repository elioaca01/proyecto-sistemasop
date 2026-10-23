package avilaos.tests;

import avilaos.config.*;
import avilaos.process.*;
import java.io.IOException;
import java.nio.file.*;
import static avilaos.tests.TestSupport.*;

public final class ConfigurationStoreTest {
    @FunctionalInterface interface IoAction{void run()throws IOException;}
    static void fails(IoAction action){try{action.run();throw new AssertionError("Se esperaba IOException");}catch(IOException expected){isTrue(expected.getMessage()!=null);}}
    static int entries(Path dir)throws IOException{int count=0;try(DirectoryStream<Path> stream=Files.newDirectoryStream(dir)){for(Path ignored:stream)count++;}return count;}
    static void deleteTree(Path path)throws IOException{if(path==null)return;if(Files.isDirectory(path)){try(DirectoryStream<Path> ds=Files.newDirectoryStream(path)){for(Path p:ds)deleteTree(p);}}Files.deleteIfExists(path);}
    public static void run(){
        Path temp=null;
        try{
            temp=Files.createTempDirectory("avilaos-json-");Path dir=temp,file=dir.resolve("escenario Ávila con espacios.json");ConfigurationStore store=new JsonConfigurationStore();
            SimulationConfig a=ScenarioJsonTest.read(ScenarioJsonTest.REMOTE),b=ScenarioJsonTest.mixed();store.save(file,a);ScenarioJsonTest.equivalent(a,store.load(file));equal(1,entries(dir));
            store.save(file,b);ScenarioJsonTest.equivalent(b,store.load(file));ScenarioJsonTest.equivalent(a,ScenarioJsonTest.read(ScenarioJsonTest.REMOTE));equal(1,entries(dir));
            String previous=Files.readString(file);
            SimulationConfig malformedName=new SimulationConfig(a.cycleMillis(),a.networkLatency(),new avilaos.simulation.ComputerConfig[]{a.computerAt(0),a.computerAt(1)},new avilaos.sync.BufferDefinition[0],
                new ProcessDefinition[]{new ProcessDefinition("bad"+(char)0xD800,1,ProcessType.CPU_BOUND,1,1,0,5,0,0,0,0,0)});
            fails(()->store.save(file,malformedName));equal(previous,Files.readString(file));equal(1,entries(dir));ScenarioJsonTest.equivalent(b,store.load(file));
            fails(()->store.load(dir.resolve("ausente.json")));fails(()->store.save(dir.resolve("sin padre").resolve("x.json"),a));equal(1,entries(dir));
            Path targetDirectory=Files.createDirectory(dir.resolve("destino.json"));Files.writeString(targetDirectory.resolve("conservar"),"original");
            fails(()->store.save(targetDirectory,a));equal("original",Files.readString(targetDirectory.resolve("conservar")));equal(2,entries(dir));
            Path bad=dir.resolve("corrupto.json");Files.write(bad,new byte[]{(byte)0xC3,0x28});fails(()->store.load(bad));
            Files.writeString(bad,"{}");fails(()->store.load(bad));equal(previous,Files.readString(file));equal(3,entries(dir));
        }catch(IOException e){throw new AssertionError(e);}
        finally{try{deleteTree(temp);}catch(IOException e){throw new AssertionError(e);}}
    }
}
