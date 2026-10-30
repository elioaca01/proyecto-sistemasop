package avilaos.config;

import java.io.IOException;

/** Error de formato o validación con contexto del campo que debe corregirse. */
public final class ConfigurationException extends IOException {
    public ConfigurationException(String message){super(message);}
    public ConfigurationException(String message,Throwable cause){super(message,cause);}
}
