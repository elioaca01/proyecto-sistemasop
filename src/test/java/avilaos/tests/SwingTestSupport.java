package avilaos.tests;
import javax.swing.SwingUtilities;
final class SwingTestSupport {
    static void edt(Runnable action){try{if(SwingUtilities.isEventDispatchThread())action.run();else SwingUtilities.invokeAndWait(action);}catch(Exception e){throw new AssertionError("Fallo en EDT",e);}}
}
