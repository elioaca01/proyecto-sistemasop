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
    }
}
