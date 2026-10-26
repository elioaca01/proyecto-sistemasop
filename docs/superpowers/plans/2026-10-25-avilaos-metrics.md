# ÁvilaOS — Module 8: Metrics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** métricas comprobables por computador/globales, esperas de semáforo por buffer y gráfico de utilización con una muestra por ciclo confirmado.

**Architecture:** los PCB conservan si alguna vez fueron admitidos; una calculadora pura suma sus contadores de instantánea. BufferService cierra episodios de espera en límites lógicos. Simulation conserva métricas e historial únicamente tras completar todos los workers; Swing observa valores inmutables y dibuja el gráfico con Java2D.

**Tech Stack:** Java 25, Maven, Swing/Java2D y estructuras propias; sin otra dependencia.

**Spec:** [diseño aprobado](../specs/2026-10-18-avilaos-design.md), §9; enunciado RF8 y gráfico. La continuación «sigue con las tareas siguientes» / «sigue» mantiene ejecución nativa en la misma carpeta del IDE. Rama `codex/performance-metrics`, base `09ff0ae`. Módulo 7 automatizado con 25234 comprobaciones; su aceptación interactiva permanece pendiente por bloqueo/timeout de la herramienta, sin impedir este trabajo independiente.

## Global Constraints

- Java posterior a 21: JDK objetivo 25, explícito por comando; no cambiar el Java global.
- No List/Map/Set/Collections/PriorityQueue/streams de colecciones ni sorters de Java; arreglos y LinkedList propia.
- Thread y Semaphore conservan el protocolo de reloj. Nunca actualizar Swing desde workers ni capturar PCB mutables en la vista.
- Todas las fórmulas usan ciclos lógicos. «Sin datos» cuando no hay denominador. Throughput usa completados normales; vencidos aparte.
- Global: sumar contadores y recalcular; utilización divide por computadores×ciclos, sin sumar/promediar porcentajes por proceso.
- Jain sobre todos los admitidos, incluidos admitidos sin servicio y terminados; excluir los que vencieron aún NEW. Servicio usuario+núcleo; cuadrados calculados en double antes de multiplicar.
- Espera empieza en el límite posterior a wait bloqueante, termina al estar disponible el permiso en un límite o al cierre por deadline. No contar el ciclo de la instrucción wait como espera ni incluir IO/red. Concesión en ciclo k está disponible en k+1. Mostrar abiertos aparte.
- Historial conserva una muestra por ciclo completo, no por Timer ni por publicación de pausa/cierre; reiniciar/cargar genera un historial vacío. Un ciclo fallido no suma métricas ni muestras.
- Pruebas headless y renderizado de paneles no equivalen a aceptación nativa/NetBeans; registrar el pendiente explícitamente.

## Review Focus

1. Cero ciclos, ningún proceso/terminado/admitido o servicio cero: sin NaN/Infinity en pantalla ni divisiones arbitrarias por uno.
2. Proceso admitido sin ejecutar y proceso vencido en NEW: equidad y cumplimiento conservan denominadores distintos, incluidos procesos terminados.
3. Permiso concedido antes del despertar, deadline ese mismo ciclo y participantes remotos: cada episodio de semáforo se cierra una sola vez; red/IO no contaminan espera.
4. Pantalla que salta ciclos, pausa/publicaciones repetidas y fallo del motor: historial completo y métricas correspondientes solo a ciclos confirmados.
5. Cambio de generación y ventana 960×640, muchas muestras o IDs de máquinas no consecutivos: etiquetas/series correctas, sin mezclar ejecuciones ni ocultar tablas sin desplazamiento.

## Task 1: servicio, admisión y fórmulas puras

**Files:** modificar `process/ProcessControlBlock.java`, `simulation/ProcessSnapshot.java`, AllTests; crear `metrics/PerformanceMetrics.java`, `metrics/MetricsCalculator.java`, `tests/PerformanceMetricsTest.java` (rutas bajo src/main/java/avilaos o src/test/java/avilaos).

**Interfaces:** PCB `boolean everAdmitted()` persiste después de terminar, true en admit. ProcessSnapshot añade último campo boolean everAdmitted. PerformanceMetrics record `(long cycles,int processors,long userCycles,long kernelCycles,long completed,long expired,long pending,long responded,double responseCycles,long admitted,double serviceSquared)`; métodos double throughput/utilization/userUtilization/kernelUtilization/averageResponse/deadlineCompliance/fairness devuelven NaN sin datos. MetricsCalculator `static PerformanceMetrics computer(ComputerSnapshot,long cycles)` y `static PerformanceMetrics aggregate(PerformanceMetrics[],long cycles)`.

- [ ] RED: admitido sin servicio y terminado después de liberar RAM conservan everAdmitted; vencido NEW false. CPU FCFS A2/B1 tras tres ciclos: completed2, busy3, respuesta promedio1, fairness9/10, throughput2/3. Todos los ceros dan NaN según denominador; utilización/throughput=0 si cycles>0.
- [ ] RED: sumar dos computadores con poblaciones/respuestas distintas recalcula promedio ponderado y Jain global, no media de índices. Caso serviceSquared mayor que long admite valor double finito y capacidad computadores×ciclos no desborda long.
- [ ] Implementar admisión persistente y fórmula desde todos los PCB en running/new/ready/blocked/terminated; evitar omisiones/duplicaciones. No alterar planificación.
- [ ] Run `mvn test` con JDK25/caché: leer OK y BUILD SUCCESS, código0. Commit `feat: calcula metricas de procesos y CPU con formulas comunes`.

## Task 2: episodios de espera de semáforos

**Files:** crear `metrics/SemaphoreWaitMetrics.java`, `tests/SemaphoreWaitMetricsTest.java`; modificar `sync/{BoundedBuffer,BufferOperation,BufferService,BufferSnapshot}.java`, AllTests.

**Interfaces:** SemaphoreWaitMetrics record `(long completedEpisodes,long completedCycles,long openEpisodes,long openCycles)`, `double averageWait()` y `static combine(SemaphoreWaitMetrics[])`. BufferSnapshot `SemaphoreWaitMetrics waits()`. BufferOperation waitStartedAt=-1; BoundedBuffer conserva contadores cerrados y método `recordWait(long cycles)`.

- [ ] RED: ejemplo local del módulo7, tras paso1 un abierto con tiempo0, tras paso2 tiempo1; al final paso12 un cerrado con6 ciclos, sin abiertos. Deadline de consumidor esperando en llenos cuenta episodio cerrado; consumidor sin productor no inventa episodios completados.
- [ ] RED: remoto L2 incluye PC inmóvil por red pero espera de semáforo cero si no se bloqueó; no contar IO. Concesión/cancelación antes de entregar wake no cierra doble. Varios participantes y espera mutex preservan total, promedio ponderado y abiertos.
- [ ] Implementar comienzo solo al fallar wait de los tres semáforos; cierre al programar concesión con boundary cycle+1 o cleanup por deadline. Capturar abiertos bajo mutex del servicio usando su próximo límite. No cambiar permisos ni orden de wakes.
- [ ] Suite GREEN y commit `feat: contabiliza episodios de espera en semaforos`.

## Task 3: publicación confirmada e historial completo

**Files:** crear `metrics/{MetricsSnapshot,UtilizationSample,UtilizationHistory}.java`, `tests/MetricsHistoryTest.java`; modificar Simulation/SimulationSnapshot, AllTests.

**Interfaces:** MetricsSnapshot `static calculate(long cycles,ComputerSnapshot[],BufferSnapshot[])`, `PerformanceMetrics global()`, `computerCount()/computerIdAt(int)/computerAt(int)`, `SemaphoreWaitMetrics waits()`. UtilizationSample inmutable `(long cycle,int[] computerIds,double[] utilizations)`, getters cycle/computerCount/computerIdAt/utilizationAt; arreglos defensivos. UtilizationHistory `record(MetricsSnapshot)`, `UtilizationSample[] copy()`. SimulationSnapshot añade sobrecarga con MetricsSnapshot y UtilizationSample[], mantiene constructores existentes; getters metrics(),sampleCount(),sampleAt(int).

- [ ] RED: tres pasos con CPU A2 generan tres muestras con utilización1,1,2/3; máquina vacía0. Pausas repetidas/cierre no agregan muestras. Lectura que omite pasos conserva las muestras intermedias. IDs2/9 se mantienen por ID, no por índice+1.
- [ ] RED: fallo de comando después de un ciclo deja métricas/historial de ese ciclo confirmado. Retener snapshot viejo y mutar arreglos de entrada no modifica muestras previas. Rechazar ciclos repetidos/fuera de secuencia en historial.
- [ ] Actualizar métricas e historial al final de advanceCycle, después de todos los workers. Publicaciones de control transportan el último cálculo sin repetirlo. Inicialización en0 con denominadores explícitos. Los constructores legacy calculan métricas y usan historial vacío.
- [ ] Suite GREEN y commit `feat: publica metricas e historial por ciclo confirmado`.

## Task 4: tablas y gráfico Swing

**Files:** crear `ui/{MetricsPanel,UtilizationChart}.java`, `ui/model/{MetricsTableModel,BufferMetricsTableModel}.java`, `tests/MetricsPanelTest.java`; modificar DashboardPanel, AllTests.

**Interfaces:** modelos AbstractTableModel propios, `setSnapshot(SimulationSnapshot)`; MetricsPanel `render(SimulationSnapshot)`, tabla names metrics.computers/metrics.buffers y chart metrics.chart. UtilizationChart `setSnapshot(SimulationSnapshot)` conserva solo copia inmutable, paintComponent Java2D.

- [ ] RED: tabla filaGlobal+computadores, porcentajes/unidades correctos, celdas solo lectura y «Sin datos» para NaN. Buffers muestran producido/consumido/ocupación, cerrados/tiempo/promedio y abiertos/tiempo separado.
- [ ] RED: panel real integrado en Dashboard muestra «Métricas»; cargar/reiniciar limpia filas/historial viejos. Layout960×640 con desplazamiento de tablas; pintar gráfico vacío/1muestra/muchasmuestras en BufferedImage sin excepción ni NaN visible.
- [ ] Añadir pestaña Métricas con JSplitPane vertical, gráfico arriba y tablas en pestañas inferiores con scroll horizontal. Eje X «Ciclo global», Y «Utilización acumulada (%)», leyenda por ID real y escala0..100; colores+trazos diferenciados. Sin librería adicional ni Timer nuevo.
- [ ] Suite GREEN y commit `feat: muestra metricas y grafico de utilizacion en Swing`.

## Task 5: aceptación integrada y evidencia gráfica

**Files:** crear `tests/MetricsIntegrationTest.java`, modificar AllTests; render de evidencia en docs/verification/images/ sin fingir ventana nativa.

- [ ] Leer ejemplos JSON reales con sesiones/botones: local12 instrucciones12, completados2, user2/kernel10, globalCPU50%, throughput2/12, Jain1, respuesta0.5, semwait6; remoto8 user1/kernel5, globalCPU37.5%, semwait sin datos.
- [ ] Cambio vivo de duración no cambia fórmulas por ciclo; reiniciar y cargar vuelve a0 con métricas vacías. Ejecución que acumula muestras sin refrescar la vista conserva todo el historial.
- [ ] Prueba sostenida de varias máquinas, captura retenida y cierre ordenado. Run clean package, diffcheck y búsqueda de colecciones/sorters prohibidos. Renderizar panel a archivo y revisar legibilidad con view_image; no afirmar aceptación nativa si la herramienta sigue sin responder.
- [ ] Commit `test: verifica metricas con escenarios y controles reales`.

## Task 6: defensa, documentación y revisión final

**Files:** crear `docs/defensa/modulo-8-metricas.md`, `docs/verification/2026-10-25-metrics.md`; actualizar README, índice y roadmap.

- [ ] Guía breve con fórmulas, denominadores, un cálculo manual, tres preguntas y pasos NetBeans; enlazar conceptos previos sin repetir todos los módulos.
- [ ] Una revisión independiente final, modelo más capaz disponible, del rango entero con Review Focus literal y ledger. Corregir Important/Critical en un único pase RED→GREEN + suite; menores diferidos explícitos. No segunda revisión.
- [ ] clean package, diffcheck, evidencia y commits. Conservar rama y carpeta local como se acordó, sin remoto/PR/fusión. Aceptación nativa pendiente se conserva como tal; no eliminar registros necesarios para retomarla.

## Comando y autorrevisión

`JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' -o -B -Dmaven.repo.local=.maven-cache test` (sustituir test por clean package para construcción limpia). Cada GREEN requiere OK:N, BUILD SUCCESS y código0.

Cobertura §9/RF8: fórmulas tarea1, esperas tarea2, confirmación/historial tarea3, presentación/gráfico tarea4, equivalencia/controles tarea5 y defensa tarea6. Interfaces entre tareas alineadas; ninguna nueva dependencia ni cambio de semántica de CPU/reloj. La espera se mide en límites compatibles con wakes existentes. La muestra usa el número de ciclos completados, no el índice de la instrucción (0-based).
