package avilaos.tests;
import avilaos.ui.session.*;
import java.util.function.Predicate;
final class SessionTestSupport {
    static SessionView await(UiSession s,Predicate<SessionView> predicate){
        long end=System.nanoTime()+3_000_000_000L;
        while(System.nanoTime()<end){SessionView v=s.view();if(predicate.test(v))return v;Thread.yield();}
        SessionView v=s.view();throw new AssertionError("Sesión no alcanzó condición: "+v.phase()+" pendientes="+v.pendingActions()+" avisos="+v.noticeCount());
    }
    static void close(UiSession s){s.requestClose();try{TestSupport.isTrue(s.awaitClosed(3000));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}}
}
