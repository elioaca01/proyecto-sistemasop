package avilaos.ui.session;
import avilaos.config.SimulationConfig;
import avilaos.simulation.SimulationSnapshot;
/** Contexto inicial y estado vivo de una única generación, sin objetos mutables del motor. */
public final class SessionView {
    private final long generation;
    private final SessionPhase phase;
    private final boolean started;
    private final int pendingActions;
    private final SimulationConfig initialConfig;
    private final SimulationSnapshot simulation;
    private final SessionNotice[] notices;
    public SessionView(long generation,SessionPhase phase,boolean started,int pendingActions,SimulationConfig initialConfig,SimulationSnapshot simulation,SessionNotice[] notices){
        if(generation<0||phase==null||pendingActions<0||initialConfig==null||notices==null)throw new IllegalArgumentException("Vista de sesión inválida");
        this.generation=generation;this.phase=phase;this.started=started;this.pendingActions=pendingActions;this.initialConfig=initialConfig;this.simulation=simulation;this.notices=notices.clone();
    }
    public long generation(){return generation;} public SessionPhase phase(){return phase;} public boolean started(){return started;}
    public int pendingActions(){return pendingActions;} public SimulationConfig initialConfig(){return initialConfig;}
    public SimulationSnapshot simulation(){return simulation;} public int noticeCount(){return notices.length;} public SessionNotice noticeAt(int i){return notices[i];}
}
