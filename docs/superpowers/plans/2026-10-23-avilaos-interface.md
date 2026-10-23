# ÁvilaOS Swing Interface Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** sustituir la ventana inicial por una interfaz Swing desde la que configurar, ejecutar, observar y defender el motor de los módulos 1–5.

**Architecture:** una sesión controla la configuración inicial y una Simulation real; un Thread propio serializa acciones y realiza cierre/reconstrucción fuera del hilo de eventos. Swing solo presenta instantáneas y envía solicitudes. Las decisiones se registran en el motor y permanecen disponibles aunque la pantalla omita publicaciones intermedias.

**Tech Stack:** Java 25, Swing/Java2D del JDK, Maven/NetBeans, Thread/Semaphore y estructuras propias, sin dependencias nuevas.

**Spec:** [diseño aprobado](../specs/2026-10-18-avilaos-design.md), secciones 5 y 8; [hoja de ruta](2026-10-19-avilaos-roadmap.md), módulo 6. Contrastado con las páginas 6–7 del enunciado original. Base actual `e000710`, 24463 comprobaciones. JSON y fórmulas/gráfico de métricas corresponden a los módulos 7 y 8.

**Execution:** método nativo de los módulos anteriores, en la misma carpeta NetBeans, rama `feat/swing-interface` al implementar y una revisión independiente final. Aprobado por el estudiante el 23 de octubre mediante «si lo apruebo»; ejecución nativa en curso.

## Global Constraints

- JDK de desarrollo propuesto: 25, sin características preview. Cumple «versiones posteriores a Java 21».
- GUI: Swing, incluida en Java; sin bibliotecas externas para componentes de interfaz.
- No usar colecciones del framework de Java: tampoco List, Map, Set, PriorityQueue, Collections, Arrays.asList o resultados de streams que creen colecciones. Usar arreglos y estructuras propias.
- No delegar el ordenamiento de colas a utilidades o bibliotecas.
- Usar Thread y java.util.concurrent.Semaphore para la concurrencia real. Los semáforos simulados serán componentes diferentes y visibles de ÁvilaOS.
- Los cambios durante ejecución se aplican en el próximo límite de ciclo. Número de computadores, RAM y latencia se configuran antes de iniciar; el enunciado no exige modificarlos en caliente.
- Swing recibe instantáneas completas mediante su hilo de eventos. La GUI no adquiere bloqueos esperando que finalice una simulación ni modifica directamente los PCB.
- Reiniciar requiere reconstruir el escenario inicial; no deja hilos antiguos activos.
- Las altas realizadas durante la ejecución no alteran silenciosamente la configuración guardada.
- La RAM usa unidades abstractas: no crear un componente gráfico por unidad de RAM o posición de buffer. Mantener el soporte de capacidades grandes del módulo 5.
- Tablas propias derivadas de AbstractTableModel, sin DefaultTableModel/Vector, TableRowSorter ni almacenamiento explícito en colecciones estándar. El orden mostrado es el publicado por el núcleo. Componentes Swing estándar y su almacenamiento interno pertenecen al toolkit, no a la gestión de procesos/PCB.
- Ningún botón afirma que una orden está aplicada mientras solo esté encolada. Mostrar pendientes, aceptación y rechazo con su causa.

## Decisiones de presentación e integración

Se continúa el diseño Swing aprobado. Una ventana con pestañas conserva el contexto mejor que varias ventanas de monitorización; colocar todo en una sola superficie haría ilegibles las cuatro colas. Se eligen pestañas **Computadores**, **Buffers y semáforos**, **Configuración inicial** y **Registro**; formularios modales cortos para altas.

- Cabecera con ciclo, estado observado, duración y latencia; barra con Iniciar/Continuar, Pausar, Paso, Reiniciar, Agregar proceso y Agregar buffer.
- Computadores: selector por ID, CPU actual/última instrucción/modo, RAM usada/libre, política y quantum editables; colas Nuevos/Listos/Bloqueados/Terminados y detalle del PCB seleccionado. La selección sigue el ID, no el índice de fila.
- Buffers: selector por ID, anfitrión/capacidad/ocupación/contadores, tokens con origen, tres valores y colas de semáforos, dueño y fases/reservas activas.
- Configuración inicial: cantidad de máquinas, RAM/política/quantum de cada una, duración/latencia y carga inicial. Puede ajustarse antes del primer Iniciar/Paso. Quitar una carga inicial reconstruye el escenario validado; no introduce cancelación o eliminación de buffers durante ejecución.
- Registro: eventos del motor con ciclo, computador, proceso y texto; conserva eventos entre refrescos. Los resultados de órdenes pendientes/rechazadas también son visibles.
- Aspecto: fondo claro, azul oscuro y verde azulado de la ventana actual, tipografía del sistema, texto en español, estados escritos además del color, espacios y scrolls; tamaño inicial 1180×780 y mínimo 960×640. No requiere imágenes ni fuentes externas.
- Valores iniciales: dos computadores IDs 1/2, RAM 64, FCFS, quantum 2, ciclo 500 ms, red 2, carga vacía. Se crea una Simulation pausada, sin ejecutar ciclos escondidos. Las cargas iniciales se encolan antes del ciclo 0 y se distinguen de los PCB todavía inexistentes.
- Política/quantum y duración cambiados durante la ejecución afectan solo el estado vivo. Reiniciar vuelve a los atributos/cargas iniciales; se informa esa semántica junto al botón. JSON se añadirá sobre el mismo modelo en el módulo 7.

## Review Focus

1. Reiniciar/cerrar mientras se configura, ejecuta o aplica una solicitud: no bloquear el EDT, filtrar callbacks/publicaciones viejas ni dejar workers/coordinadores/sesiones vivos; tareas 3 y 8–9.
2. Datos iniciales frente a cambios vivos: reiniciar no incorpora silenciosamente altas/políticas/velocidad de la ejecución y una configuración inválida conserva la anterior; tareas 1, 3 y 5.
3. Reordenamiento de colas mientras se inspecciona un PCB: conservar identidad y contexto, no editar el proceso equivocado ni dejar un detalle de otra fila; tareas 4 y 6.
4. Reloj más rápido que el refresco: el registro no pierde selección, bloqueo, red o terminación; las órdenes encoladas no se presentan como confirmadas; tareas 2, 3 y 7–9.
5. Entradas numéricas/enums, cambio de tipo en formularios, capacidades grandes y ventana pequeña: errores comprensibles sin mutación, campos ocultos no contaminan el tipo y vistas navegables sin crear millones de componentes; tareas 1 y 4–9.

## Mapa de archivos y contratos

- `config/SimulationConfig.java`: escenario inicial inmutable, validación completa en memoria y operaciones de copia. Todavía no lee/escribe archivos.
- `simulation/EventLog.java`: lista propia de SimulationEvent protegida por semáforo, compartida por el clúster; el motor emite decisiones sin conocer Swing.
- `ui/session/{UiSession,SessionView,SessionPhase,SessionNotice}.java`: dueño de la Simulation, solicitudes FIFO y ciclo de vida asíncrono; sin widgets ni acceso externo al modelo mutable.
- `ui/model/{ProcessTableModel,BufferItemTableModel,SemaphoreTableModel,BufferOperationTableModel,EventTableModel,ComputerConfigTableModel}.java`: proyecciones de valores, sin lógica de simulación.
- `ui/{UiText,InputParser,UiTheme}.java`: etiquetas/formatos, conversión validada y presentación común.
- `ui/{ConfigurationPanel,ProcessFormPanel,BufferFormPanel,ComputerPanel,ProcessDetailsPanel,BufferPanel,EventPanel}.java`: vistas con una responsabilidad cada una.
- `ui/{MainWindow,UiController,UiView}.java`: ensamblado, acciones de botones, Timer de refresco y actualización exclusivamente en EDT. App conserva entrada NetBeans.
- Pruebas bajo `src/test/java/avilaos/tests/`; todas las deterministas se registran en AllTests. Los JFrame/dialogs reales se verifican en la aceptación visual aparte para que mvn test siga funcionando en modo headless.

`SimulationConfig(long cycleMillis,int networkLatency,ComputerConfig[] computers,BufferDefinition[] buffers,ProcessDefinition[] processes)` copia arreglos; getters escalares y `computerCount/At`, `bufferCount/At`, `processCount/At`. Produce una nueva configuración con `withComputers(ComputerConfig[])`, `withTiming(long,int)`, `withAddedBuffer(BufferDefinition)`, `withAddedProcess(ProcessDefinition)`, `withoutBuffer(int id)`, `withoutProcess(int index)`; toda copia vuelve a validar. No expone arreglos.

`UiSession(SimulationConfig initial)` inicia su Thread de sesión y prepara una Simulation pausada. API de solicitudes no bloqueantes: `long replaceScenario(SimulationConfig)`, `addProcess(ProcessDefinition)`, `addBuffer(BufferDefinition)`, `changeScheduling(int,SchedulingPolicyType,int)`, `changeCycleDuration(long)`, `resume()`, `pause()`, `step()`, `restart()`; el valor es ID de acción de sesión. Ofrece `SessionView view()`, `void requestClose()` y `boolean awaitClosed(long timeoutMillis)`; awaitClosed solo se usa fuera del EDT, principalmente en pruebas. Las acciones posteriores al cierre se rechazan.

`SessionPhase`: `PREPARING, ACTIVE, REPLACING, CLOSING, CLOSED, FAILED`. `SessionNotice(long actionId,long engineRequestId,boolean error,String message)` distingue acciones de sesión y solicitudes del motor (-1 si no existe). `SessionView` inmutable contiene generation, phase, started, pendingActions, initialConfig, simulation nullable y notices copiados con count/At. Una vista conserva su generación aunque se consulte mientras otra se publica. No expone Simulation, Computer ni PCB. Una validación rechazada produce aviso sin destruir la sesión; SimulationState.FAILED se muestra como fallo del motor y permite reiniciar/cerrar, deshabilitando Iniciar/Paso. Un fallo de infraestructura de sesión usa SessionPhase.FAILED y mantiene la ruta de cierre disponible.

El Thread de UiSession es el único que crea/cierra/reemplaza la Simulation y envía sus controles/comandos. Consume acciones en LinkedQueue propia protegida por Semaphore y espera mediante otro semáforo. Las vistas capturan brevemente el contexto publicado y consultan el SnapshotStore, sin sostener el mutex de sesión al esperar ciclos o cerrar. Cerrar sella la cola: la acción en curso termina con limpieza, cancela pendientes con aviso y cierra cualquier Simulation creada antes de confirmar CLOSED. Una sustitución valida primero, cierra el motor anterior y solo publica generación nueva después de crear el nuevo pausado; un fallo de cierre se muestra, no se ignora creando otro motor.

## Comandos de verificación

Usar `BUILD` como abreviatura de este comando exacto en los pasos siguientes:

```sh
/usr/bin/env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -o -B -Dmaven.repo.local=.maven-cache test
```

Esperado: `OK: N comprobaciones`, N >= 24463, y BUILD SUCCESS. Empaquetado: sustituir test por `clean package`. Arranque visual: sustituirlo por `compile exec:exec@run-app`. No cambiar el Java global ni agregar dependencias. Si falta una API, el RED puede ser de compilación; errores ajenos a la prueba no cuentan como comportamiento verificado. Usar barreras/esperas acotadas y cierre en finally, no sleeps como sincronización.

## Task 1: escenario inicial validado e inmutable

**Files:** crear `config/SimulationConfig.java`, `tests/SimulationConfigTest.java`; registrar en AllTests.

**Interfaces:** constructor y métodos de copia de SimulationConfig descritos arriba; consume ComputerConfig, ProcessDefinition y BufferDefinition existentes.

- [x] Escribir pruebas: dos máquinas RAM 10, buffer anfitrión 1/capacidad 3 y proceso memoria 8 son válidos (podrá permanecer NEW); dos buffers capacidades 6+5 del mismo anfitrión se rechazan. Rechazar menos de 2, null, máquinas/IDs de buffer duplicados, referencias inexistentes, proceso memoria > RAM total, ms <= 0 y latencia < 0. No exigir que toda la carga de procesos quepa simultáneamente.
- [x] Comprobar copias: alterar arreglos de entrada no cambia el escenario; withAddedProcess no cambia el original; withoutBuffer rechaza quitar uno referenciado y withoutProcess usa índice validado. withComputers no borra referencias inválidas silenciosamente. BUILD debe fallar por clase ausente.
- [x] Implementar validación con arreglos/bucles propios, sumando reservas de buffers en long. BUILD debe pasar. Commit `feat: modela el escenario inicial para la interfaz`.

## Task 2: registro completo de decisiones en el motor

**Files:** crear `simulation/EventLog.java`, `tests/EventLogTest.java`, `tests/DecisionLogTest.java`; modificar Kernel, Computer, Simulation, BufferService y ajustar únicamente la antigua expectativa de evento único de BufferSimulationTest.

**Interfaces:** EventLog `void append(SimulationEvent event)`, `int size()`, `SimulationEvent at(int)`, `SimulationEvent[] copy()`; copia defensiva de valores. Kernel añade constructor con EventLog después de BufferService, Computer equivalente; anteriores delegan sin registro. BufferService añade constructor `(BufferRegistry,int,EventLog)` y conserva sus consultas locales de mantenimiento. Simulation es dueña del registro compartido y captura sus copias en SimulationSnapshot.

- [x] Escribir pruebas de copia estable, append nulo rechazado y varios escritores con barreras. En Simulation real observar eventos de alta/rechazo, admisión, selección, apropiación/cambio de política, bloqueo E/S/semáforo/red, desbloqueo, inserción/extracción y terminación con IDs/ciclos correctos; BUILD debe fallar por API/eventos ausentes.
- [x] Implementar emisión donde ocurre la decisión. Desbloqueo externo usa Kernel.nextCycle antes del tick; el cierre de deadline y la limpieza conservan el ciclo real. No reconstruir eventos comparando snapshots en Swing ni añadir instrucciones por log. Un rechazo sin computador/proceso usa 0/-1 y el texto de causa.
- [x] Probar que avanzar varios ciclos sin leer snapshots conserva todas las selecciones/bloqueos intermedios. Ajustar BufferSimulationTest para localizar su evento de limpieza, no exigir que sea el único evento global; mantener sus pruebas de copia y contenido. BUILD pasa y trazas/PC/RAM previos no cambian. Commit `feat: registra decisiones del motor para la interfaz`.

## Task 3: sesión y ciclo de vida fuera del EDT

**Files:** crear `ui/session/{UiSession,SessionView,SessionPhase,SessionNotice}.java`, `tests/UiSessionTest.java`, `tests/UiSessionLifecycleTest.java`.

**Interfaces:** API de UiSession y valores descritos en el mapa. Configuración/cargas antes del primer resume/step actualizan el escenario inicial y encolan órdenes para ciclo 0. Después, altas/política/duración solo envían comandos al estado vivo. replaceScenario se rechaza después de empezar; restart siempre reconstruye initialConfig. No realizar ticks ocultos para confirmar cargas.

- [x] Pruebas reales: preparar deja nextCycle=0/PAUSED, las altas iniciales aparecen en config antes de su aplicación; step crea sus PCB en 0. Alta durante ejecución aparece en resultados/snapshot y no en initialConfig. Cambiar política/duración y reiniciar conserva valores iniciales, generación mayor, ciclo 0 y nuevas IDs desde 1 al ejecutar.
- [x] Probar sustitución inválida sin perder configuración/motor, rechazo de edición estructural después de empezar, orden FIFO de acciones, cierre durante preparación/reinicio y cinco reinicios sin acumular hilos. Los avisos distinguen encolado y resultado del motor. BUILD debe fallar por API ausente.
- [x] Implementar Thread/cola/semáforos y contextos publicados; no invocar Swing ni cerrar bajo el mutex de cola. Sellar/cancelar al cerrar, confirmar salida del Thread y del motor con espera acotada; una vista retenida sigue estable. BUILD pasa. Commit `feat: controla sesiones y reinicios sin bloquear la interfaz`.

## Task 4: modelos de tablas y presentación por identidad

**Files:** crear los seis modelos en `ui/model/`, `ui/UiText.java`, `tests/UiTableModelTest.java`.

**Interfaces:** ProcessTableModel `setRows(ProcessSnapshot[])`, `processAt(int)`, `rowOf(long id)`; copia array, solo lectura. Modelos de buffer `setBuffer(BufferSnapshot)` y de registro `setSnapshot(SimulationSnapshot)` admiten null como vacío. ComputerConfigTableModel usa ComputerConfig[] copiados y edición solo de atributos iniciales con validación; `configs()` retorna copia. UiText ofrece etiquetas de enums y `deadlineRemaining(ProcessSnapshot,long boundaryCycle)`.

- [x] Pruebas en EDT mediante invokeAndWait: filas READY [A,B] pasan a [B,A], rowOf(A) cambia y processAt sigue siendo A; no editar PCB. Tokens conservan ID/productor/computador; cada semáforo muestra valor y esperas FIFO. Eventos repetidos del mismo snapshot no se duplican. Capacidad Integer.MAX_VALUE vacía produce cero filas de tokens.
- [x] Comprobar deadline al límite después del ciclo: vivo usa nextCycle; terminado usa terminationCycle+1 para mostrar restante al finalizar. CPU/cola vacía y sentinel -1 se muestran de forma comprensible. BUILD debe fallar por modelos ausentes.
- [x] Implementar AbstractTableModel, sin sorters ni DefaultTableModel. Conservar tipos numéricos para columnas y separar etiquetas de lógica. BUILD pasa. Commit `feat: presenta PCB buffers y eventos en tablas propias`.

## Task 5: configuración y formularios validados

**Files:** crear `ui/{InputParser,ConfigurationPanel,ProcessFormPanel,BufferFormPanel}.java`, `tests/UiFormTest.java`.

**Interfaces:** InputParser `integer(String text,String field,int minimum)`, `longInteger(String text,String field,long minimum)` rechazan fracciones/overflow y retornan valores tipados. ConfigurationPanel `load(SimulationConfig)`, `readConfiguration()` y `setEditable(boolean)`; ProcessFormPanel `setChoices(ComputerConfig[],BufferDefinition[])`, `readDefinition()`; BufferFormPanel `setComputers(ComputerConfig[])`, `readDefinition()`.

- [x] Pruebas EDT de formularios sin JFrame: CPU/IO/productor/consumidor generan definiciones correctas; cambiar de productor a CPU ignora y pone en cero auxiliares ocultos. Vacío, 1.5, negativos donde no proceden, prioridad negativa, overflow int/long, buffer no elegido y presupuesto insuficiente muestran campo/causa y no envían solicitudes. BUILD debe fallar por API ausente.
- [x] Implementar controles Swing, campos específicos por tipo, IDs/computadores por selectores, mensajes junto al formulario. Configuración permite cantidad >= 2, RAM/política/quantum por fila y duración/latencia; muestra cargas iniciales y permite quitar antes de empezar validando referencias mediante SimulationConfig. Las altas en vivo se etiquetan como de esta ejecución.
- [x] BUILD pasa. Probar valores con espacios y deadline long; aplicar una configuración inválida conserva el escenario vigente. Commit `feat: agrega formularios de configuracion procesos y buffers`.

## Task 6: panel por computador y detalle del PCB

**Files:** crear `ui/{ComputerPanel,ProcessDetailsPanel}.java`, `tests/ComputerPanelTest.java`.

**Interfaces:** ComputerPanel `render(ComputerSnapshot,long boundaryCycle)`, `setSchedulingHandler(ComputerPanel.SchedulingHandler)` con interfaz funcional pública anidada `apply(int computerId,SchedulingPolicyType,int quantum)`; ProcessDetailsPanel `render(ProcessSnapshot,long boundaryCycle)` admite null. Acciones solo comunican valores al controlador.

- [x] Escribir pruebas EDT: CPU bound, E/S, semáforo, NEW sin RAM y TERMINATED aparecen en sus colas con motivos correctos; selección por ID se conserva al reordenar/mover de cola y se limpia al sustituir escenario. Detalle accesible también desde CPU y muestra todos los campos de ProcessDefinition/ProcessSnapshot y operación de buffer. BUILD debe fallar por paneles ausentes.
- [x] Implementar resumen de CPU actual separado de «último ciclo»: proceso/instrucción/modo, PC/MAR, prioridad/deadline; RAM con barra/valores; cuatro colas contadas y detalle. Modo IDLE se distingue de USER/KERNEL; una CPU liberada después de terminar no se presenta como si siguiera ejecutando el PCB terminado.
- [x] Selector política/quantum envía un solo cambio con validación y conserva valores en edición mientras llegan snapshots. El valor aplicado se actualiza al confirmarse el cambio. El detalle muestra también deadline inicial/absoluto y, para terminados, el restante al finalizar con etiqueta explícita. BUILD pasa. Commit `feat: visualiza CPU RAM colas y contexto por computador`.

## Task 7: buffers, semáforos y registro

**Files:** crear `ui/{BufferPanel,EventPanel}.java`, `tests/BufferPanelTest.java`, `tests/EventPanelTest.java`.

**Interfaces:** BufferPanel `render(SimulationSnapshot)` conserva buffer seleccionado por ID; EventPanel `render(SimulationSnapshot)` muestra el registro y resultados pendientes/rechazados con su ciclo/causa. Consume únicamente snapshots/modelos de tareas 2 y 4.

- [x] Pruebas EDT: capacidad 1 muestra inserción, token de origen, dueño y concesión pendiente; sus tres colas/valores y fases cambian al avanzar. Buffer vacío inicial y selección eliminada por reinicio no dejan datos antiguos. Capacidad grande no crea componentes por posición. BUILD debe fallar por paneles ausentes.
- [x] Implementar resumen, tabla FIFO de tokens, tres semáforos y operaciones activas. Registro conserva eventos en orden de emisión e ID/ciclo, sin rehacerlos desde la GUI; permite scroll sin saltar al final si el usuario inspecciona filas anteriores.
- [x] Verificar que leer solo tras varios ciclos conserva todos los eventos y que render repetido no duplica filas ni cambia la selección del buffer. BUILD pasa. Commit `feat: muestra buffers semaforos y decisiones del sistema`.

## Task 8: ventana completa, controles y actualización segura

**Files:** crear `ui/{UiController,UiView,UiTheme}.java`, modificar MainWindow/App si procede, crear `tests/UiControllerTest.java`.

**Interfaces:** UiView define `void render(SessionView)`, `void showNotice(SessionNotice)` y `void closed()`. MainWindow implementa UiView; una vista de prueba con paneles reales evita depender de JFrame en modo headless. UiController `(UiSession,UiView)` ofrece `start()` para activar su Timer, `refresh()` solo EDT y `requestClose()` no bloqueante, más acciones tipadas que delegan los métodos públicos de UiSession (sin exponer el motor). Timer Swing de 100 ms toma la última SessionView, descarta generaciones/versiones anteriores y renderiza solo datos nuevos o cambios de sesión. MainWindow crea el controlador, usa DO_NOTHING_ON_CLOSE y se dispone cuando el cierre está confirmado; no invoca close/awaitStopped/join en EDT.

- [x] Pruebas de controlador con paneles reales sin JFrame: estado inicial, un click Paso => un ciclo, Iniciar/Pausar, controles deshabilitados según fase, pendientes y rechazo visibles; refresco viejo tras reinicio no cambia ciclo/tablas. Enviar acciones desde EDT retorna mientras un trabajo de ciclo de vida sigue pendiente; comprobar latido EDT mediante barrera, no duración arbitraria. BUILD debe fallar por controlador ausente.
- [x] Implementar cuatro pestañas y barra/cabecera descritas, formularios en dialogs, estado de error y cierre. No sobrescribir campos con foco/edición durante refresco. La pantalla diferencia configuración inicial, órdenes pendientes y estado confirmado. Al cerrar con operación de sesión en curso se espera de forma asíncrona su limpieza, sin instalar una generación después de CLOSED.
- [ ] BUILD pasa; `clean package` pasa. Arranque `compile exec:exec@run-app` abre ventana y puede cerrarse sin retener proceso/hilos. Commit `feat: conecta la interfaz Swing con el simulador completo`.

## Task 9: aceptación visual y regresión integrada

**Files:** crear `tests/UiIntegrationTest.java`; correcciones solo tras reproducción; evidencia visual bajo `docs/verification/images/` si las herramientas permiten guardarla.

- [x] Trazas por sesión/controlador: consumidor local primero, productor después, capacidad 1, intervalo/cuota 1; 12 pasos reproducen los ciclos 0..11 del módulo 5 y los valores finales 0/1/0/1. Remoto L=2 muestra NETWORK sin avance PC durante dos ciclos. RR quantum 1, EDF y prioridades actualizan el orden visible y conservan contexto.
- [x] Probar escenarios sin carga, buffer pendiente en ciclo 0, formulario inválido, alta con simulación pausada, alta/cambio en ejecución, deadline en semáforo, capacidad grande, cinco reinicios y cierre durante ejecución/reemplazo. BUILD debe pasar si la integración ya es correcta; registrar RED real si aparece un defecto antes de corregirlo.
- [ ] Abrir la aplicación real e inspeccionar con herramientas UI disponibles: ventana grande y mínimo 960×640, las cuatro pestañas, edición/formularios, PCB, scrolls, seleccionar una fila durante cambios de orden, confirmar que Pausar estabiliza el ciclo y Paso añade exactamente uno. Hacer el ejemplo local desde los controles, revisar el remoto y cerrar. No sustituir esta aceptación por una captura estática o solo tests headless.
- [x] `clean package`, git diff --check e inspección de colecciones/sorters/EDT. Repetir ciclos de vida en JVM separadas con tiempo límite si aparecen carreras. Commit `test: verifica la interfaz y el ciclo de vida integrado`.

## Task 10: defensa, evidencia y revisión independiente

**Files:** crear `docs/defensa/modulo-6-interfaz.md`, `docs/verification/2026-10-23-interface.md` (fecha real al ejecutar); actualizar README, índice de guías y hoja de ruta.

- [x] Guía breve: qué se construyó, EDT, comando/snapshot, estado inicial frente a vivo, ejemplo y tres preguntas. Añadir una lista corta de pruebas manuales para el estudiante en NetBeans, indicando resultado esperado de cada una.
- [ ] Registrar resultados reales y total acumulado, aceptación visual efectivamente realizada, límites y evidencia del IDE que el estudiante aporte. La comprobación del módulo 5 desde NetBeans sigue pendiente mientras no aporte el resultado; no inferirla de BUILD en terminal.
- [x] Una revisión independiente final del rango completo y las cinco condiciones de Review Focus; corregir importantes en un pase mediante RED→GREEN y suite completa; documentar decisiones/menores si existen. No revisores por tarea ni implementadores paralelos.
- [ ] BUILD, clean package y git diff --check satisfactorios; commit `docs: explica y verifica la interfaz para la defensa`. Conservar rama/carpeta NetBeans, sin PR/fusión inventados. Siguiente módulo: escenarios JSON.

Estado: código implementado y 24770 comprobaciones satisfactorias tras corregir los tres hallazgos importantes de la revisión independiente. Pendiente la inspección visual real (Mac bloqueado), incluido arranque/cierre con el código corregido; no se da por completada la aceptación del módulo.
