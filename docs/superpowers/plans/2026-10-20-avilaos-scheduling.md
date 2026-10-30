# ÁvilaOS Scheduling Policies Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** completar las cuatro políticas del proyecto y permitir cambiar política/quantum al comienzo de un ciclo, preservando contexto, memoria y E/S.

**Architecture:** continuar el núcleo existente mediante SchedulingPolicy. Las políticas deciden selección/apropiación; Kernel cambia estados y controla el turno consumido. La cola conserva el orden de entrada a listos aunque otra política cambie su presentación. Computer permite configuración inicial y solicitudes de cambio; el coordinador con hilos y la GUI completa siguen en módulos posteriores.

**Tech Stack:** Java 25, Maven/NetBeans, estructuras propias y runner AllTests; sin nuevas dependencias.

**Spec:** [diseño aprobado](../specs/2026-10-18-avilaos-design.md), secciones 3, 5, 6 y 11; módulo 3 de la [hoja de ruta](2026-10-19-avilaos-roadmap.md). La especificación general ya fue aprobada; este plan concreta su ejecución sobre el módulo 2.

**Execution:** conservar el método nativo y la carpeta compartida con NetBeans elegidos por el estudiante; implementar tarea por tarea, con una revisión independiente al final. Plan aprobado por el estudiante el 20 de octubre de 2026 («hazlo»).

## Global Constraints

- JDK de desarrollo propuesto: 25, sin características preview. Cumple «versiones posteriores a Java 21».
- GUI: Swing, incluida en Java; sin bibliotecas externas para componentes de interfaz. No modificar la ventana inicial en este módulo.
- No usar colecciones del framework de Java: tampoco List, Map, Set, PriorityQueue, Collections, Arrays.asList o resultados de streams que creen colecciones. Usar arreglos y estructuras propias.
- No delegar el ordenamiento de colas a utilidades o bibliotecas.
- Mantener CPU, RAM y E/S sin dependencias de políticas ni ventanas. Kernel conserva la propiedad de sus transiciones.
- CPU y E/S siguen recibiendo exactamente un tick consecutivo por ciclo; una apropiación no ejecuta instrucciones ni consume un ciclo adicional.
- FCFS: no apropiativa, orden de entrada a listos.
- EDF: apropiativa, menor deadline absoluto primero.
- Round Robin: FIFO; quantum positivo por computador, consumido por instrucciones tanto de usuario como de núcleo. Al expirar, el proceso vuelve al final.
- Prioridades: apropiativa, número menor significa prioridad mayor.
- Empates: conservar orden de entrada a listos. La llegada de otro proceso con la misma prioridad o deadline no causa apropiación.
- Un cambio de política entra en vigor al comienzo del próximo ciclo; reinicia el quantum consumido y reordena listos. La nueva política evalúa al proceso actual según sus propias reglas. No hay costo adicional de cambio de contexto en esta versión.
- Conservar las 509 comprobaciones existentes y proporcionar una guía breve independiente del módulo 3 en docs/defensa/.

## Decisiones operativas

- Se mantiene el constructor `Computer(int id, int ramCapacity)`: FCFS y quantum predeterminado 2. Nuevo constructor de cuatro argumentos permite elegir política y quantum iniciales; incluso FCFS/EDF/prioridades validan quantum positivo para que los cambios posteriores sean seguros.
- Se amplía SchedulingPolicy con `SchedulingPolicyType type()`. Cada implementación identifica su enum; la política sintética de SchedulingTest declara FCFS como identificador de prueba y conserva su selección del último elemento.
- `SchedulingPolicies.create(type)` valida y construye una de las cuatro políticas. No contiene estados de procesos.
- `Kernel.configureScheduling(type, quantum)` y su delegado en Computer guardan una configuración pendiente validada. Antes de onTick los accesores siguen mostrando la configuración activa. Varias solicitudes válidas antes del mismo tick conservan la última; una solicitud inválida no reemplaza la pendiente.
- Un cambio de quantum también inicia un turno nuevo. Aplicar configuración se hace después de validar el tick y antes de avanzar E/S; un tick inválido no consume el cambio pendiente.
- `sliceUsed` cuenta instrucciones desde el último despacho; se incrementa una vez tras CPU.onTick si ejecutó. Se reinicia en cada despacho, bloqueo, terminación y cambio de configuración.
- RR decide apropiación al comienzo del ciclo siguiente cuando `sliceUsed >= quantum`, incluso sin competidores; en ese caso el mismo proceso vuelve a despacharse sin costo de ciclo ni alterar su primera respuesta. Un proceso que termina o se bloquea en su última instrucción del quantum se retira normalmente, sin reencolarlo.
- Antes de evaluar apropiación se entregan E/S y se admiten nuevos. Un recién llegado o desbloqueado puede desplazar al actual si tiene deadline/prioridad estrictamente mejor. El proceso apropiado vuelve al final del orden de entrada a listos, conserva PC/MAR y RAM, y después se reordena según la política.
- La cola debe poder restaurar FIFO histórico de los procesos actualmente listos. Ordenar por otra política no cambia ese historial; retirar y reinsertar un PCB sí constituye una entrada nueva. Para empates EDF/prioridades se restaura FIFO antes del ordenamiento estable por clave.

## Review Focus

1. Cambiar EDF/prioridades a FCFS/RR debe restaurar el FIFO de entrada a listos, no reutilizar accidentalmente el orden por otra clave; cubrir en tareas 1, 2 y 4.
2. Un cambio pendiente seguido de tick inválido o configuración inválida debe seguir pendiente e intacto, sin mover PC, RAM, colas ni contadores; cubrir en tarea 4.
3. Agotar quantum con una instrucción KERNEL de E/S no debe duplicar el PCB en listos/bloqueados ni perder su reserva o ticket; cubrir en tarea 4.
4. Apropiar por un recién admitido/desbloqueado con clave mejor debe conservar contexto/RAM; una clave igual no apropia; cubrir en tareas 2 y 3.
5. Con un solo proceso en RR, cambios sucesivos de política o finalización en el último ciclo permitido no deben añadir instrucciones, retrasar E/S ni reiniciar firstRunCycle; cubrir en tareas 3 y 4.

## Verificación

`BUILD` significa ejecutar desde la raíz:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -o -B -Dmaven.repo.local=.maven-cache test
```

Esperado al pasar: `OK: N comprobaciones` con N >= 509 y `BUILD SUCCESS`. AllTests conserva todas las llamadas anteriores; agregar cada prueba nueva. En cada tarea: escribir prueba, observar fallo por comportamiento/clase ausente, implementar, ejecutar BUILD completo y registrar commit. El mensaje de Surefire «Tests run: 0» sigue siendo independiente del runner propio.

## Task 1: conservar FIFO al reordenar listos

**Files:** modificar `src/main/java/avilaos/structures/OrderedProcessQueue.java`; crear `src/test/java/avilaos/tests/ReadyOrderTest.java`; registrar en `AllTests.java`.

**Interfaces:** conservar todos los métodos actuales y añadir `void restoreFifo()`. Mantener por cada entrada una posición de incorporación independiente de su posición ordenada, usando estructuras propias. `sort(Comparator)` sigue siendo estable sobre el orden actual; restoreFifo recupera el orden de entrada de los elementos vivos.

- [x] Prueba: encolar A,B,C; ordenar por prioridad que da C,A,B; restoreFifo da A,B,C. Retirar B y reinsertarlo da A,C,B incluso después de ordenar/restaurar. IDs pueden ser 30,10,20: FIFO no se deriva del ID ni de la llegada del PCB.
- [x] Prueba: ordenar/restaurar vacío y un elemento; rechazar nulo/NEW/duplicados sigue intacto y los empates de sort conservan el orden actual.
- [x] Ejecutar BUILD: esperado fallo porque restoreFifo no existe.
- [x] Implementar historial interno sin cambiar los PCB ni usar colecciones estándar. Si se usa contador ordinal, prevenir su overflow antes de mutar; puede reiniciarse cuando la cola queda vacía.
- [x] BUILD: esperado éxito; commit `feat: conserva FIFO al reordenar procesos listos`.

## Task 2: políticas y selección intercambiable

**Files:** modificar `scheduling/{SchedulingPolicy,FcfsPolicy}.java`, `kernel/Scheduler.java`, `tests/SchedulingTest.java`; crear `scheduling/{EdfPolicy,RoundRobinPolicy,PreemptivePriorityPolicy,SchedulingPolicies}.java` y `tests/PolicyTest.java` bajo sus raíces existentes main/test Java.

**Interfaces:** añadir `SchedulingPolicyType type()` al contrato. `SchedulingPolicies.create(SchedulingPolicyType type)` devuelve SchedulingPolicy y rechaza null. Scheduler conserva constructor inyectado y añade `void setPolicy(SchedulingPolicy policy)`, `SchedulingPolicyType policyType()`, `void reorder()`, `boolean shouldPreempt(ProcessControlBlock running, int sliceUsed, int quantum)`. Validar argumentos nulos antes de cambiar política. `select()` conserva la delegación actual (policy.select y después policy.reorder); cada política garantiza su criterio también al llamar select directamente, reordenando antes de retirar si corresponde. setPolicy reordena al instalar la política.

- [x] Pruebas con READY en orden A,B,C: deadlines absolutos 30,10,10 dan B,C,A; prioridades 2,0,0 dan B,C,A. FcfsPolicy/RoundRobinPolicy restauran A,B,C al cambiar desde el orden EDF/prioridades. Seleccionar retira exactamente un PCB y no cambia su estado.
- [x] EDF apropia solo si el menor deadline listo es estrictamente menor al actual; prioridades solo con número estrictamente menor. Verificar empates, vacío y claves extremas con comparación sin restas que desborden. Comparar deadlines absolutos con llegadas distintas y plazo suficiente, no los plazos relativos.
- [x] RR: slice 1/quantum 2 => false; slice 2/2 => true; también con cola vacía. Quantum 0/negativo o slice negativo se rechaza en RR. Con running nulo, las cuatro políticas devuelven false; FCFS nunca apropia. Reorder no modifica PCB, memoria ni cantidad de elementos.
- [x] Prueba factory para los cuatro enums y null; cambio Scheduler.setPolicy conserva la cola y aplica el nuevo criterio. Actualizar la política sintética anterior con type() y mantener su verificación del callback posterior.
- [x] BUILD: esperado fallo por clases/API ausentes. Implementar criterios, factory y delegación; restaurar FIFO antes de orden estable por clave.
- [x] BUILD: esperado éxito; commit `feat: implementa EDF RR y prioridades apropiativas`.

## Task 3: apropiación y quantum en el núcleo

**Files:** modificar `kernel/Kernel.java`; crear `tests/PreemptiveKernelTest.java`; registrar en AllTests.

**Interfaces:** conservar constructor Kernel de cinco argumentos y delegarlo a uno nuevo `Kernel(int computerId, Cpu cpu, Ram ram, IoController io, SchedulingPolicy policy, int quantum)`. Predeterminado 2; validar quantum > 0. Añadir `int quantum()`, `int sliceUsed()`, `SchedulingPolicyType policyType()`.

- [x] EDF: A CPU de 4 instrucciones, llegada 0 y plazo 20; tras tick 0, llega B de 1 instrucción en 1 con plazo 3. Orden A,B,A,A,A en ciclos 0..4; A conserva PC=1 mientras ejecuta B, firstRun=0 y su RAM. Prioridades: repetir con A prioridad 5 y B 1, plazos 20; misma traza.
- [x] Empates: A de 3 instrucciones prioridad 1/deadline absoluto 10; B llega en 1 con misma prioridad/deadline absoluto 10. A conserva CPU. Probar EDF con B llegada 1/plazo 9 y prioridades con plazos suficientes.
- [x] RR: A de 3 y B de 2 instrucciones, quantum 2: A,A,B,B,A en 0..4. Quantum 1: A,B,A,B,A. Guardar PC/MAR, RAM y primera ejecución a través de turnos; al final toda RAM libre.
- [x] RR con A solo, 5 instrucciones y quantum 2: ejecuta en 0..4 sin ciclos IDLE y firstRun sigue 0. FCFS conserva el escenario A,A,B del módulo 2 aunque quantum sea 1.
- [x] Desbloqueo: A IO de 3 instrucciones/ráfaga 1/duración 2, prioridad 0/plazo 10; B CPU de 5, prioridad 5/plazo 20. En prioridades A usa 0/1, B usa 2/3, A vuelve en 4 y lo apropia. Análogo EDF usando deadline absoluto menor de A. PC de B sigue 2 y su RAM se conserva en 4.
- [x] BUILD: esperado fallo por constructor/API o traza no apropiativa. Implementar evaluación después de E/S/admisión y antes de despacho. Reencolar únicamente tras PCB.preempt, limpiar CPU y resetear turno; reordenar tras las admisiones también al cierre para exponer listos coherentes.
- [x] BUILD: esperado éxito, incluidas las 509 comprobaciones anteriores; commit `feat: aplica apropiacion y quantum por computador`.

## Task 4: configuración inicial y cambios en límites de ciclo

**Files:** modificar `kernel/Kernel.java`, `simulation/Computer.java`; crear `tests/SchedulingChangeTest.java`; registrar en AllTests.

**Interfaces:** `Kernel.configureScheduling(SchedulingPolicyType type, int quantum)` registra una pareja pendiente validada. Constructor `Computer(int id, int ramCapacity, SchedulingPolicyType policy, int quantum)` usa factory y Kernel extendido; el constructor actual delega a FCFS/2. Computer expone `configureScheduling(type, quantum)`, `policyType()`, `quantum()`, `sliceUsed()` delegando al núcleo. No agregar hilos ni llamadas desde Swing.

- [x] Cambio: A CPU 4/deadline 20 y B CPU 1/deadline 5 llegan en 0, inicio FCFS. Tras tick 0 solicitar EDF; aún muestra FCFS y PC intactos. En tick 1 aplica EDF, B apropia/termina y A conserva contexto. Volver a FCFS entra en vigor al próximo tick y no causa apropiación inmediata de A por otro listo.
- [x] FIFO histórico integrado: A de 1, B de 3 y C de 1 instrucciones llegan en 0; prioridades A=0,B=5,C=1. Inicio prioridades: A termina en 0, listos se presentan C,B. Solicitar FCFS antes de 1: B debe ejecutar en 1 por su entrada previa, no C. Repetir cambio a RR con quantum 1.
- [x] Turno nuevo: A/B de 4 instrucciones, RR/2. A ejecuta 0; solicitar RR/1 antes de 1. Tick 1 reinicia turno y vuelve a ejecutar A; tick 2 ejecuta B. Solicitar RR/2 tras un tick con turno consumido también reinicia el contador.
- [x] Cambios inválidos: null, quantum 0/negativo no cambian activo/pendiente. Guardar EDF/2 válido, intentar null/0 y ticks repetidos/saltados; PC, RAM, colas, slice y configuración activa se conservan. Próximo tick válido sí aplica EDF. Dos válidos seguidos: gana el último. Cambiar con CPU libre y todos bloqueados/vacío también funciona.
- [x] E/S en frontera de quantum: IO A tiene 2 instrucciones/ráfaga 1/duración 2, RR/2; CPU B tiene 2. A usa 0/1 y queda BLOCKED restante 0, no READY; B usa 2/3, A termina al inicio de 4. Verificar un solo ticket, sin duplicados y RAM finalmente libre. Plazo de A=4: vence en 3 y no revive en 4/5. Cambiar política mientras espera no altera su fin de E/S.
- [x] Independencia: dos Computer con políticas/quantum distintos; configurar uno no cambia el otro. Quantum inicial no positivo y política inicial null se rechazan.
- [x] BUILD: esperado fallo por nuevas API/configuración ausentes. Implementar aplicación pendiente después de la validación de tick y antes del avance de componentes; usar factory y el mismo Scheduler, sin duplicar colas ni reemplazar hardware.
- [x] BUILD y clean package: esperado éxito; commit `feat: cambia planificacion en limites de ciclo`.

## Task 5: evidencia, guía breve y revisión final

**Files:** actualizar README, hoja de ruta e índice `docs/defensa/README.md`; crear `docs/defensa/modulo-3-planificacion.md` y `docs/verification/2026-10-20-scheduling.md` (usar fecha real de ejecución si cambia).

- [x] Registrar las trazas efectivamente comprobadas, total de comprobaciones y salida de clean package; documentar el resultado de NetBeans compartido por el estudiante el 20 de octubre a las 13:13 para módulo 2 sin atribuirle operaciones que el fragmento no muestra.
- [x] Guía breve con el formato acordado: qué hicimos, conceptos esenciales, clases, ejemplo de RR, tres preguntas y explicación oral corta. Explicar apropiación frente a bloqueo, quantum, empates y efecto de cambiar política. Referenciar guías anteriores sin repetirlas.
- [x] Ejecutar BUILD; esperado éxito. Verificar imports permitidos y git diff --check. Actualizar el estado real de cobertura, sin declarar reloj/hilos/GUI terminados.
- [x] Generar paquete de revisión del rango completo y solicitar una revisión independiente según executing-plans. Corregir hallazgos importantes con prueba que falle primero; registrar decisiones de alcance y menores diferidos si los hubiera.
- [x] Conservar rama funcional/commits y carpeta de NetBeans; GitHub/PR sigue pendiente de remoto. Commit `docs: explica y verifica politicas para la defensa`.

Estado: módulo 3 implementado y verificado; revisión independiente sin hallazgos. Módulo 4 pendiente.
