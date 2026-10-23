# ÁvilaOS Producer Consumer and Network Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** ejecutar productores/consumidores locales y remotos con buffers acotados en RAM, tres semáforos simulados, latencia por elemento y limpieza correcta al terminar o vencer.

**Architecture:** BufferRegistry conserva buffers y reservas del anfitrión; BufferService coordina operaciones, permisos y eventos de despertar bajo un semáforo Java. El programa determina instrucciones y CPU las cuenta; Kernel conserva las transiciones. Simulation entrega despertares al inicio global, ejecuta los workers del módulo 4 y publica copias de buffers, operaciones y eventos.

**Tech Stack:** Java 25, Maven/NetBeans, Thread/Semaphore existentes y estructuras propias; sin dependencias nuevas.

**Spec:** [diseño aprobado](../specs/2026-10-18-avilaos-design.md), secciones 4–7 y 9; módulo 5 de la [hoja de ruta](2026-10-19-avilaos-roadmap.md). Continúa el clúster y las 1246 comprobaciones previas.

**Execution:** conservar método nativo, carpeta compartida con NetBeans y una revisión independiente final. Aprobado por el estudiante mediante «hazlo» el 23 de octubre de 2026; implementación nativa en curso. Incluir guía breve independiente del módulo 5.

## Global Constraints

- JDK de desarrollo propuesto: 25, sin características preview. Cumple «versiones posteriores a Java 21».
- GUI: Swing, incluida en Java; sin bibliotecas externas para componentes de interfaz. Conservar la ventana inicial en este módulo.
- No usar colecciones del framework de Java: tampoco List, Map, Set, PriorityQueue, Collections, Arrays.asList o resultados de streams que creen colecciones. Usar arreglos y estructuras propias.
- No delegar el ordenamiento de colas a utilidades o bibliotecas.
- Cada instrucción ejecutada cuesta un ciclo. semWait, semSignal, inserción y extracción se ejecutan en modo núcleo. PC y MAR avanzan una unidad por instrucción ejecutada; no avanzan cuando el proceso está bloqueado o no dispone de CPU.
- Un semWait que bloquea cuenta como instrucción ejecutada; al despertar, el proceso continúa en la siguiente fase con el permiso concedido, sin repetir el decremento.
- Productor: cómputo, wait(vacíos), wait(mutex), insertar, signal(mutex), signal(llenos).
- Consumidor: wait(llenos), wait(mutex), extraer, signal(mutex), signal(vacíos), cómputo.
- Cada operación completa añade cinco instrucciones de núcleo. Si hay instrucciones sobrantes, son cómputo posterior a la cuota, sin producir/consumir elementos adicionales.
- Un desbloqueo causado por una instrucción del ciclo t hace al proceso elegible en t+1, aunque su computador todavía no haya avanzado.
- Procesos bloqueados conservan su RAM. Cada posición de buffer cuesta una unidad de RAM del anfitrión.
- Para acceso remoto, antes de los cinco pasos de núcleo, esperar L ciclos: si comienza en t, retoma en t+L. Una espera por elemento, sin permisos de conteo ni mutex retenidos; L=0 ejecuta el primer paso en t.
- Mantener los hilos y protocolo de cierre existentes. Las esperas simuladas encolan PCB, no bloquean los workers Java.

## Decisiones de comportamiento

- Un buffer se crea por comando con ID positivo único, anfitrión existente y capacidad positiva; debe reservar toda su capacidad en RAM antes de registrarse. Falta de RAM/duplicado/anfitrión desconocido rechaza el comando sin estado parcial. No se incorpora eliminación de buffers.
- Cada proceso productor/consumidor debe referirse a un buffer existente antes de aceptarse. Conserva el computador elegido y puede compartir buffer con varios participantes locales/remotos. No cambiar ProcessDefinition ni su validación de presupuesto `(interval + 5L) * targetItems`.
- Programa puro por índice: período interval+5. Productor hace interval COMPUTE y cinco fases; consumidor hace cinco fases e interval COMPUTE. Tras targetItems períodos, el resto es COMPUTE. Usar long para sumas/productos. El intervalo cuenta únicamente instrucciones de cómputo ejecutadas.
- BufferOperation registra por PCB permiso de conteo adquirido, mutex, modificación confirmada aún no señalizada, operación/red iniciada y elementos completados. Una concesión directa desde signal actualiza primero estas reservas, aunque el PCB no sea elegible hasta t+1. El próximo paso nunca vuelve a adquirir un permiso ya concedido.
- Los semáforos simulados tienen contador no negativo y cola FIFO: wait toma permiso o encola; signal entrega al primero o incrementa. Sus clases no cambian PCB ni despiertan hilos reales. BufferService convierte una concesión en un evento para el Kernel.
- El mutex tiene dueño explícito, incluido un permiso entregado a un bloqueado que despertará después. Solo ese dueño puede insertar/extraer/liberar. Una inserción/extracción modifica contenido y contadores producidos/consumidos una sola vez.
- Finalizar una operación señala el semáforo opuesto y reinicia sus banderas. La limpieza por deadline retira esperas/red/despertares, devuelve permisos no utilizados y libera mutex. Si ya insertó, publica ese elemento mediante signal(llenos); si ya extrajo, publica el espacio mediante signal(vacíos). No deshace contenido confirmado ni ejecuta instrucciones adicionales.
- Las señales de limpieza también conceden elegibilidad en t+1. Limpiar repetidamente un PCB retirado no libera recursos de otros. Un NEW sin RAM de proceso puede tener registro lógico de operación, pero ningún permiso adquirido.
- La latencia es un entero global >= 0 configurado inicialmente. El constructor existente de Simulation delega a latencia 0; se añade uno con latencia explícita. No se incorpora cambio de latencia durante ejecución en este módulo.
- Iniciar red no consume instrucción. Antes de despachar un candidato READY, Kernel prepara recursos; si requiere red, lo pasa a BLOCKED mediante un nuevo método PCB exclusivo para esa transición y conserva firstRunCycle=-1 si nunca ejecutó. Un RUNNING que alcanza la primera fase de una operación puede bloquearse por red conservando su contexto. Se permite seleccionar otro READY en ese mismo ciclo porque aún no se ejecutó instrucción. Cpu.onTick sigue invocándose exactamente una vez, incluso si todos quedan bloqueados.
- BufferService.beginCycle(t) se invoca una vez por ciclo global, después del lote de comandos y antes de cualquier worker. Entrega eventos de red y semáforos disponibles; Simulation los pasa al Kernel de destino fuera del mutex del servicio. No hay ventajas de despertar por ID de máquina.
- Simulation conserva privados servicio/registro. Un Semaphore Java protege los datos compartidos del servicio; no se llama al Kernel ni se espera a workers sosteniéndolo. Los componentes de prueba independientes transfieren su propiedad al servicio mientras lo usan.
- Se añaden eventos inmutables de mantenimiento para la limpieza y contadores de tokens por buffer; el módulo 8 seguirá calculando métricas generales. Las instantáneas nuevas copian todos los arreglos y no exponen BufferOperation, PCB, semáforos o RingBuffer mutables.

## Review Focus

1. Deadline después de recibir permiso por signal y antes de despertar: devolverlo/transferirlo exactamente una vez, retirar todo evento futuro y conservar FIFO; tareas 4, 5 y 8.
2. Deadline con mutex, después de insertar/extraer o después de signal(mutex): conservar contenido confirmado, completar el semáforo opuesto y no liberar un mutex ya entregado a otro; tareas 4 y 8.
3. Latencia 0 o positiva, red seguida de espera por buffer y varios elementos: una latencia por elemento, sin instrucción ni permisos consumidos durante red; tareas 5, 6 y 8.
4. Despertar de máquina de ID mayor hacia menor y viceversa: en ambos casos disponible en t+1, sin doble encolado ni ejecución de una segunda instrucción; tareas 6 y 8.
5. Apropiación RR/EDF/prioridades en cada fase, cuotas con cómputo sobrante y RAM compartida con buffers: no perder reservas, repetir tokens, reactivar terminados ni romper las trazas anteriores; tareas 3, 6, 7 y 8.

## Interfaces y construcción

Raíces: producción `src/main/java/avilaos/`, pruebas `src/test/java/avilaos/tests/`. Cada tarea registra su suite en AllTests y conserva las anteriores; se actualiza únicamente una expectativa antigua si el comportamiento antes rechazado ahora queda soportado por este módulo.

`BUILD`:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -o -B -Dmaven.repo.local=.maven-cache test
```

Esperado: `OK: N comprobaciones` (N >= 1246) y BUILD SUCCESS. TDD en cada tarea: prueba, fallo por comportamiento/API ausente, implementación, suite completa y commit. Pruebas concurrentes con barreras y esperas acotadas; no sleeps para suponer sincronización. Al final ejecutar clean package y repetir escenarios concurrentes cuando cambios/fallos lo justifiquen.

## Task 1: semáforo simulado FIFO

**Files:** crear `sync/SimulatedSemaphore.java` y `tests/SimulatedSemaphoreTest.java`.

**Interfaces:** `SimulatedSemaphore(int initial, int maximum)` con 0 <= initial <= maximum y maximum > 0; `boolean waitFor(ProcessControlBlock p)`, `ProcessControlBlock signal()`, `boolean cancelWaiting(ProcessControlBlock p)`, `int value()`, `int waitingCount()`, `ProcessControlBlock waitingAt(int index)`. signal retorna el PCB al que entrega permiso o null; rechaza señal excedente si no hay espera y el contador está al máximo. La lista de espera es propia. Rechazar null, doble encolado/ID duplicado; cancelación por identidad, no libera permisos adquiridos ni modifica estados.

- [x] Con inicial 1/máximo 1: A toma, B/C esperan; signal devuelve B y después C, manteniendo valor 0. Señalar sin espera recupera 1; otra señal falla sin cambiarlo. Los PCB conservan estado y PC.
- [x] Cancelar B esperando hace que la siguiente entrega vaya a C; cancelar ausente/repetido devuelve false. Rechazos de argumentos, índices y alias con mismo ID no alteran la cola original.
- [x] BUILD: fallo esperado por clase ausente. Implementar contador y cola FIFO sin Semaphore Java para estas esperas de procesos.
- [x] BUILD exitoso; commit `feat: agrega semaforos simulados con espera FIFO`.

## Task 2: buffers, tokens y reservas del anfitrión

**Files:** crear `sync/{BufferDefinition,BufferItem,BoundedBuffer,BufferRegistry}.java` y `tests/BoundedBufferTest.java`.

**Interfaces:** BufferDefinition record `(int id, int hostId, int capacity)` valida positivos. BufferItem record `(long id, long producerId, int producerComputerId)` valida positivos. BoundedBuffer recibe BufferDefinition, usa RingBuffer<BufferItem>, vacíos(capacidad/capacidad), llenos(0/capacidad), mutex(1/1), dueño PCB y contadores long producidos/consumidos. Ofrece `definition()`, `size()`, `itemAt(int)`, `produced()`, `consumed()`, `empty()`, `full()`, `mutex()`, `mutexOwner()`, `void claimMutex(ProcessControlBlock p)`, `ProcessControlBlock releaseMutex(ProcessControlBlock p)`, `void insert(ProcessControlBlock p,BufferItem item)` y `BufferItem remove(ProcessControlBlock p)` con validación de dueño antes de mutar. claimMutex registra un permiso ya adquirido, sin descontarlo otra vez. releaseMutex devuelve el destinatario de signal y lo registra como nuevo dueño si hubo entrega directa. BufferRegistry: `registerRam(int hostId, Ram ram)`, `BoundedBuffer create(BufferDefinition definition)`, `BoundedBuffer get(int id)`, `int size()`, `BoundedBuffer getAt(int index)`. Conserva la reserva Ram.Reservation con cada buffer; mutaciones concurrentes posteriores pertenecen al BufferService.

- [x] Capacidad 2 conserva FIFO, rechaza sobrellenado/vacío, un no dueño no inserta/extrae/libera mutex. Producidos-consumidos coincide con ocupación; owner exclusivo y valores iniciales de los tres semáforos correctos.
- [x] RAM 10: buffer capacidad 4 deja 6 libres. Otro de 7 es rechazado sin reserva/registro parcial; duplicado y anfitrión inexistente tampoco cambian RAM. Reservas de buffers de anfitriones distintos son independientes.
- [x] BUILD: fallo por clases ausentes. Implementar con RingBuffer y listas propias; validar antes de reservar/insertar, no permitir eliminación.
- [x] BUILD exitoso; commit `feat: reserva RAM para buffers acotados y tokens FIFO`.

## Task 3: fases del programa y ejecución de instrucciones de núcleo

**Files:** crear `process/{BufferPhase,BufferProgram,ProcessProgram}.java`; modificar `hardware/Cpu.java`; pruebas `tests/BufferProgramTest.java`, ajustar el caso de productor no soportado en CpuTest.

**Interfaces:** BufferPhase enum `COMPUTE, WAIT_EMPTY, WAIT_FULL, WAIT_MUTEX, INSERT, REMOVE, SIGNAL_MUTEX, SIGNAL_FULL, SIGNAL_EMPTY, COMPLETE`. BufferProgram: `static BufferPhase phaseAt(ProcessDefinition definition, int instructionIndex)`, `static InstructionKind nextInstruction(ProcessControlBlock p)`; valida tipos productor/consumidor e índice 0..instructionCount; el índice final retorna COMPLETE. nextInstruction rechaza procesos sin pendientes. ProcessProgram.nextInstruction delega CPU/IO a LinearProgram y productores/consumidores a BufferProgram. Cpu valida/carga con ProcessProgram; COMPUTE usa USER y las demás instrucciones KERNEL. No incorpora semáforos ni recursos al hardware.

- [x] Intervalo 1/cuota 1: productor tiene COMPUTE, SEM_WAIT, SEM_WAIT, BUFFER_INSERT, SEM_SIGNAL, SEM_SIGNAL; consumidor SEM_WAIT, SEM_WAIT, BUFFER_REMOVE, SEM_SIGNAL, SEM_SIGNAL, COMPUTE. Verificar fases concretas que distinguen los dos wait/signal.
- [x] Dos elementos repiten el período; instrucciones extra tras la cuota son solo COMPUTE. Intervalo Integer.MAX_VALUE y límites de índice se calculan sin overflow ni cuota adicional.
- [x] Cpu ejecuta una instrucción por tick, cuenta USER/KERNEL y no bloquea ni modifica buffer por sí sola. Reemplazar la expectativa antigua de CpuTest que rechazaba cargar un productor por comprobación del nuevo soporte, manteniendo las demás comprobaciones. Computer sin servicio sigue rechazando productores.
- [x] BUILD: fallo esperado. Implementar despachador y fases puras; BUILD exitoso y commit `feat: ejecuta programas de productor y consumidor por fases`.

## Task 4: operaciones locales, concesiones y limpieza

**Files:** crear `sync/{BufferOperation,BufferOperationSnapshot,BufferWake,BufferService}.java`, `simulation/SimulationEvent.java`; pruebas `tests/BufferOperationTest.java`.

**Interfaces:** BufferService(BufferRegistry registry), `void registerProcess(ProcessControlBlock p)`, `void validate(ProcessDefinition definition)`, `BlockReason prepare(ProcessControlBlock p,long cycle)` (local retorna NONE), `BlockReason afterInstruction(ProcessControlBlock p,long cycle)`, `void cleanup(ProcessControlBlock p,long cycle,TerminationReason reason)`, `LinkedQueue<BufferWake> beginCycle(long cycle)`, `BufferOperationSnapshot operationSnapshot(ProcessControlBlock p)`. afterInstruction deriva la fase ejecutada desde pc-1 y aplica un efecto; exige RUNNING. beginCycle valida ciclos exactos desde 0 y retira los avisos disponibles. BufferWake es record interno del motor `(ProcessControlBlock process, BlockReason reason, long availableCycle)`; no cambia PCB. Semaphore Java protege servicio; entregar la cola de avisos fuera de ese mutex.

BufferOperation conserva banderas de permiso de conteo, mutex, modificación pendiente de señal, red iniciada/ciclo disponible, elementos completos y razón de espera. Su snapshot inmutable contiene `(long processId,int bufferId,BufferPhase phase,boolean countingPermit,boolean mutexHeld,boolean modified,boolean networkStarted,long networkAvailableAt,int completedItems,BlockReason waitingReason)`. La fase efectiva refleja el wait pendiente si está bloqueado; al recibir permiso pasa al siguiente paso ya contado en PC. SimulationEvent record `(long cycle,int computerId,long processId,String message)` registra mantenimiento; `eventCount()/eventAt(int)` en servicio entregan valores inmutables.

- [x] Caminar instrucciones reales con PCB y Cpu: consumidor espera llenos, productor inserta/señala; signal concede el permiso al consumidor pero su aviso es disponible solo en t+1. Al retomar no vuelve a decrementar llenos. Varios bloqueados se atienden FIFO; nunca se duplica PCB en dos esperas.
- [x] Limpieza antes de insertar: devuelve permiso de vacíos y libera/cancela mutex. Después de insertar: conserva token y producido, libera mutex y señala llenos. Simétrico consumidor antes/después de extraer: conservar extracción, señalar vacíos, consumido una vez.
- [x] Limpiar después de signal(mutex) no libera al nuevo dueño; si falta señal opuesta, la completa una vez. Limpiar de nuevo o limpiar NEW sin permisos no modifica reservas ajenas. Verificar eventos y PC/MAR sin incrementos de mantenimiento.
- [x] Concesión recibida pero aún no entregada al Kernel: cleanup retira el aviso y devuelve/transfiere el permiso al siguiente bloqueado para t+1, sin resucitar el vencido.
- [x] BUILD: fallo por API ausente. Implementar efectos y cleanup idempotente por contexto, sin rollback de modificaciones confirmadas; BUILD exitoso y commit `feat: coordina operaciones locales y libera permisos por deadline`.

## Task 5: latencia por elemento y avisos globales

**Files:** crear `sync/NetworkTransfer.java`; extender BufferService con constructor `(BufferRegistry registry,int latency)`; prueba `tests/BufferNetworkTest.java`.

**Interfaces:** NetworkTransfer recibe `(ProcessControlBlock process,int bufferId,long issuedCycle,int latency)` y expone proceso, bufferId, availableCycle calculado como issuedCycle+latency con Math.addExact; requiere latencia > 0. BufferService.latency() devuelve configuración. prepare inicia transferencia únicamente al primer paso de núcleo de cada elemento remoto; si L=0/local retorna NONE. Si L>0 retorna NETWORK y registra transferencia sin permisos. beginCycle entrega al comienzo de t+L; cleanup cancela transferencias/avisos propios. Latencia 0 es constructor predeterminado.

- [x] Remoto listo para primer paso en 1, L=2: disponible en 3, PC/MAR y permisos intactos durante 1/2; al volver no inicia otra red durante los cinco pasos ni tras esperar un semáforo. Dos elementos tienen exactamente dos esperas. Local con L=2 y remoto con L=0 no esperan.
- [x] Cancelar antes de disponibilidad retira todo evento sin alterar contenido/RAM/permisos; extrema suma temporal se rechaza antes de registrar. Una transferencia no retiene mutex ni permisos de conteo.
- [x] BUILD: fallo esperado por constructor/red ausentes. Implementar tickets y validación previa a mutación; BUILD exitoso y commit `feat: simula latencia remota una vez por elemento`.

## Task 6: integrar el núcleo sin instrucciones durante esperas

**Files:** modificar `kernel/Kernel.java`, `simulation/Computer.java`, `process/ProcessControlBlock.java`; pruebas `tests/BufferKernelTest.java`.

**Interfaces:** añadir constructor Kernel con BufferService tras quantum y constructor Computer `(int id,int ramCapacity,SchedulingPolicyType policy,int quantum,BufferService buffers)`; los anteriores conservan comportamiento sin servicio. Kernel.submit permite productores/consumidores solo con servicio y validación/registro exitosos; registrar después de validar ID local y antes de encolar. PCB añade `blockBeforeDispatch(BlockReason reason)` que solo acepta NETWORK desde READY, conserva RAM/PC y no registra primera ejecución. Kernel/Computer ofrecen `boolean unblockExternal(ProcessControlBlock p,BlockReason reason)` para un PCB propio por identidad, bloqueado con esa razón externa; nunca revive TERMINATED ni encola dos veces.

- [x] Antes del despacho preparar candidato; un READY que inicia red usa blockBeforeDispatch y permite seleccionar otro listo sin consumir instrucción. Un RUNNING puede iniciar red al alcanzar su primera fase de núcleo. Cpu.onTick avanza exactamente una vez, aunque no haya instrucción. Tras instrucción de buffer, afterInstruction decide NONE o bloqueo de semáforo; Kernel cambia estado/colas.
- [x] Finalizar valida y marca con PCB.terminate como antes, después invoca BufferService.cleanup, retira de CPU/colas y libera RAM de proceso. Cleanup acepta ese PCB TERMINATED y elimina su contexto al final; PC/MAR no avanzan. Despertares se aplican fuera del servicio; el deadline sigue contando en todas las esperas.
- [x] Local, consumidor primero en buffer vacío y productor después (intervalo 1/cuota 1/capacidad 1): consumidor wait en 0, productor COMPUTE en 1, wait vacíos 2, wait mutex 3, insert 4, signal mutex 5, signal llenos 6; consumidor retoma wait mutex 7, remove 8, signal mutex 9, signal vacíos 10, COMPUTE 11. PC no repite el wait de 0. Final: ocupación 0, vacíos 1, llenos 0, mutex 1, ninguna espera y RAM de procesos liberada.
- [x] Red con consumidor nunca ejecutado conserva firstRun=-1 hasta su primera instrucción real; un CPU bound listo aprovecha el ciclo de preparación. RR cuenta los cinco pasos de núcleo en su quantum y conserva recursos al apropiar.
- [x] BUILD: fallo por soporte ausente. Implementar circuito de preparación/selección y efectos sin segunda instrucción; BUILD completo y commit `feat: integra bloqueos de buffers y red en el nucleo`.

## Task 7: comandos y vistas del clúster

**Files:** crear `simulation/AddBufferCommand.java`, `sync/BufferSnapshot.java`; modificar SimulationCommand, Simulation, SimulationSnapshot, ComputerSnapshot y ProcessSnapshot; pruebas `tests/BufferSimulationTest.java`.

**Interfaces:** AddBufferCommand(BufferDefinition definition) crea buffer y devuelve -1 como processId; resultado y BufferSnapshot identifican el buffer, sin confundirlo con un proceso. Simulation añade constructor `(ComputerConfig[] configs,long cycleMillis,int networkLatency)`; el actual delega a 0. Construye BufferRegistry/Service privados, registra RAM anfitriona antes de arrancar workers e inyecta el mismo servicio a todas las máquinas. Después del lote llama beginCycle y entrega avisos a Computer.unblockExternal, antes de avanzar workers.

BufferSnapshot es final e inmutable: definición, size, produced/consumed, mutexOwnerId (-1 libre), valores vacíos/llenos/mutex, `itemAt(int)`, esperas por semáforo (`emptyWaitingCount/At`, `fullWaitingCount/At`, `mutexWaitingCount/At`, por ID) y operaciones (`operationCount/At`, BufferOperationSnapshot). BufferService ofrece `bufferCount()/snapshotAt(int)` con copias. ProcessSnapshot añade operation nullable y conserva capture(p); sobrecarga capture(p,operation). ComputerSnapshot añade capture(c,buffers); la versión actual sigue válida. SimulationSnapshot conserva constructor anterior y añade constructor con networkLatency, BufferSnapshot[] y SimulationEvent[]; getters networkLatency, bufferCount/bufferAt, eventCount/eventAt, arreglos privados copiados.

- [x] En un lote: crear buffer y después procesos válidos funciona; invertir orden rechaza solo el proceso (sin consumir ID), permitiendo crearlo luego. Rechazar anfitrión/ID/capacidad/RAM y buffer inexistente sin estado parcial. Dos buffers anfitriones distintos descuentan solo su RAM. El buffer sigue reservando su capacidad tras terminar sus procesos.
- [x] Snapshot conserva tokens/origen, dueño, permisos, esperas, fase y eventos tras más ciclos. Alterar arreglos originales no modifica copias. Publicación normal contiene estado consistente tras todos los workers.
- [x] BUILD: fallo esperado por APIs ausentes. Implementar enlace/serialización por campos sin acceso mutable externo; BUILD y clean package exitosos, commit `feat: publica buffers y acepta comandos de productor consumidor`.

## Task 8: aceptación local/remota y conservación de invariantes

**Files:** crear `tests/ProducerConsumerIntegrationTest.java`; correcciones de producción solo ante comportamientos reproducidos.

- [x] Repetir la traza local literal de tarea 6 desde Simulation real, con pasos y comandos. Buffer capacity 1, mismo anfitrión y varios productores/consumidores; verificar FIFO de tokens y esperas, exclusividad del mutex y PC/servicio una vez por instrucción.
- [x] Remoto productor intervalo 1/L=2: COMPUTE 0; red en 1/2 sin instrucciones; WAIT_EMPTY 3, WAIT_MUTEX 4, INSERT 5, SIGNAL_MUTEX 6, SIGNAL_FULL 7. Con L=0 o local ejecuta los cinco pasos en 1..5. Consumer remoto con buffer vacío espera red una vez y después llenos, sin repetir latencia al recibir el permiso.
- [x] Señal desde computador de ID menor hacia mayor y mayor hacia menor: ambos receptores solo elegibles en t+1. Cuota 2 y presupuesto sobrante: dos tokens como máximo, resto COMPUTE. Latencias distintas cambian tiempos, no conteos finales.
- [x] Deadlines en NEW, red, espera vacíos/llenos/mutex, permiso concedido antes de despertar, mutex adquirido, insert/remove confirmado y después de liberar mutex. En cada caso no quedan referencias/eventos del terminado; verificar permisos y contenido confirmado, sin doble señal ni despertar tardío.
- [x] Ejecutar escenarios pequeños con FCFS, EDF, RR quantum 1 y prioridades: apropiación en fases críticas conserva reservas. En cada publicación verificar ocupación [0,cap], producido-consumido=ocupación, dueño único y contador libre+reservado+modificación pendiente. Verificar exactamente: vacíos libres + permisos de productor aún no insertados + extracciones aún no señalizadas = capacidad-ocupación; llenos libres + permisos de consumidor aún no extraídos + inserciones aún no señalizadas = ocupación; mutex libre + dueño reservado = 1. No exigir vacíos+llenos=capacidad a mitad de operación.
- [x] Falta de RAM por reservas de buffers mantiene un proceso NEW; liberar otro proceso permite admisión sin liberar la reserva del buffer. Pausa/paso/cierre y lectores concurrentes conservan el protocolo del módulo 4.
- [x] BUILD: si falla comportamiento, registrar causa y corregir con regresión; si pasa inicialmente, registrar verificación integrada sin inventar RED. Ejecutar clean package y comprobaciones repetidas cuando las carreras/cambios lo justifiquen. Commit `test: verifica productor consumidor local remoto y deadlines`.

## Task 9: evidencia, defensa y revisión final

**Files:** README, hoja de ruta, índice de defensa; crear `docs/defensa/modulo-5-productor-consumidor.md` y `docs/verification/2026-10-22-buffers.md` (usar fecha real al ejecutar).

- [ ] Guía breve con conceptos, clases, ejemplo, tres preguntas y explicación oral. Distinguir espera simulada de hilos reales y conservación de permisos frente a ocupación. No repetir las guías anteriores.
- [ ] Registrar trazas efectivamente comprobadas, número real de pruebas, casos de deadline y revisión. Añadir evidencia NetBeans del módulo 4 aportada por el estudiante: 1246 comprobaciones, JAR/install, BUILD SUCCESS; finalizó el 22 de octubre a las 19:54:55 de Venezuela. El fragmento no acredita Clean, cierre ni Debug.
- [ ] Ejecutar BUILD, clean package y git diff --check; revisar colecciones prohibidas, referencias retenidas, liberación de mutex Java con finally y cierre de hilos de prueba.
- [ ] Una revisión independiente final del rango completo; corregir importantes con pruebas que fallen antes, registrar decisiones/menores si existen. Mantener rama funcional y carpeta NetBeans; no inventar PR ni fusionar por falta de remoto.
- [ ] Commit `docs: explica y verifica productor consumidor para la defensa`. Siguiente módulo: interfaz completa.

Estado: tareas 1–8 implementadas y probadas; documentación preparada y revisión independiente final en curso.
