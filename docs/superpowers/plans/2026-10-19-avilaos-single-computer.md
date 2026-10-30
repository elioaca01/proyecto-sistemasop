# ÁvilaOS Single Computer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** ejecutar procesos CPU bound e I/O bound en un computador reutilizable, con FCFS, memoria limitada, admisión, bloqueos y vencimiento de deadlines.

**Architecture:** Computer ensambla CPU, RAM, controlador de E/S y Kernel. El núcleo coordina cada ciclo lógico y los cambios del PCB; CPU ejecuta instrucciones y el controlador entrega las E/S completadas. La política de selección se inyecta mediante interfaz y las estructuras siguen siendo propias.

**Tech Stack:** Java 25, proyecto Maven existente, runner Java AllTests y estructuras del módulo 1. Sin nuevas dependencias.

**Spec:** [diseño aprobado](../specs/2026-10-18-avilaos-design.md), especialmente secciones 3–6. Continúa el módulo 2 de la [hoja de ruta](2026-10-19-avilaos-roadmap.md).

**Execution:** nativa, aprobada por el estudiante el 20 de octubre de 2026, en esta conversación y en la misma carpeta abierta en NetBeans. Al cerrar cada módulo se entregan explicación de clases y conceptos para la defensa, según la petición del estudiante. El módulo no implementa todavía el reloj global con hilos, productor-consumidor, las otras tres políticas ni la GUI completa.

## Global Constraints

- JDK de desarrollo propuesto: 25, sin características preview. Cumple «versiones posteriores a Java 21».
- GUI: Swing, incluida en Java; sin bibliotecas externas para componentes de interfaz.
- No usar colecciones del framework de Java: tampoco List, Map, Set, PriorityQueue, Collections, Arrays.asList o resultados de streams que creen colecciones. Usar arreglos y estructuras propias.
- No delegar el ordenamiento de colas a utilidades o bibliotecas.
- CPU y controlador de E/S comparten la interfaz Tickable.
- La política de planificación se define por interfaz; FCFS no debe quedar codificado dentro del Kernel.
- Conservar las pruebas existentes y ejecutar la suite completa después de cada tarea.
- Los nuevos hilos y la protección mediante semáforos Java se incorporan con el reloj del clúster; en este módulo el motor recibe ciclos manuales, sin temporizadores en Swing.
- No modificar la GUI inicial ni aparentar que existe ya una simulación interactiva completa.

## Review Focus

1. Vencer un proceso NEW no debe liberar memoria que nunca reservó; repetir una finalización no debe liberar RAM ajena: tareas 1, 3 y 6.
2. Una solicitud de E/S puede ser la última instrucción; el proceso debe esperar a que termine la operación, salvo vencimiento: tareas 4–6.
3. Repetir, retroceder o saltar ciclos no puede ejecutar instrucciones adicionales ni alterar colas: tareas 4–6.
4. Un proceso grande en nuevos no debe impedir admitir otro pequeño que sí quepa: tareas 3 y 6.
5. Una E/S cancelada por deadline no puede despertar después a un proceso terminado: tareas 5–6.

## Decisiones de simulación

- El primer ciclo es 0. Cada `onTick(c)` debe recibir exactamente el ciclo siguiente del componente; ciclos negativos, repetidos, saltos y `Long.MAX_VALUE` se rechazan antes de mutar su estado. CPU y controlador se actualizan también cuando no tienen trabajo.
- Orden del Kernel: completar E/S; admitir; despachar si CPU libre; ejecutar como máximo una instrucción; bloquear o completar según el resultado; vencer deadlines al cierre; readmitir con RAM liberada para el próximo ciclo.
- FCFS es FIFO por entrada a listos, sin apropiación. Un proceso que vuelve de E/S entra al final.
- Programa CPU bound: todas las instrucciones son COMPUTE en USER.
- Programa I/O bound: repetir `cpuBurst` instrucciones COMPUTE y una IO_REQUEST de un ciclo en KERNEL. El presupuesto total incluye la solicitud. El programa se corta al completar su número total de instrucciones.
- Una solicitud emitida en ciclo t, con duración d positiva, espera los ciclos t+1 a t+d y completa al comienzo de t+d+1. Ejemplo: solicitud en 1 y duración 2 completa en 4.
- Si la última instrucción solicita E/S, el PCB queda bloqueado con restante 0; termina cuando completa la E/S. No se despacha ni ejecuta una instrucción extra.
- Las solicitudes de E/S progresan de forma independiente; no se simula exclusividad de un dispositivo físico en este módulo.
- Si acaba su última instrucción de cómputo en el último ciclo permitido, completa normalmente antes de evaluar vencimiento. Si todavía espera E/S, vence aunque restante sea 0.
- Una finalización de E/S al comienzo del ciclo puede liberar RAM para admisión y ejecución en ese mismo ciclo. La memoria liberada por CPU o deadline al cierre permite admitir, pero no ejecutar hasta el siguiente ciclo.
- Se rechaza al enviar un proceso de otro computador, ID ya conocido, estado distinto de NEW, llegada distinta del siguiente ciclo del Kernel o memoria mayor que la RAM total. Productores/consumidores se rechazan explícitamente hasta el módulo de buffers.

## Construcción y archivos

Todos los caminos son relativos a la raíz. `BUILD` significa:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -o -B -Dmaven.repo.local=.maven-cache test
```

Cada tarea añade su prueba a `src/test/java/avilaos/tests/AllTests.java`. Los fallos se conservan en el workspace del plan y deben corresponder al comportamiento o clase ausente, no a errores de red o JDK.

## Task 1: RAM con reservas y liberación segura

**Files:** crear `src/main/java/avilaos/hardware/Ram.java`, `src/test/java/avilaos/tests/RamTest.java`.

**Interfaces:** `Ram(int capacity)`; `int capacity()`, `int used()`, `int free()`; `Ram.Reservation reserve(int units)` devuelve una reserva o null si falta RAM libre; `void release(Ram.Reservation reservation)`; `Reservation.units()`. La reserva guarda privadamente su RAM de origen y si sigue activa.

- [x] Escribir RamTest: capacidad 10, reservar 7 deja usada/libre 7/3; intentar 4 devuelve null sin variar contadores; liberar deja 0/10. Rechazar capacidad y unidades no positivas, unidades mayores que capacidad, reserva nula, reserva de otra RAM y doble liberación, sin alterar otras reservas.
- [x] Ejecutar BUILD y observar fallo por Ram ausente.
- [x] Implementar los contratos con contadores; usar `units <= free()` para no desbordar sumas. No usar particiones ni direcciones físicas.
- [x] Ejecutar BUILD completo y registrar `feat: agrega RAM con reservas verificables`.

```java
Ram ram = new Ram(10);
Ram.Reservation reservation = ram.reserve(7);
TestSupport.equal(3, ram.free());
TestSupport.equal(null, ram.reserve(4));
ram.release(reservation);
TestSupport.throwsType(IllegalStateException.class, () -> ram.release(reservation));
TestSupport.equal(10, ram.free());
```

## Task 2: cola de listos y política FCFS intercambiable

**Files:** crear `src/main/java/avilaos/structures/OrderedProcessQueue.java`, `src/main/java/avilaos/scheduling/{SchedulingPolicy,FcfsPolicy}.java`, `src/main/java/avilaos/kernel/Scheduler.java`, `src/test/java/avilaos/tests/SchedulingTest.java`.

**Interfaces:** `OrderedProcessQueue` ofrece `void addLast(ProcessControlBlock)`, `ProcessControlBlock poll()`, `ProcessControlBlock peek()`, `ProcessControlBlock get(int)`, `ProcessControlBlock remove(long id)`, `int size()`, `boolean isEmpty()` y `void sort(Comparator<? super ProcessControlBlock>)`. Acepta solo READY, sin IDs duplicados; poll/peek/remove ausente devuelven null.

`SchedulingPolicy` expone `ProcessControlBlock select(OrderedProcessQueue ready)`, `boolean shouldPreempt(ProcessControlBlock running, OrderedProcessQueue ready, int sliceUsed, int quantum)` y `void reorder(OrderedProcessQueue ready)`.

`FcfsPolicy` retira la cabeza, nunca apropia y conserva el orden restante. `Scheduler(OrderedProcessQueue ready, SchedulingPolicy policy)` expone `ProcessControlBlock select()` que llama select y luego reorder. No cambia el estado del PCB.

- [x] Escribir SchedulingTest con tres PCB READY: selección A,B,C; vacía devuelve null; retirar B por ID mantiene A,C; reinsertar B después de un bloqueo simulado lo deja al final; rechazar nulo, NEW e ID duplicado. Verificar estabilidad de sort con dos prioridades iguales.
- [x] Añadir una política de prueba que use los mismos contratos y seleccione el último elemento: Scheduler debe usarla sin modificar su código y llamar reorder después de cada selección.
- [x] Ejecutar BUILD y observar fallo por clases ausentes.
- [x] Implementar cola sobre LinkedList propia e interfaz/FCFS/Scheduler. No introducir las otras políticas todavía.
- [x] Ejecutar BUILD completo y registrar `feat: agrega FCFS mediante interfaz de planificacion`.

## Task 3: admisión y propiedad de reservas

**Files:** crear `src/main/java/avilaos/kernel/MemoryAdmission.java`, `src/test/java/avilaos/tests/MemoryAdmissionTest.java`.

**Interfaces:** `MemoryAdmission(Ram ram)`, `boolean admit(ProcessControlBlock process)`, `boolean releaseIfAllocated(ProcessControlBlock process)`. Una lista propia relaciona PCB y reserva. admit requiere NEW y rechaza un ID con reserva activa; solo cambia PCB a READY después de reservar RAM. La liberación solo afecta a ese mismo objeto PCB; devuelve false si no tiene reserva y, si tiene una, exige que el PCB ya esté TERMINATED. Los argumentos nulos se rechazan.

- [x] Escribir MemoryAdmissionTest: RAM 10, A requiere 8 y B 4; A pasa a READY, B sigue NEW sin asignación. Liberar A antes de terminar falla sin alterar RAM; terminar A por deadline en el ciclo 9 (plazo relativo 10, llegada 0) y liberar permite admitir B. Liberar un NEW sin reserva devuelve false y no cambia RAM. Un PCB ajeno con el mismo ID no puede liberar la reserva del original. Un intento de doble admisión falla sin reservar dos veces.
- [x] Ejecutar BUILD, esperando fallo por MemoryAdmission ausente.
- [x] Implementar con reservas de tarea 1 y lista propia; no ordenar ni decidir la cola de nuevos aquí.
- [x] Ejecutar BUILD completo y registrar `feat: administra admision y reservas por proceso`.

## Task 4: CPU y programa lineal

**Files:** crear `src/main/java/avilaos/simulation/Tickable.java`, `src/main/java/avilaos/hardware/Cpu.java`, `src/main/java/avilaos/process/LinearProgram.java`, `src/test/java/avilaos/tests/CpuTest.java`.

**Interfaces:** `Tickable.onTick(long cycle)`; `LinearProgram.nextInstruction(ProcessControlBlock process)` retorna InstructionKind sin mutar el PCB. Solo admite CPU_BOUND e IO_BOUND con instrucciones pendientes. Para E/S, usa período `cpuBurst + 1L` para evitar overflow.

`Cpu implements Tickable`: `void load(ProcessControlBlock process)` exige RUNNING, trabajo pendiente y CPU libre; `ProcessControlBlock current()`, `void clear()`, `CpuMode mode()`, `InstructionKind lastInstruction()`, `long lastProcessId()`. Cada tick válido ejecuta una sola instrucción a través del PCB. Un tick inactivo deja IDLE, lastInstruction=null y lastProcessId=-1. clear retira el PCB y conserva la información de la última instrucción hasta el próximo tick.

- [x] Escribir CpuTest: tick vacío no ejecuta nada; CPU bound de 3 instrucciones avanza PC/MAR 1 por tick y cuenta USER; rechazar carga NEW, nula, CPU ocupada, programa productor o proceso agotado. Repetir, retroceder o saltar ticks falla sin mover PC.
- [x] Para I/O bound con ráfaga 1, verificar secuencia COMPUTE, IO_REQUEST, COMPUTE y modos USER,KERNEL,USER. CPU no decide por sí misma poner el PCB en bloqueados ni terminarlo.
- [x] Ejecutar BUILD y observar fallo por las nuevas clases ausentes.
- [x] Implementar Tickable, selector lineal y CPU respetando esos límites de hardware.
- [x] Ejecutar BUILD completo y registrar `feat: ejecuta instrucciones por ciclo en CPU simulada`.

## Task 5: controlador de E/S y cancelación

**Files:** crear `src/main/java/avilaos/hardware/{IoRequest,IoController}.java`, `src/test/java/avilaos/tests/IoControllerTest.java`.

**Interfaces:** `IoController implements Tickable`, `void request(ProcessControlBlock process, long issuedCycle, int duration)`, `ProcessControlBlock pollCompleted()`, `boolean cancel(ProcessControlBlock process)`, `int pendingCount()`. request valida duración positiva, argumentos no nulos, issuedCycle igual al último tick del controlador, ausencia de otra solicitud del mismo PCB y suma temporal sin overflow. No cambia el PCB. Las completadas permanecen disponibles hasta extraerlas; cancel retira pendientes y completadas sin alterar PCB ni RAM.

`IoRequest(ProcessControlBlock process, long issuedCycle, int duration)` es el ticket inmutable de hardware que crea request; expone `ProcessControlBlock process()` y `long completesAt()`. Valida proceso no nulo, issuedCycle no negativo y duración positiva; calcula `issuedCycle + 1 + duration` con suma exacta. Su constructor permite verificar desbordamientos sin introducir ciclos iniciales artificiales en el controlador.

- [x] Escribir IoControllerTest: emitir tras tick 1, duración 2; no completa en ticks 2/3 y entrega el PCB una vez en 4. Varias solicitudes completan independientemente y conservan orden de solicitud cuando empatan. Cancelar impide una entrega posterior; cancelar ya completada también la retira.
- [x] Rechazar duración 0, nulo y doble solicitud en el controlador. Comprobar en IoRequest la suma extrema con issuedCycle=Long.MAX_VALUE y duración 1, esperando IllegalArgumentException. No añadir hooks exclusivos de pruebas a producción.
- [x] Ejecutar BUILD y observar fallo por controlador ausente.
- [x] Implementar solicitudes y completadas con listas propias y suma mediante Math.addExact, convirtiendo overflow a IllegalArgumentException.
- [x] Ejecutar BUILD completo y registrar `feat: completa y cancela E/S por ciclos logicos`.

## Task 6: núcleo y computador reutilizable

**Files:** crear `src/main/java/avilaos/kernel/Kernel.java`, `src/main/java/avilaos/simulation/Computer.java`, `src/test/java/avilaos/tests/ComputerTest.java`.

**Interfaces:** `Kernel(int computerId, Cpu cpu, Ram ram, IoController io, SchedulingPolicy policy)` crea sus colas y usa MemoryAdmission/Scheduler. Ofrece `void submit(ProcessControlBlock process)`, `void onTick(long cycle)`, `long nextCycle()`, `int newCount()/readyCount()/blockedCount()/terminatedCount()` y `ProcessControlBlock newAt(int)/readyAt(int)/blockedAt(int)/terminatedAt(int)`. Un registro propio retiene los IDs conocidos para impedir su reutilización durante la ejecución.

`Computer(int id, int ramCapacity) implements Tickable` ensambla esas piezas con FcfsPolicy. Ofrece `int id()`, `Cpu cpu()`, `Ram ram()`, `IoController io()`, `Kernel kernel()`, `void submit(ProcessControlBlock process)` y `onTick(long cycle)` que delega al Kernel. No inicia hilos ni contiene Swing.

El núcleo es dueño de las transiciones. Su finalización llama primero a PCB.terminate para validar y marcar el estado; después retira de CPU y colas, cancela E/S y libera la reserva si existe antes de añadir a terminados. Recorre todos los procesos no terminados al revisar deadlines, incluidos NEW y BLOCKED. No altera el estado al rechazar un tick o submit inválido.

- [x] Escribir ComputerTest: A de 2 instrucciones y B de 1, RAM suficiente; ejecutar ciclos 0,1,2 y comprobar orden A,A,B, PC, primera respuesta y terminación. Queda la RAM completa libre al terminar.
- [x] Caso de memoria: RAM 10; A pide 8 y tiene 2 instrucciones, B pide 4 y tiene 1, C pide 2 y tiene 1; todos llegan en 0 con plazo 10. B espera y C se admite sin saltar a CPU delante de A. Al terminar A en 1 se admite B detrás de C en listos. C ejecuta en 2 y B en 3; al final queda toda la RAM libre. No ejecutar una segunda instrucción al liberar RAM.
- [x] Caso de E/S: A llega en 0, memoria 8, 3 instrucciones, ráfaga 1 y duración 2; B llega en 0, memoria 2 y 2 instrucciones de CPU; RAM 10 y plazo 10 para ambos. A ejecuta en 0 y solicita en 1; B ejecuta en 2 y 3; A espera 2/3, vuelve a listos en 4 y termina ese ciclo. PC de A se mantiene en 2 mientras espera y RAM sigue reservada.
- [x] Caso última instrucción: A tiene 2 instrucciones, ráfaga 1, duración 2 y plazo 10; está BLOCKED con restante 0 tras ciclo 1 y termina normalmente al inicio del 4 sin otra instrucción.
- [x] Casos de deadline: una instrucción/plazo 1 completa en 0; dos instrucciones/plazo 1 vence en 0. Un NEW vence sin liberar RAM ajena. La E/S final anterior con plazo 4 vence en 3 y nunca reaparece al avanzar a 4 y 5. Comprobar cancelación y ausencia de duplicados en terminados.
- [x] Rechazar computador ajeno, ID repetido, llegada fuera de límite actual, memoria imposible y tipos aún no soportados sin cambiar contadores. Repetir o saltar tick deja PC, RAM y colas intactos. Dos instancias Computer conservan RAM/colas independientes.
- [x] Ejecutar BUILD y observar fallo por núcleo/computador ausentes.
- [x] Implementar exactamente el orden de ciclo acordado. En E/S completada con restante 0 finalizar; con trabajo pendiente, desbloquear y encolar. Admitir recorriendo toda la cola de nuevos para permitir que quepan procesos pequeños.
- [x] Ejecutar BUILD completo y `clean package`, revisar que las 212 comprobaciones del módulo 1 siguen pasando y registrar `feat: integra computador con FCFS memoria y E/S`.

## Cierre

- [x] Actualizar README y hoja de ruta con las capacidades reales, sin afirmar que existe ya un clúster con hilos.
- [x] Registrar trazas de ComputerTest, resultados y revisión independiente en `docs/verification/2026-10-20-single-computer.md`; guía en `docs/defensa/modulo-2-computador.md`.
- [x] Conservar una rama funcional y commits separados; PR hacia develop cuando se conecte GitHub. No fusionar para simular un PR.
- [x] Revisar fugas de RAM, tickets de E/S y referencias duplicadas, además de los cinco casos de Review Focus.
- [ ] Continuar después con las otras políticas y el reloj distribuido de la hoja de ruta.

Estado: módulo 2 implementado y verificado; siguiente módulo pendiente.
