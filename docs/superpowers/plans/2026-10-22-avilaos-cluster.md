# ÁvilaOS Cluster and Clock Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** ejecutar al menos dos computadores con hilos reales, un reloj coordinador, comandos seguros e instantáneas inmutables, con pausa/paso/cierre y propagación de fallos comprobados.

**Architecture:** Simulation conserva la propiedad del clúster. GlobalClock aplica comandos en límites de ciclo y coordina un ComputerWorker por máquina en orden ascendente de ID, mediante semáforos de inicio/fin. Solo se publican instantáneas después de estabilizar el ciclo o detenerlo por error; la futura GUI usa comandos y copias, sin acceso al modelo mutable.

**Tech Stack:** Java 25, Thread, Semaphore, TimeUnit, Maven/NetBeans y estructuras propias; sin dependencias nuevas.

**Spec:** [diseño aprobado](../specs/2026-10-18-avilaos-design.md), secciones 3, 5 y 6; módulo 4 de la [hoja de ruta](2026-10-19-avilaos-roadmap.md). Continúa las 916 comprobaciones de los módulos 1–3.

**Execution:** mantener ejecución nativa, revisión independiente final y carpeta compartida con NetBeans. El estudiante solicitó comenzar el módulo; este plan concreto queda pendiente de su revisión antes de implementar.

## Global Constraints

- JDK de desarrollo propuesto: 25, sin características preview. Cumple «versiones posteriores a Java 21».
- GUI: Swing, incluida en Java; sin bibliotecas externas para componentes de interfaz. Este módulo conserva la ventana inicial.
- No usar colecciones del framework de Java: tampoco List, Map, Set, PriorityQueue, Collections, Arrays.asList o resultados de streams que creen colecciones. Usar arreglos y estructuras propias.
- No delegar el ordenamiento de colas a utilidades o bibliotecas.
- Un hilo GlobalClock coordina un ComputerWorker por computador. Cada worker usa un semáforo Java de inicio y otro de fin. Swing conserva su hilo de eventos.
- El reloj despierta un worker, espera su fin y pasa al siguiente; el ciclo global concluye cuando todos avanzaron exactamente una vez.
- Los IDs son secuenciales para toda la simulación, asignados por el coordinador al aplicar comandos; no se reutilizan durante una ejecución.
- Las estructuras compartidas reales se protegen mediante Semaphore Java: cola de comandos, registro compartido y publicación/lectura de instantáneas.
- Las excepciones de un worker se comunican al reloj y detienen la simulación con un error visible, sin dejarlo esperando indefinidamente.
- La velocidad en milisegundos solo controla el ritmo visual del reloj. No cambia las métricas expresadas en ciclos ni simula E/S con Thread.sleep por proceso.
- La GUI no adquiere bloqueos esperando que finalice una simulación ni modifica directamente los PCB. pause/resume/step/stop son solicitudes cortas; las esperas explícitas son para pruebas o tareas fuera del hilo de eventos.
- Conservar pruebas anteriores y guía breve por módulo. Productor-consumidor/red, controles Swing completos, JSON y métricas quedan en módulos posteriores.

## Decisiones de ciclo y vida útil

- Simulation recibe un arreglo defensivo de ComputerConfig, exige al menos dos máquinas con IDs positivos únicos y construye nuevas Computer ordenadas por ID. No acepta computadores ya avanzados ni expone los suyos mediante accesores mutables.
- El constructor valida antes de iniciar hilos; arranca workers/reloj y queda PAUSED en ciclo siguiente 0. Si falla el arranque, cierra los hilos ya iniciados antes de propagar el error.
- Estados: PAUSED, RUNNING, STOPPING, STOPPED, FAILED. El reinicio posterior se implementará creando una nueva Simulation; una instancia detenida o fallida no se reanuda.
- resume inicia avance continuo; pause solicita terminar el ciclo completo en curso y quedar PAUSED; step solo se acepta estando pausado y concede exactamente un ciclo. Varias solicitudes de paso acumulan ciclos individuales; resume absorbe los pasos aún pendientes en su ejecución continua. stop es terminal y no bloquea esperando joins.
- Una solicitud de pausa durante espera de ritmo despierta al reloj sin iniciar otro ciclo. Si llega después de conceder el ciclo, este se completa. La instantánea PAUSED confirma que ningún worker está mutando el modelo.
- Orden: drenar un lote de comandos, aplicarlo en FIFO, avanzar todos los workers una vez, incrementar nextCycle, publicar. Un comando encolado después de drenar ese lote espera al próximo ciclo. Los comandos en PAUSED esperan step/resume; no avanzan procesos por su cuenta.
- Cambiar política/quantum usa la API pendiente de Kernel del módulo 3 antes de avanzar workers. Cambiar duración modifica la espera de ritmo después de ese ciclo. El paso manual no espera los milisegundos configurados.
- Los comandos inválidos esperables se rechazan con un resultado sin detener el reloj ni consumir un ID de proceso. Un fallo inesperado del motor produce FAILED, cierra workers y conserva un mensaje de error.
- Ante fallo de un worker, el contador global no confirma ese ciclo. Algunas máquinas pueden haberlo avanzado; se publica FAILED con sus nextCycle individuales, sin presentar el ciclo parcial como exitoso ni intentar rollback o reanudación.
- Al detenerse, se rechazan con resultado explícito los comandos aceptados que no llegaron a aplicarse. Una bandera de aceptación protegida por el semáforo de la cola impide aceptar comandos después de cerrarla.
- close solicita stop y espera cierre con límite; es idempotente y se usa fuera del hilo Swing. awaitStopped(timeoutMillis) permite comprobar cierre sin espera indefinida. El trabajo ordinario de Tickable es finito; no se usará Thread.stop para interrumpir código arbitrario.

## Review Focus

1. Fallar un worker después de que otra máquina avanzó: informar FAILED, no confirmar el ciclo ni bloquear el reloj esperando un permiso que nunca llega; tareas 3, 4 y 6.
2. pause/step/stop durante espera de ritmo o un ciclo: no producir ciclos extra, no cerrar a medias una captura y no dejar hilos propios vivos; tareas 4 y 6.
3. Una instantánea retenida, o los arreglos usados para crearla, no deben permitir modificar el estado vivo ni cambiar al avanzar el clúster; tareas 1, 2 y 6.
4. Comandos concurrentes, inválidos o aceptados justo antes de stop: aplicar/rechazar cada solicitud una sola vez, en el lote correcto, sin perder resultados ni reutilizar IDs; tareas 5 y 6.
5. Los distintos ritmos, IDs/configuraciones de máquinas y lectores simultáneos deben conservar el orden determinista y la publicación completa, sin lecturas directas de PCB ni deadlocks; tareas 2 y 6.

## Verificación

`BUILD` significa, desde la raíz:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -o -B -Dmaven.repo.local=.maven-cache test
```

Esperado: `OK: N comprobaciones`, N >= 916, y `BUILD SUCCESS`. Registrar cada nueva suite en AllTests. Las pruebas de hilos usan semáforos/barreras y esperas con tiempo límite, no Thread.sleep para suponer que un hilo ya avanzó. Las aserciones y el contador TestSupport se ejecutan en el hilo de pruebas; los auxiliares comunican resultados/excepciones al hilo principal. Todo test cierra sus hilos en finally y verifica que finalizaron.

## Task 1: configuración e instantáneas sin referencias mutables

**Files:** crear en `src/main/java/avilaos/simulation/`: `ComputerConfig.java`, `SimulationState.java`, `ProcessSnapshot.java`, `ComputerSnapshot.java`, `CommandResult.java`, `SimulationSnapshot.java`; prueba `src/test/java/avilaos/tests/SnapshotTest.java`.

**Interfaces:** ComputerConfig es record `(int id, int ramCapacity, SchedulingPolicyType policy, int quantum)` con validaciones del motor. ProcessSnapshot es record con copia de todos los campos públicos de PCB y su ProcessDefinition inmutable, con `static capture(ProcessControlBlock)`. ComputerSnapshot es clase final con `static capture(Computer)`, accesores `id()`, `nextCycle()`, `ramCapacity()`, `ramUsed()`, `ramFree()`, `mode()`, `lastInstruction()`, `lastProcessId()`, `policyType()`, `quantum()`, `sliceUsed()`, `running()` nullable y `newCount/newAt`, `readyCount/readyAt`, `blockedCount/blockedAt`, `terminatedCount/terminatedAt` devolviendo solo ProcessSnapshot. SimulationSnapshot es clase final con constructor `(long version, long nextCycle, SimulationState state, long cycleMillis, String error, ComputerSnapshot[] computers, CommandResult[] results)`, accesores escalares homónimos y arreglos privados copiados de ComputerSnapshot/CommandResult; `computerCount()/computerAt(int)`, `resultCount()/resultAt(int)`. CommandResult es record `(long requestId, long appliedCycle, boolean accepted, long processId, String message)`; processId=-1 si no crea proceso y appliedCycle=-1 si se descarta antes de aplicar por cierre. Ningún accessor entrega arreglos ni PCB/Computer vivos.

- [ ] Escribir SnapshotTest: capturar una Computer con A RUNNING y B READY, retener copia, avanzar hasta terminar y verificar que la copia conserva PC/estados/RAM originales. Comprobar campos de una IO bloqueada y una terminada por deadline.
- [ ] Probar copia defensiva de arreglos de SimulationSnapshot, validaciones de ComputerConfig, IDs/orden y accesos indexados; los datos de ProcessDefinition no se modifican.
- [ ] BUILD: esperado fallo por clases ausentes. Implementar capturas por campos, no envoltorios del objeto mutable; construir orden de colas igual al núcleo.
- [ ] BUILD: esperado éxito; commit `feat: captura estado inmutable de computadores y procesos`.

## Task 2: publicación y espera de instantáneas con semáforos

**Files:** crear `simulation/SnapshotStore.java`, prueba `tests/SnapshotStoreTest.java` bajo las raíces existentes.

**Interfaces:** `SnapshotStore(SimulationSnapshot initial)`, `SimulationSnapshot get()`, `void publish(SimulationSnapshot next)`, `SimulationSnapshot awaitNewer(long version, long timeoutMillis) throws InterruptedException`. publish exige versión estrictamente mayor y no-null; await exige versión >= 0 y timeout > 0, devuelve copia ya disponible o espera una versión mayor; devuelve null si vence el tiempo. Un Semaphore(1) protege referencia y registro de lectores; cada lector pendiente usa su propio semáforo de aviso en lista propia para despertar a todos. Registrar/comprobar versión bajo el mismo mutex evita despertares perdidos; timeout/interrupción retira al lector.

- [ ] Pruebas: get inicial, publicación válida, rechazo atómico de versión vieja/nulo, lectura de una versión ya publicada y timeout acotado sin publicación.
- [ ] Dos lectores registrados esperan la misma versión, publish los libera a ambos; interrupción/timeout de uno no consume el aviso del otro ni bloquea publicaciones futuras. Coordinar inicio con semáforos, y limitar joins a 2 segundos.
- [ ] BUILD: esperado fallo por SnapshotStore ausente. Implementar acquire/release con finally y limpieza de lectores.
- [ ] BUILD: esperado éxito; commit `feat: publica instantaneas protegidas por semaforos`.

## Task 3: hilo de computador con relevo inicio/fin

**Files:** crear `simulation/ComputerWorker.java`, prueba `tests/ComputerWorkerTest.java`.

**Interfaces:** `ComputerWorker(int computerId, Tickable target)`, `void start()`, `void executeCycle(long cycle) throws InterruptedException`, `int computerId()`, `boolean isAlive()`, `void close()` (AutoCloseable). Tiene Thread propio y Semaphore de inicio/fin; executeCycle es una llamada del coordinador que entrega un ciclo y espera su resultado. Una exclusión de peticiones impide dos entregas simultáneas. Validar secuencia 0.. y Long.MAX_VALUE antes de entregar; antes de start, tras fallo o tras cierre no se aceptan ciclos. start no es repetible. Publicar el resultado y liberar fin en finally, también ante Throwable del target; propagar IllegalStateException con causa al coordinador. close señala cierre, interrumpe la espera de inicio y hace join acotado a 2 segundos; no depende del mutex retenido por executeCycle.

- [ ] Target registra ciclo y nombre/identidad de Thread: ejecutar 0/1 ocurre en el worker, no en quien llama; cada petición ejecuta una sola vez. Rechazos de repetición/salto no invocan target ni rompen el siguiente ciclo válido.
- [ ] Target lanza IllegalStateException en 0: el coordinador recibe causa sin quedar esperando fin; el worker termina y no acepta otro ciclo. Verificar close sin trabajo, close repetido y cierre tras fallo, sin hilos propios vivos.
- [ ] BUILD: esperado fallo por worker ausente. Implementar el protocolo con resultado visible por relevo de semáforos, sin bucle de sondeo.
- [ ] BUILD: esperado éxito; commit `feat: ejecuta cada computador en su hilo coordinado`.

## Task 4: reloj, controles y propagación de fallo

**Files:** crear `simulation/ClockDriver.java`, `simulation/GlobalClock.java`, prueba `tests/GlobalClockTest.java`.

**Interfaces:** ClockDriver define `long advanceCycle(long cycle)` (devuelve duración activa tras aplicar comandos), `void publishState(long nextCycle, SimulationState state, long cycleMillis, String error)` y `void shutdown()`. GlobalClock recibe `(ClockDriver driver, long cycleMillis)` positivo; ofrece `start()`, `resume()`, `pause()`, `step()`, `stop()`, `state()`, `boolean isAlive()`, `boolean awaitStopped(long timeoutMillis) throws InterruptedException`, `close()`. Control protegido por Semaphore(1), aviso con otro Semaphore y confirmación de cierre; no retener mutex de control al ejecutar driver, esperar ritmo, publicar o cerrar. Los callbacks son del hilo del reloj. Una espera de ritmo interruptible por aviso permite pausa/cierre inmediatos aunque duración sea grande. No aceptar controles de avance antes de start ni tras solicitud terminal.

- [ ] Driver registra llamadas usando semáforos: start queda PAUSED sin ticks; un step produce solo ciclo 0 y vuelve PAUSED; dos pasos producen 1/2. resume produce ciclos consecutivos; pause confirma un límite estable. No aceptar step cuando RUNNING.
- [ ] Con duración 60 000 ms, pause/stop despiertan la espera sin esperar un minuto. Pausa solicitada dentro de un ciclo permite terminarlo y no concede otro. No usar sleeps como barreras.
- [ ] Driver falla en advance: state FAILED, error legible, nextCycle sin confirmar ese ciclo, shutdown invocado una vez y cierre anunciado. Stop en PAUSED y en RUNNING cierra una vez; resume tras STOPPED/FAILED se rechaza. Preservar interrupciones, publicar estado terminal también si falla shutdown.
- [ ] BUILD: esperado fallo por reloj/driver ausentes. Implementar ciclo/control de vida con try/finally y cierre garantizado para trabajo finito; callbacks fallidos producen error terminal, no reinicio automático.
- [ ] BUILD: esperado éxito; commit `feat: coordina ciclos pausa paso y cierre del reloj`.

## Task 5: clúster, comandos e IDs globales

**Files:** crear `simulation/{Simulation,SimulationCommand,AddProcessCommand,ChangeSchedulingCommand,ChangeCycleDurationCommand}.java`, pruebas `tests/SimulationTest.java`.

**Interfaces:** Simulation implementa ClockDriver y AutoCloseable; constructor `(ComputerConfig[] configs, long cycleMillis)` valida/copia, construye computadores/workers ordenados por ID y arranca pausado. API pública: `long enqueue(SimulationCommand command)`, `SimulationSnapshot snapshot()`, `SimulationSnapshot awaitNextSnapshot(long afterVersion, long timeoutMillis) throws InterruptedException`, delegados resume/pause/step/stop/awaitStopped/close. No hay getter de Computer/PCB. SimulationCommand es interfaz sellada con `long apply(Simulation simulation)`; solo permite los tres records de comandos y sus datos inmutables. apply devuelve ID de proceso creado o -1. Los callbacks ClockDriver y métodos internos de aplicación verifican que llama el hilo coordinador; el constructor usa una captura inicial privada antes de arrancar el reloj. AddProcessCommand contiene ProcessDefinition; ChangeSchedulingCommand contiene computerId/type/quantum; ChangeCycleDurationCommand contiene cycleMillis. Validar campos estructurales en constructores; máquina inexistente o tipo de proceso aún no soportado se rechazan al aplicar mediante CommandResult.

**Propiedad:** cola de solicitudes/results con IDs bajo semáforo propio; registro global de PCB y siguiente ID de proceso bajo otro; publicación mediante SnapshotStore. Solo el reloj aplica comandos cuando no hay worker activo. Al iniciar ciclo extrae un lote completo y suelta el mutex antes de aplicarlo. Cada resultado queda conservado en el historial de instantáneas, indexado por orden de solicitud. Una cola cerrada rechaza enqueue atómicamente; shutdown convierte pendientes no aplicadas en resultados rechazados antes de publicar estado terminal.

- [ ] Configuración: rechazar null, menos de dos máquinas, IDs duplicados/datos inválidos sin dejar hilos vivos. Arreglo [ID 2,ID 1] se publica ordenado 1,2 y cambios al arreglo original no afectan al clúster.
- [ ] En PAUSED encolar A para máquina 2, B para máquina 1 y máquina desconocida: no ejecutar hasta step. Primer ciclo aplica en FIFO, asigna IDs globales 1/2 a A/B, rechaza la tercera sin consumir ID y ambas máquinas avanzan una vez. En siguiente lote, otro proceso recibe 3 y llegada igual al ciclo de aplicación. No hay migración ni reutilización de IDs terminados.
- [ ] Capturar una instantánea antes de más pasos: conserva PC, colas, memoria y resultados. Cambiar política/quantum de máquina 1 y duración en el mismo lote afecta al siguiente avance, sin cambiar la máquina 2.
- [ ] stop con comandos todavía en cola los publica rechazados; enqueue posterior falla y close repetido no crea duplicados. Rechazos de comandos no detienen el clúster ni cambian RAM/PC/colas de máquinas ajenas.
- [ ] BUILD: esperado fallo por Simulation/comandos ausentes. Implementar ensamblado y callbacks del reloj, capturar solo después de que todos los workers terminaron o después de cerrarlos por fallo.
- [ ] BUILD: esperado éxito; commit `feat: integra cluster comandos seguros e IDs globales`.

## Task 6: concurrencia integrada y trazas repetibles

**Files:** crear `tests/ClusterIntegrationTest.java`; modificar producción solo para corregir comportamientos reproducidos por las pruebas. Registrar en AllTests.

- [ ] Dos máquinas con CPU/E/S: avanzar pasos y comprobar nextCycle iguales al global, una instrucción máxima por máquina/ciclo, deadlines globales aunque una esté bloqueada e independencia de RAM/políticas. Repetir escenario con duraciones 1 y 1000 ms usando pasos: mismas trazas/resultados por ciclo.
- [ ] Driver de prueba compone workers ID 1 y 2; registra orden 1,2 y hilos diferentes del coordinador. El segundo falla después del primero: snapshot/error indica ciclo parcial, nextCycle global no avanza, no se solicita otro tick, todos los hilos terminan. Usar el mismo protocolo ClockDriver/ComputerWorker que producción, sin hooks exclusivos.
- [ ] Productores reales de comandos, sincronizados con semáforos: verificar IDs de solicitud únicos y resultados en orden de solicitud; rechazos no consumen IDs de proceso. Un comando entregado después de cerrar el lote queda para el siguiente ciclo. Pausa/cierre concurrentes no pierden una solicitud aceptada: tendrá resultado aplicado o rechazado.
- [ ] Lectores simultáneos retienen instantáneas mientras avanza el reloj: cada publicación no fallida es completa, las copias no cambian y el lector no puede modificar el motor. Esperas de versión despiertan también con publicación terminal.
- [ ] Ejecutar BUILD. Si falla comportamiento, registrar causa y corregir bajo TDD; si todas pasan inicialmente, registrar que esta tarea añade verificación integrada sobre las piezas ya implementadas, sin inventar un fallo.
- [ ] Ejecutar clean package y verificar que las 916 comprobaciones originales siguen en el runner. Commit `test: verifica coordinacion y cierre del cluster`.

## Task 7: explicación breve, evidencia y revisión

**Files:** README, hoja de ruta, índice de defensa; crear `docs/defensa/modulo-4-cluster-y-reloj.md` y `docs/verification/2026-10-22-cluster.md` (usar fecha real de ejecución).

- [ ] Guía breve con el formato acordado: qué hicimos, conceptos, clases, ejemplo de dos máquinas, tres preguntas y explicación oral. Distinguir concurrencia real, orden determinista y paralelismo; semáforos Java de coordinación frente a semáforos simulados futuros.
- [ ] Registrar trazas, número real de comprobaciones, construcción limpia, cierre de hilos y resultado de revisión. Incorporar la salida de NetBeans del módulo 3 aportada por el estudiante: 916 comprobaciones, JAR/install y BUILD SUCCESS, terminada el 21 de octubre a las 23:37:07 de Venezuela; no atribuir al fragmento Clean, cierre ni Debug.
- [ ] Ejecutar BUILD y git diff --check; revisar imports, defensas de arreglos, permisos liberados con finally y ausencia de hilos propios vivos al finalizar pruebas.
- [ ] Solicitar una única revisión independiente del rango completo según executing-plans. Corregir hallazgos importantes con reproducción que falle primero; registrar decisiones y menores diferidos si existen.
- [ ] Guardar rama funcional y commits sin inventar PR ni fusionar localmente por falta de remoto; commit `docs: explica y verifica cluster para la defensa`. El siguiente módulo es productor-consumidor local/remoto.

Estado: plan preparado y revisado contra el diseño; pendiente de aprobación para implementar.
