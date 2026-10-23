package avilaos.tests;
import avilaos.simulation.*;
import java.util.concurrent.Semaphore;
import static avilaos.tests.TestSupport.*;
public final class EventLogTest {
    public static void run() {
        EventLog log=new EventLog();throwsType(IllegalArgumentException.class,()->log.append(null));
        Semaphore start=new Semaphore(0);Throwable[] errors=new Throwable[2];Thread[] writers=new Thread[2];
        for(int j=0;j<2;j++){final int id=j;writers[j]=new Thread(()->{try{start.acquire();for(int i=0;i<50;i++)log.append(new SimulationEvent(i,id+1,i+1,"evento"));}catch(Throwable e){errors[id]=e;}});writers[j].start();}
        start.release(2);
        try {for(Thread t:writers){t.join(2000);isTrue(!t.isAlive());}}catch(InterruptedException e){throw new AssertionError(e);}finally{for(Thread t:writers)t.interrupt();}
        equal(null,errors[0]);equal(null,errors[1]);equal(100,log.size());
        SimulationEvent[] copy=log.copy();copy[0]=null;isTrue(log.at(0)!=null);
        SimulationEvent[] retained=log.copy();log.append(new SimulationEvent(51,1,1,"fin"));equal(100,retained.length);equal(101,log.size());
        avilaos.structures.LinkedList<SimulationEvent> list=new avilaos.structures.LinkedList<>();
        SimulationEvent first=new SimulationEvent(0,1,1,"primero"),last=new SimulationEvent(79999,1,1,"último");
        list.addLast(first);for(int i=1;i<79999;i++)list.addLast(new SimulationEvent(i,1,1,"registro"));list.addLast(last);
        SimulationEvent[] bulk=new SimulationEvent[80000];list.copyInto(bulk);equal(first,bulk[0]);equal(last,bulk[79999]);
        throwsType(IllegalArgumentException.class,()->list.copyInto(new SimulationEvent[1]));
        bulk[0]=null;equal(first,list.get(0));
    }
}
