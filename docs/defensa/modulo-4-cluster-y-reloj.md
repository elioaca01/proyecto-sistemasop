# Módulo 4 — Clúster y reloj

## Qué hicimos

Integramos al menos dos computadores, cada uno con su hilo, y un reloj global. Añadimos pausa, paso por ciclo, ejecución continua, cierre ordenado, comandos seguros e instantáneas inmutables. Estas funciones se prueban en el motor; los botones y paneles de la GUI se incorporarán en el módulo de interfaz.

## Conceptos esenciales

| Concepto | Qué debes entender |
| --- | --- |
| Hilo | Flujo real de ejecución de Java. Hay un coordinador y un worker por computador. |
| Concurrencia | Varios hilos tienen trabajo y se coordinan. El orden fijo por ID permite repetir el resultado. |
| Paralelismo | Ejecución simultánea en núcleos físicos. Nuestro reloj concede turnos por ID y espera cada confirmación. |
| Semáforo de inicio/fin | El reloj da permiso para un ciclo y espera el aviso de que terminó. |
| Exclusión mutua | Un semáforo de un permiso protege una estructura compartida de accesos incompatibles. |
| Instantánea inmutable | Copia estable de CPU, PCB, RAM y colas que puede leer otro hilo sin modificar el motor. |
| Comando | Solicitud que se guarda y aplica en un límite de ciclo seguro. |

Los semáforos Java coordinan hilos reales. Los semáforos simulados de productores/consumidores pertenecen al siguiente módulo.

## Dónde está en el código

- `ComputerConfig` conserva la configuración inicial validada.
- `ComputerWorker` ejecuta un computador en su Thread y confirma trabajo o error.
- `GlobalClock` controla los ciclos, pausa, pasos, ritmo y cierre mediante `ClockDriver`.
- `Simulation` es dueña de los computadores, la cola de comandos y los IDs globales.
- Los comandos permiten agregar procesos y cambiar planificación o duración del ciclo; `CommandResult` informa aceptación/rechazo.
- `ProcessSnapshot`, `ComputerSnapshot`, `SimulationSnapshot` y `SnapshotStore` publican copias versionadas y avisan a cada lector.

## Ejemplo para explicarlo

Máquina 1: A tiene 3 instrucciones y solicita E/S de duración 2 después de un cómputo. Máquina 2: B tiene 5 instrucciones y plazo 3. Ambos llegan en 0.

| Ciclo global | Máquina 1 | Máquina 2 |
| --- | --- | --- |
| 0 | A calcula | B calcula |
| 1 | A solicita E/S | B calcula |
| 2 | A espera | B calcula y vence |
| 3 | A espera | CPU inactiva |
| 4 | A continúa y termina | CPU inactiva |

En cada fila el reloj concede el turno a la máquina 1, espera su fin, hace lo mismo con la 2 y publica el resultado completo. El deadline de B sigue avanzando aunque A esté bloqueado. Cambiar milisegundos afecta el ritmo real, no esta traza lógica.

## Tres preguntas probables

- **¿Cómo evitan que la GUI cambie un PCB a mitad de ejecución?** Simulation recibe comandos y publica copias. Los comandos se aplican antes de avanzar los workers; no entrega sus computadores o PCB vivos al exterior.
- **¿Qué pasa al pausar o cerrar?** La pausa confirma el límite de un ciclo completo. Un paso concede un ciclo. El cierre termina los hilos propios y registra como rechazadas las órdenes pendientes sin aplicar.
- **¿Qué ocurre si falla una máquina?** Su worker confirma el error; el reloj pasa a FAILED y cierra los hilos. No confirma el ciclo global parcial. La instantánea conserva el avance individual para explicar qué ocurrió; se inicia otra Simulation para volver a ejecutar.

## Cómo lo comprobamos y qué puedes decir

Pasaron 1246 comprobaciones de trazas, órdenes concurrentes, IDs únicos, copias retenidas, lectores simultáneos, fallos e interrupciones. La [evidencia](../verification/2026-10-22-cluster.md) conserva los resultados y la revisión final.

**Explicación breve:** «Cada computador ejecuta en su hilo, pero un reloj coordina los turnos con semáforos. Solo avanzamos el ciclo global cuando todos confirman. Las órdenes entran entre ciclos y los lectores reciben instantáneas estables».
