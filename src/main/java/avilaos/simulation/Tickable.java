package avilaos.simulation;

/** Componente que avanza un ciclo lógico. */
public interface Tickable {
    void onTick(long cycle);
}
