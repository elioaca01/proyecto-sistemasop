package avilaos.ui.session;
/** engineRequestId=-1 indica una acción de sesión, no un comando del motor. */
public record SessionNotice(long actionId,long engineRequestId,boolean error,String message) { }
