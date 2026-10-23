package avilaos.tests;
import javax.swing.SwingUtilities;
final class SwingTestSupport {
    static <T extends java.awt.Component> T find(java.awt.Container root,String name,Class<T> type){
        for(java.awt.Component c:root.getComponents()){
            if(name.equals(c.getName())&&type.isInstance(c))return type.cast(c);
            if(c instanceof java.awt.Container child){T found=findOrNull(child,name,type);if(found!=null)return found;}
        }
        throw new AssertionError("Componente ausente: "+name);
    }
    private static <T extends java.awt.Component> T findOrNull(java.awt.Container root,String name,Class<T> type){
        for(java.awt.Component c:root.getComponents()){
            if(name.equals(c.getName())&&type.isInstance(c))return type.cast(c);
            if(c instanceof java.awt.Container child){T found=findOrNull(child,name,type);if(found!=null)return found;}
        }return null;
    }
    static void edt(Runnable action){try{if(SwingUtilities.isEventDispatchThread())action.run();else SwingUtilities.invokeAndWait(action);}catch(Exception e){throw new AssertionError("Fallo en EDT",e);}}
}
