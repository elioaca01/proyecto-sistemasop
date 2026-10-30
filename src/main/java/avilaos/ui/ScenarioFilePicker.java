package avilaos.ui;

import java.awt.Component;
import java.nio.file.Path;

/** Solo elige una ruta; null representa cancelar y no debe producir una orden. */
public interface ScenarioFilePicker {
    Path chooseOpen(Component parent);
    Path chooseSave(Component parent);
}
