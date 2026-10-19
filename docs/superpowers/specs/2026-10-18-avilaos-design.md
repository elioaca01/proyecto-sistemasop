# ÁvilaOS — diseño del Proyecto 1

Estado: diseño aprobado por el estudiante el 19 de octubre de 2026, mediante la instrucción «hazlo» para preparar las tareas de implementación. La implementación del primer módulo comenzó el 19 de octubre; su comprobación visual en NetBeans sigue pendiente.

## 1. Objetivo y alcance

Construir un simulador de sistema operativo distribuido que cumpla todos los requisitos funcionales del Proyecto 1. Lo desarrollará un único estudiante con asistencia de Codex, priorizando que pueda explicar y defender cada módulo.

Fuente: [enunciado original](/Users/eliocolmenares/Downloads/Enunciado%20Proyecto%201.%20Sistemas%20Operativos%202627-1.pdf), leído completo, incluidos sus anexos.

- Meta acordada: proyecto, informe y demostración listos el lunes 26 de octubre de 2026; martes 27 como margen para correcciones.
- Entrega académica: viernes 30 de octubre de 2026 antes de las 07:00, hora de Venezuela.
- Obligatorio: mínimo dos computadores, cuatro políticas, GUI, concurrencia, memoria limitada, productor-consumidor local/remoto, persistencia, métricas y gráfico.
- Se implementarán FCFS, EDF, Round Robin y prioridades apropiativas. Aunque el texto dice «mínimo 3», enumera cuatro.
- Asignación manual de procesos y sin migración. Se omite el balanceo automático opcional.
- Disco, memoria virtual y procesos suspendidos quedan fuera del Proyecto 1.

## 2. Entorno y restricciones

- JDK de desarrollo propuesto: 25, sin características preview. Cumple «versiones posteriores a Java 21».
- IDE obligatorio: Apache NetBeans con soporte para el JDK elegido. La validación final incluye abrir, compilar y ejecutar allí.
- GUI: Swing, incluida en Java; sin bibliotecas externas para componentes de interfaz.
- Proyecto Maven importable en NetBeans, con configuración explícita de la versión de Java. Las herramientas de construcción no implementan la lógica del simulador.
- Biblioteca externa prevista: Gson exclusivamente para leer y escribir JSON mediante su API de streaming. Gráficos dibujados con Swing/Java2D.
- No usar colecciones del framework de Java: tampoco `List`, `Map`, `Set`, `PriorityQueue`, `Collections`, `Arrays.asList` o resultados de streams que creen colecciones. Usar arreglos y estructuras propias.
- No delegar el ordenamiento de colas a utilidades o bibliotecas. La lista ordenada será implementación propia y conservará desempates estables.
- Usar `Thread` y `java.util.concurrent.Semaphore` para la concurrencia real. Los semáforos simulados serán componentes diferentes y visibles de ÁvilaOS.
- Pruebas del núcleo con ejecutables Java y comprobaciones propias, sin añadir una biblioteca externa de pruebas al proyecto entregable.
- Repositorio GitHub, ramas `main`, `develop` y ramas por funcionalidad; Issues y PR con descripción. Historial real del único integrante, sin atribuciones ficticias.

Referencias técnicas consultadas:

- https://www.oracle.com/news/announcement/oracle-releases-java-25-2025-09-16/
- https://cwiki.apache.org/confluence/spaces/NETBEANS/pages/255068881/Minimum+JDK+build+and+run+policy

## 3. Arquitectura y alternativas

Se elige un núcleo independiente de la GUI y un reloj que coordina hilos de computador. Permite probar el modelo por ciclos y observarlo desde Swing sin que las ventanas decidan la planificación.

Alternativas evaluadas:

1. Hilos coordinados por reloj global: elegida; satisface el avance exacto de cada máquina y permite explicar la sincronización.
2. Hilos con temporizadores independientes: descartada; dormir el mismo número de milisegundos no garantiza que avancen al mismo ciclo.
3. Toda la simulación en el hilo de Swing: descartada; congelaría la interfaz y no ofrece la separación de responsabilidades requerida.

| Paquete | Responsabilidad | Clases principales previstas |
| --- | --- | --- |
| `avilaos.structures` | Almacenamiento y ordenamiento propios | `LinkedList`, `LinkedQueue`, `OrderedProcessQueue`, `RingBuffer` |
| `avilaos.process` | Modelo monohilo y contexto de ejecución | `SimulatedProcess`, `ProcessControlBlock`, `ProcessDefinition`, `Instruction` |
| `avilaos.hardware` | Ejecutar una instrucción y contabilizar RAM/E/S | `Cpu`, `Ram`, `IoController` |
| `avilaos.kernel` | Admisión, despacho, estados y finalización | `Kernel`, `Scheduler`, `MemoryAdmission` |
| `avilaos.scheduling` | Selección y apropiación intercambiables | `SchedulingPolicy`, cuatro implementaciones |
| `avilaos.sync` | Buffers y semáforos simulados | `BoundedBuffer`, `SimulatedSemaphore`, `BufferOperation` |
| `avilaos.simulation` | Reloj, clúster, hilos y órdenes de usuario | `Simulation`, `Computer`, `GlobalClock`, `ComputerWorker`, `Tickable`, `SimulationCommand` |
| `avilaos.metrics` | Registro de contadores, muestras y fórmulas | `MetricsCollector`, `ComputerMetrics`, `BufferMetrics` |
| `avilaos.config` | Configuración y validación JSON | `SimulationConfig`, `ConfigurationStore`, `ConfigurationValidator` |
| `avilaos.ui` | Formularios, paneles, tablas y gráfico | `MainWindow`, `ConfigurationPanel`, `ProcessForm`, `ComputerPanel`, `BufferPanel`, `MetricsPanel` |

`Computer` ensambla CPU, RAM, controlador de E/S y núcleo. Todos los computadores se crean con esta misma clase. Ni CPU ni RAM eligen procesos. Los componentes inferiores no dependen de ventanas ni del planificador.

Interfaces mínimas:

- `Tickable.onTick(long cycle)`: reacción a un ciclo lógico; usada por CPU y controlador de E/S.
- `SchedulingPolicy.select(OrderedProcessQueue ready)`: seleccionar y retirar un proceso listo, o devolver `null`.
- `SchedulingPolicy.shouldPreempt(ProcessControlBlock running, OrderedProcessQueue ready, int sliceUsed, int quantum)`: decidir apropiación sin modificar estados.
- `SchedulingPolicy.reorder(OrderedProcessQueue ready)`: aplicar el orden estable de la política.
- `SimulationCommand.apply(Simulation simulation)`: aplicar una acción de usuario en un límite de ciclo seguro. Se justifica para que Swing no modifique las colas mientras ejecuta la CPU.

## 4. Procesos, estados e instrucciones

Enums obligatorios y adicionales:

- `ProcessState`: `NEW`, `READY`, `RUNNING`, `BLOCKED`, `TERMINATED`.
- `BlockReason`: `NONE`, `IO`, `NETWORK`, `SEMAPHORE_EMPTY`, `SEMAPHORE_FULL`, `SEMAPHORE_MUTEX`.
- `ProcessType`: `CPU_BOUND`, `IO_BOUND`, `PRODUCER`, `CONSUMER`.
- `SchedulingPolicyType`: `FCFS`, `EDF`, `ROUND_ROBIN`, `PREEMPTIVE_PRIORITY`.
- `CpuMode`: `USER`, `KERNEL`, `IDLE`. `IDLE` identifica ausencia de trabajo y no se presenta como ejecución del SO.
- `TerminationReason`: `COMPLETED`, `DEADLINE_EXPIRED`.
- `InstructionKind`: cómputo, solicitud de E/S, `SEM_WAIT`, `SEM_SIGNAL`, inserción y extracción.

Cada proceso representa un único hilo simulado; no se crea un hilo real por cada PCB. El hilo real pertenece al computador. PCB y contexto del único hilo mantienen relación 1:1.

PCB: ID global, nombre, computador fijo, estado, tipo, prioridad, memoria requerida/asignada, deadline inicial y restante, instrucciones totales/restantes, PC, MAR, motivo de bloqueo, buffer asociado, fase pendiente del algoritmo, ciclo de creación, primera ejecución, terminación y contadores de espera/servicio.

Los IDs son secuenciales para toda la simulación, asignados por el coordinador al aplicar comandos; no se reutilizan durante una ejecución.

Creación de procesos:

- Campos comunes: nombre, computador, tipo, instrucciones, memoria, prioridad y deadline relativo en ciclos.
- CPU bound: programa lineal de instrucciones de cómputo.
- I/O bound: además, ráfaga de CPU entre solicitudes y duración de cada E/S en ciclos. E/S es simulada, sin dispositivos reales.
- Productor/consumidor: buffer, intervalo de cómputo entre operaciones y cantidad de elementos objetivo. Cada operación completa añade cinco instrucciones de núcleo. Validar que el presupuesto de instrucciones alcance el trabajo solicitado. Si hay instrucciones sobrantes, son cómputo posterior a la cuota, sin producir/consumir elementos adicionales.
- Productor: cómputo, `wait(vacíos)`, `wait(mutex)`, insertar, `signal(mutex)`, `signal(llenos)`.
- Consumidor: `wait(llenos)`, `wait(mutex)`, extraer, `signal(mutex)`, `signal(vacíos)`, cómputo.
- El intervalo de los productores/consumidores se mide en ciclos de cómputo del proceso; una espera o apropiación no produce trabajo automático.

Cada instrucción ejecutada cuesta un ciclo. `semWait`, `semSignal`, inserción y extracción se ejecutan en modo núcleo. PC y MAR avanzan una unidad por instrucción ejecutada; no avanzan cuando el proceso está bloqueado o no dispone de CPU. Esta lectura evita inventar ejecución durante esperas, a pesar de la simplificación del enunciado «por cada ciclo del reloj».

Un `semWait` que bloquea cuenta como instrucción ejecutada; al despertar, el proceso continúa en la siguiente fase con el permiso concedido, sin repetir el decremento.

## 5. Ciclo global y concurrencia real

Un hilo `GlobalClock` coordina un `ComputerWorker` por computador. Cada worker usa un semáforo Java de inicio y otro de fin. Swing conserva su hilo de eventos.

Para la primera versión se elige un orden de avance fijo por ID de computador. El reloj despierta un worker, espera su fin y pasa al siguiente; el ciclo global concluye cuando todos avanzaron exactamente una vez. Hay hilos reales coordinados, sin exigir ejecución simultánea en los núcleos físicos. El orden es explícito y permite repetir escenarios para la defensa.

Orden de un ciclo `t`:

1. Aplicar comandos pendientes: altas de procesos/buffers, cambios de política, quantum y duración del ciclo.
2. Entregar despertares de E/S, red y semáforos cuyo ciclo de disponibilidad sea `t`.
3. Admitir procesos en RAM, recorrer la cola de nuevos en orden de llegada y admitir los que quepan. Un proceso grande no impide admitir otro que sí quepa.
4. Cada computador, una sola vez: evaluar apropiación, despachar si procede y ejecutar como máximo una instrucción. No ejecutar otra instrucción en el mismo ciclo si la primera bloquea o termina.
5. Confirmar terminaciones y comprobar deadlines de todos los procesos no terminados, incluidos nuevos y bloqueados. Liberar recursos y volver a revisar admisión cuando se libere RAM; los nuevos admitidos ejecutan a partir del próximo ciclo.
6. Actualizar métricas, publicar una instantánea inmutable de la simulación y avanzar el contador global.

Un desbloqueo causado por una instrucción del ciclo `t` hace al proceso elegible en `t+1`, aunque su computador todavía no haya avanzado. Esto evita ventajas por el orden de los computadores.

Un proceso creado en el límite inicial del ciclo `t`, con deadline relativo `D`, dispone de los ciclos `t` a `t+D-1`. Si termina su última instrucción en ese último ciclo, cumple el plazo; de lo contrario termina por deadline al cerrarlo. El tiempo simulado avanza para todos, no solo para el proceso en CPU.

La velocidad en milisegundos solo controla el ritmo visual del reloj. No cambia las métricas expresadas en ciclos ni simula E/S con `Thread.sleep` por proceso.

Las estructuras compartidas reales se protegen mediante `Semaphore` Java: cola de comandos, registro compartido y publicación/lectura de instantáneas. El relevo inicio/fin impide que dos workers muten el núcleo simultáneamente. Las excepciones de un worker se comunican al reloj y detienen la simulación con un error visible, sin dejarlo esperando indefinidamente.

Swing recibe instantáneas completas mediante su hilo de eventos. La GUI no adquiere bloqueos esperando que finalice una simulación ni modifica directamente los PCB.

## 6. Planificación, memoria y terminación

- Hay una sola cola de listos por computador.
- FCFS: no apropiativa, orden de entrada a listos.
- EDF: apropiativa, menor deadline absoluto primero.
- Round Robin: FIFO; quantum positivo por computador, consumido por instrucciones tanto de usuario como de núcleo. Al expirar, el proceso vuelve al final.
- Prioridades: apropiativa, número menor significa prioridad mayor.
- Empates: conservar orden de entrada a listos. La llegada de otro proceso con la misma prioridad o deadline no causa apropiación.
- Un cambio de política entra en vigor al comienzo del próximo ciclo; reinicia el quantum consumido y reordena listos. La nueva política evalúa al proceso actual según sus propias reglas. No hay costo adicional de cambio de contexto en esta versión.

RAM usa unidades abstractas enteras, identificadas igual en formularios, configuración y gráficos. Cada posición de buffer cuesta una unidad, por lo que su reserva es igual a su capacidad. Los procesos declaran sus unidades requeridas.

- `NEW -> READY` requiere reserva completa de memoria.
- Procesos bloqueados conservan su RAM.
- Al terminar, devolver la RAM y retirar el proceso de CPU y de todas las colas de espera antes de conservarlo en terminados.
- Rechazar un proceso cuya memoria requerida supere la RAM total de su computador, porque nunca podría admitirse. Si solo falta memoria libre temporal, permanece en nuevos.
- Crear un buffer reserva inmediatamente RAM del anfitrión. Si no cabe, rechazar la creación con un mensaje; no crear un buffer parcialmente operativo.
- No se permite eliminar buffers en uso. La eliminación de buffers y cancelación manual de procesos no son parte del alcance inicial.

Terminar por deadline debe limpiar esperas y reservas incluso si el proceso estaba en una sección crítica. `BufferOperation` registra permisos adquiridos, propiedad del mutex e inserción/extracción ya confirmada. La limpieza devuelve permisos aún no utilizados y completa la contabilidad correspondiente a modificaciones ya confirmadas; una inserción o extracción confirmada no se revierte. El mantenimiento del núcleo no ejecuta instrucciones nuevas del proceso terminado y queda registrado en el log.

## 7. Buffers, semáforos simulados y red

Cada buffer tiene ID, anfitrión, capacidad, contenido FIFO y tres semáforos simulados:

- `vacíos`: capacidad inicial.
- `llenos`: cero inicial.
- `mutex`: uno inicial.

Se usa la convención de contador no negativo: `wait` toma un permiso disponible o encola el PCB; `signal` entrega directamente el permiso al primer bloqueado, o incrementa el contador si no hay espera. Las colas son FIFO propias y se muestran junto al valor. No se usan los hilos reales para representar esas esperas.

Varios productores y consumidores pueden compartir un buffer desde cualquier computador. Un proceso mantiene una única operación pendiente; no intenta adquirir otra vez un permiso ya obtenido.

Para un acceso remoto, antes de iniciar los cinco pasos de núcleo, bloquear por latencia `L`. Si la operación está lista al comienzo del ciclo `t`, espera los `L` ciclos `t` a `t+L-1` y puede retomar en `t+L`. Iniciar esta espera no es una instrucción adicional. Con `L=0` no hay bloqueo de red y el primer paso puede ejecutarse en `t`. Solo hay una espera de red por elemento, sin multiplicarla por cada operación de semáforo. La latencia no retiene el mutex ni un permiso de conteo.

El buffer contiene elementos con identificador propio y productor de origen. Sus unidades son tokens simulados, no bytes de comunicación real.

Invariantes que deben verificarse:

- Ocupación entre cero y capacidad; nunca se extrae un elemento inexistente ni se sobreescribe otro.
- Un solo dueño del mutex.
- Un PCB no aparece en dos colas de espera y no puede estar bloqueado y en CPU.
- Los permisos libres, reservados y las modificaciones pendientes se contabilizan sin fugas; no suponer `vacíos + llenos = capacidad` en medio de una operación.
- Tras retirar un proceso vencido no quedan referencias suyas en semáforos o transferencias.
- Producidos menos consumidos es igual a ocupación, desde el inicio vacío.

## 8. Interfaz y persistencia

La interfaz tendrá:

1. Configuración: cantidad de computadores (mínimo 2), RAM y política inicial por máquina, quantum, duración del ciclo y latencia.
2. Formularios de procesos y buffers, disponibles antes y durante la simulación. Campos específicos según tipo y validación de tipos, rangos y referencias.
3. Panel por computador: proceso en CPU, instrucción, PC, MAR, modo, prioridad, deadline, PCB completo, RAM usada/libre, política y quantum editables.
4. Colas de nuevos, listos, bloqueados con motivo y terminados; orden visible y acceso al PCB de cada fila.
5. Vista global: ciclo, buffers, anfitrión, elementos, capacidad, valores/colas de semáforos y log de decisiones con ciclo y computador.
6. Métricas por máquina/globales y un gráfico de utilización con una serie por computador y ejes rotulados.
7. Iniciar, pausar, avanzar un ciclo y reiniciar. Reiniciar requiere reconstruir el escenario inicial; no deja hilos antiguos activos.

Los cambios durante ejecución se aplican en el próximo límite de ciclo. Número de computadores, RAM y latencia se configuran antes de iniciar; el enunciado no exige modificarlos en caliente.

JSON tendrá una versión de formato y guardará duración, máquinas/RAM/políticas/quantum, latencia, carga inicial completa y buffers/anfitriones. Guarda escenarios, no checkpoints de PCB, semáforos o hilos en mitad de ejecución. La GUI distinguirá escenario inicial de estado vivo; las altas realizadas durante la ejecución no alteran silenciosamente la configuración guardada. Importar un archivo se permite al estar detenido y crea una nueva simulación desde ciclo cero.

La lectura valida el archivo completo antes de reemplazar el escenario: campos obligatorios, valores positivos, latencia no negativa, enums, referencias a máquinas/buffers, IDs duplicados y capacidad/RAM. Un error conserva la configuración anterior y muestra su causa. Escribir JSON de forma segura mediante archivo temporal y reemplazo al finalizar.

## 9. Métricas y evidencia

Todas las fórmulas usan ciclos lógicos. Mostrar «sin datos» cuando no existe denominador.

- Throughput: procesos completados normalmente / ciclos transcurridos. Los vencidos se cuentan aparte.
- Utilización: ciclos ejecutando instrucciones / ciclos transcurridos. Mostrar desglose usuario/núcleo; no contar inactividad como ocupación.
- Respuesta: promedio de primera ejecución menos llegada, entre procesos que ya recibieron CPU. Responder no significa terminar.
- Cumplimiento de deadlines: completados normalmente / (completados normalmente + terminados por deadline). Mostrar también los pendientes.
- Equidad: índice de Jain sobre ciclos de servicio recibidos por procesos admitidos durante la ejecución: `(sum(x))^2 / (n * sum(x^2))`. Se incluye servicio de núcleo y usuario. Es una medida de reparto, no una garantía de ausencia de inanición; comparar escenarios con cargas equivalentes.
- Global: sumar contadores base y recalcular las fórmulas. Utilización global divide entre computadores por ciclos; no sumar porcentajes.
- Por buffer: producidos, consumidos y ocupación.
- Espera en semáforos: suma de ciclos bloqueados / cantidad de episodios de bloqueo terminados, incluidos los terminados por deadline. Mostrar por separado episodios aún abiertos; no mezclar E/S o red.
- Gráfico: utilización acumulada por computador frente al ciclo global, con una muestra por ciclo.

Pruebas de aceptación:

- Una instrucción por computador y ciclo; pausa y paso manual conservan la misma semántica.
- IDs distintos entre máquinas y transiciones válidas, sin duplicación en colas.
- Trazas pequeñas con orden esperado para las cuatro políticas, empates, apropiación, quantum y cambio de política.
- Finalización justo en el deadline y vencimiento en nuevo, listo, CPU, E/S, red y semáforo.
- Falta de RAM, liberación y admisión posterior; buffers también ocupan RAM.
- Buffer de capacidad uno, varios productores/consumidores y operaciones intercaladas.
- Deadline en cada fase del protocolo de buffer, incluido permiso concedido aún no usado.
- Latencias cero, uno y varios ciclos, con CPU disponible para otro proceso.
- Lectura/escritura de escenario equivalente y rechazo de archivos inválidos sin perder el escenario previo.
- Cálculos manuales de métricas; ningún proceso, ningún completado y ningún bloqueo.
- Prueba sostenida con varios computadores; cierre/reinicio sin hilos supervivientes.
- Revisión de GUI y ejecución real desde una copia limpia del repositorio en NetBeans.

## 10. Entregables y secuencia de construcción

Secuencia: entorno y estructuras; modelo/procesador/memoria; reloj y clúster; políticas y E/S; buffers/red; GUI y persistencia; métricas; integración e informe. La GUI básica y la documentación acompañan al núcleo desde las primeras entregas.

El informe final documentará clases/métodos principales, enums e interfaces, protocolo de reloj, fórmulas y decisiones de interpretación. Incluirá dos diagramas UML: clases y estados de proceso. Las conclusiones compararán las cuatro políticas y escenarios de productor-consumidor local y remoto sobre cargas equivalentes.

El historial registrará cambios pequeños con verificaciones. Cada funcionalidad se integrará mediante PR. La entrega incluye informe PDF y enlace de GitHub; el estudiante realiza el envío académico y asiste a la defensa.

Estado del entorno comprobado el 19 de octubre de 2026:

- NetBeans instalado en `/Applications/Apache NetBeans.app`; el estudiante confirma que lo tiene abierto.
- Temurin JDK y compilador 25.0.4.1 instalados en `/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home`.
- Maven 3.9.16 incluido en NetBeans, ejecutado correctamente con ese JDK mediante `mvn --version`.
- La terminal conserva Java 17 como selección predeterminada. Las verificaciones del proyecto deben seleccionar explícitamente el JDK 25, sin cambiar la configuración global del estudiante. En NetBeans también se debe seleccionar JDK 25 como plataforma del proyecto.
- Repositorio Git local creado con ramas `main` y `develop`; diseño inicial registrado en el commit `85694c0`.

Pendiente externo: identificar o crear el repositorio GitHub cuando esté disponible la cuenta/conexión correspondiente. También falta configurar y verificar el proyecto real dentro de NetBeans; las comprobaciones de versiones no sustituyen una compilación ni una ejecución del simulador.
