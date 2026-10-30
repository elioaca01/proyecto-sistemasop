package avilaos.config;

import java.io.IOException;
import java.nio.file.Path;

/** Persistencia del escenario, independiente de los hilos y del estado de ejecución. */
public interface ConfigurationStore {
    SimulationConfig load(Path file)throws IOException;
    void save(Path file,SimulationConfig config)throws IOException;
}
