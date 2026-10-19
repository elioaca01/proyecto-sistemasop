# ÁvilaOS Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** entregar la base ejecutable en NetBeans, estructuras propias y un modelo de procesos con transiciones y deadlines comprobados.

**Architecture:** las estructuras son independientes del modelo de procesos; el PCB encapsula su estado y contexto. Swing tendrá un punto de entrada mínimo, separado de estas clases. CPU, planificación, hilos del clúster y buffers se implementan en los siguientes módulos de la hoja de ruta.

**Tech Stack:** JDK 25, Maven incluido en NetBeans, Swing y pruebas ejecutables Java sin bibliotecas externas.

**Spec:** [diseño aprobado](../specs/2026-10-18-avilaos-design.md). [Cobertura del proyecto completo](2026-10-19-avilaos-roadmap.md).

**Ejecución aprobada el 19 de octubre:** nativa, por Codex en esta conversación, con el estudiante revisando y ejecutando en NetBeans. No iniciar agentes adicionales sin que el estudiante elija ese método.

## Global Constraints

- JDK de desarrollo propuesto: 25, sin características preview. Cumple «versiones posteriores a Java 21».
- IDE obligatorio: Apache NetBeans con soporte para el JDK elegido. La validación final incluye abrir, compilar y ejecutar allí.
- GUI: Swing, incluida en Java; sin bibliotecas externas para componentes de interfaz.
- No usar colecciones del framework de Java: tampoco `List`, `Map`, `Set`, `PriorityQueue`, `Collections`, `Arrays.asList` o resultados de streams que creen colecciones. Usar arreglos y estructuras propias.
- No delegar el ordenamiento de colas a utilidades o bibliotecas. La lista ordenada será implementación propia y conservará desempates estables.
- Pruebas del núcleo con ejecutables Java y comprobaciones propias, sin añadir una biblioteca externa de pruebas al proyecto entregable.
- Repositorio GitHub, ramas `main`, `develop` y ramas por funcionalidad; Issues y PR con descripción. Historial real del único integrante, sin atribuciones ficticias.

No añadir Gson en este módulo: la dependencia se incorpora con la persistencia JSON. Los plugins Maven son herramientas de construcción, no implementaciones de estructuras o lógica del simulador.

## Review Focus

1. Una estructura vacía o un índice inválido conserva sus enlaces y tamaño: tareas 1–2.
2. Un presupuesto productor/consumidor puede desbordarse al multiplicar intervalo y cuota: tarea 3, cálculo en `long` y rechazo explícito.
3. Cambiar el estado de un proceso sin admisión o volver a ejecutar uno terminado debe fallar sin mutarlo: tarea 4.
4. Un proceso con deadline 1 dispone exactamente de una oportunidad de ejecución; las esperas también consumen plazo: tarea 4.
5. La terminal usa Java 17 aunque Java 25 ya esté instalado; abrir una ventana no demuestra que NetBeans compile con 25: tareas 1 y 5.

## Archivos y contratos compartidos

Todos los caminos siguientes son relativos a la raíz del repositorio.

- Construcción: `pom.xml`, `nbactions.xml`, `.gitignore`, `README.md`.
- Pruebas: `src/test/java/avilaos/tests/TestSupport.java`, `AllTests.java` y una clase de pruebas por tarea.
- Estructuras: `src/main/java/avilaos/structures/{LinkedList,LinkedQueue,RingBuffer}.java`; nodos privados internos.
- Modelo: `src/main/java/avilaos/process/{ProcessState,BlockReason,ProcessType,TerminationReason,InstructionKind,ProcessDefinition,ProcessControlBlock}.java`.
- Enums de hardware/políticas: `src/main/java/avilaos/hardware/CpuMode.java`, `src/main/java/avilaos/scheduling/SchedulingPolicyType.java`.
- Arranque: `src/main/java/avilaos/App.java`, `src/main/java/avilaos/ui/MainWindow.java`.

Se permite `java.util.Comparator` porque es una interfaz de comparación y no una colección. No se permite `java.util.LinkedList`; todas las referencias a nuestra lista deben importar `avilaos.structures.LinkedList`.

Comando `BUILD` de este plan, ejecutado desde la raíz con el JDK explícito:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' test
```

`AllTests.main(String[] args)` llama a `run()` de cada clase de pruebas, imprime `OK: N comprobaciones` si todas pasan y propaga cualquier fallo para terminar con estado distinto de cero. `TestSupport` ofrece `equal(Object expected, Object actual)`, `isTrue(boolean condition)` y `throwsType(Class<? extends Throwable> type, Runnable action)`; cada fallo lanza `AssertionError`. No depende de habilitar `-ea`.

## Task 1: proyecto compilable y lista enlazada propia

**Files:** crear `pom.xml`, `.gitignore`, `README.md`, `src/main/java/avilaos/structures/LinkedList.java` y `src/test/java/avilaos/tests/{TestSupport,AllTests,LinkedListTest}.java`.

**Interfaces:** no consume código anterior. Produce `LinkedList<T>()`, `void addLast(T value)`, `T get(int index)`, `T removeAt(int index)`, `T removeFirst()`, `int size()`, `boolean isEmpty()` y `void sort(Comparator<? super T> order)`.

- [x] **Step 1:** crear la configuración Maven con `maven.compiler.release=25`, codificación UTF-8, `maven-compiler-plugin:3.16.0` y `exec-maven-plugin:3.6.4`. Ejecutar `AllTests` mediante exec en fase `test`, con classpath de pruebas y propagación de excepciones. Configurar el POM para que sobrescribir `exec.mainClass` al ejecutar la aplicación no sustituya el runner de pruebas. Ignorar `target/`, metadatos locales de NetBeans, `.DS_Store` y archivos temporales.
- [x] **Step 2:** escribir `LinkedListTest.run()` con estas comprobaciones: lista nueva vacía; añadir `10,20,30` conserva orden; retirar primero, medio y último devuelve el valor correcto; volver a añadir después de vaciar conserva enlaces; `get(-1)` y `removeAt(size)` lanzan `IndexOutOfBoundsException` sin variar el tamaño; `removeFirst()` vacía lanza `IllegalStateException`; valores `null` se rechazan con `IllegalArgumentException`.
- [x] **Step 3:** añadir el caso de orden estable con objetos `(clave, etiqueta)`: entrada `(2,A),(1,B),(2,C)` y comparador por clave; salida `(1,B),(2,A),(2,C)`. Ordenar una lista vacía o de un elemento no falla. No usar una colección estándar como referencia de la prueba.
- [x] **Step 4:** ejecutar `BUILD`. Debe fallar por ausencia de `LinkedList`; no aceptar un fallo por JDK equivocado o plugin no descargado como evidencia de la prueba.
- [x] **Step 5:** implementar la lista con enlaces propios, tamaño y referencias a cabeza/cola. `sort` usa inserción estable escrita por nosotros. No exponer nodos mutables fuera de la clase.
- [x] **Step 6:** ejecutar `BUILD`; exigir runner satisfactorio y `BUILD SUCCESS`. Revisar imports de este módulo para descartar colecciones prohibidas.
- [x] **Step 7:** registrar `feat: agrega lista enlazada propia y base de pruebas` en la rama funcional del módulo, incluyendo únicamente los archivos de esta tarea.

Fragmento obligatorio de `LinkedListTest.run()`:

```java
LinkedList<Integer> list = new LinkedList<>();
TestSupport.isTrue(list.isEmpty());
TestSupport.throwsType(IllegalStateException.class, list::removeFirst);
list.addLast(10);
list.addLast(20);
TestSupport.equal(10, list.removeFirst());
TestSupport.equal(20, list.get(0));
TestSupport.throwsType(IndexOutOfBoundsException.class, () -> list.removeAt(1));
TestSupport.equal(1, list.size());
```

## Task 2: cola FIFO y buffer circular acotado

**Files:** crear `src/main/java/avilaos/structures/{LinkedQueue,RingBuffer}.java`, `src/test/java/avilaos/tests/{LinkedQueueTest,RingBufferTest}.java`; modificar `AllTests.java` para incluirlas.

**Interfaces:** consume `LinkedList<T>` de tarea 1. Produce `LinkedQueue<T>` con `void enqueue(T)`, `T dequeue()`, `T peek()`, `int size()`, `boolean isEmpty()` y `T get(int index)`; cola vacía devuelve `null` en `dequeue/peek`. Produce `RingBuffer<T>(int capacity)` con `void put(T)`, `T take()`, `T get(int index)`, `int size()`, `int capacity()`, `boolean isEmpty()` y `boolean isFull()`. Ambas estructuras rechazan valores `null` con `IllegalArgumentException` sin mutarse.

- [x] **Step 1:** escribir `LinkedQueueTest.run()` con FIFO `A,B,C`, `peek` sin extracción, retorno `null` en vacío, acceso indexado en orden FIFO y rechazo de `null` sin cambiar tamaño.
- [x] **Step 2:** escribir `RingBufferTest.run()` con capacidad 1; rechazo de capacidad 0/negativa; `put` lleno y `take` vacío lanzan `IllegalStateException`; llenado de capacidad 3, dos extracciones y dos inserciones generan orden `3,4,5` tras envolver el índice; `get(-1)` y `get(size)` fallan sin mutación.
- [x] **Step 3:** ejecutar `BUILD`, esperando fallo por ausencia de las nuevas estructuras.
- [x] **Step 4:** implementar la cola sobre nuestra lista y el buffer con un arreglo interno. `get(0)` del buffer siempre significa el siguiente elemento a consumir, no la posición física cero del arreglo.
- [x] **Step 5:** ejecutar `BUILD`, exigiendo éxito de todas las pruebas anteriores y nuevas.
- [x] **Step 6:** registrar `feat: agrega cola FIFO y buffer circular propios` con los archivos de esta tarea.

Fragmento obligatorio de `RingBufferTest.run()`:

```java
RingBuffer<Integer> buffer = new RingBuffer<>(1);
buffer.put(7);
TestSupport.throwsType(IllegalStateException.class, () -> buffer.put(8));
TestSupport.equal(1, buffer.size());
TestSupport.equal(7, buffer.take());
TestSupport.throwsType(IllegalStateException.class, buffer::take);
TestSupport.throwsType(IllegalArgumentException.class, () -> buffer.put(null));
TestSupport.equal(0, buffer.size());
```

## Task 3: enums y definición validada de procesos

**Files:** crear los enums y `ProcessDefinition.java` de la sección de archivos; crear `src/test/java/avilaos/tests/ProcessDefinitionTest.java` y modificar `AllTests.java`.

**Interfaces:** produce el record `ProcessDefinition(String name, int computerId, ProcessType type, int instructionCount, int memoryRequired, int priority, long deadlineCycles, int cpuBurst, int ioCycles, int bufferId, int operationInterval, int targetItems)`.

Validación: nombre no nulo ni vacío tras `strip`, computador mayor que 0, tipo no nulo, instrucciones/memoria/deadline positivos y prioridad mayor o igual que 0. CPU bound requiere campos específicos en cero. I/O bound requiere `cpuBurst` e `ioCycles` positivos y campos de buffer en cero. Productor/consumidor requiere `bufferId`, intervalo y cuota positivos, campos de E/S en cero y `instructionCount >= (operationInterval + 5L) * targetItems`. Todas las entradas inválidas lanzan `IllegalArgumentException`.

No validar aquí existencia del computador/buffer ni RAM total: esas referencias las valida el coordinador al conocer el escenario completo. `bufferId=0` representa ausencia de buffer en los otros tipos.

Enums exactos: `ProcessState={NEW,READY,RUNNING,BLOCKED,TERMINATED}`; `BlockReason={NONE,IO,NETWORK,SEMAPHORE_EMPTY,SEMAPHORE_FULL,SEMAPHORE_MUTEX}`; `ProcessType={CPU_BOUND,IO_BOUND,PRODUCER,CONSUMER}`; `TerminationReason={COMPLETED,DEADLINE_EXPIRED}`; `CpuMode={USER,KERNEL,IDLE}`; `SchedulingPolicyType={FCFS,EDF,ROUND_ROBIN,PREEMPTIVE_PRIORITY}`; `InstructionKind={COMPUTE,IO_REQUEST,SEM_WAIT,SEM_SIGNAL,BUFFER_INSERT,BUFFER_REMOVE}`.

- [x] **Step 1:** escribir `ProcessDefinitionTest.run()` creando un caso válido de cada tipo y comprobando que conserva los atributos y normaliza el nombre.
- [x] **Step 2:** comprobar el rechazo individual de nombre vacío, computador 0, tipo nulo, instrucciones 0, memoria negativa, prioridad negativa, deadline 0, ráfaga/duración de E/S cero, buffer/intervalo/cuota cero y campos incompatibles con el tipo.
- [x] **Step 3:** comprobar productor con intervalo 2 y cuota 3: 21 instrucciones se acepta, 20 se rechaza. Comprobar intervalo/cuota `Integer.MAX_VALUE` con presupuesto `Integer.MAX_VALUE`: se rechaza en lugar de aceptar un producto desbordado.
- [x] **Step 4:** ejecutar `BUILD`, esperando fallo por ausencia del modelo; implementar record y enums; volver a ejecutar `BUILD` hasta obtener éxito de las pruebas completas.
- [x] **Step 5:** registrar `feat: define tipos y validacion de procesos` con los archivos de esta tarea.

Fragmento obligatorio de `ProcessDefinitionTest.run()`:

```java
ProcessDefinition producer = new ProcessDefinition(" P1 ", 1, ProcessType.PRODUCER,
        21, 8, 0, 100, 0, 0, 1, 2, 3);
TestSupport.equal("P1", producer.name());
TestSupport.throwsType(IllegalArgumentException.class, () ->
        new ProcessDefinition("P1", 1, ProcessType.PRODUCER,
                20, 8, 0, 100, 0, 0, 1, 2, 3));
```

## Task 4: PCB con transiciones, contexto y deadlines

**Files:** crear `src/main/java/avilaos/process/ProcessControlBlock.java`, `src/test/java/avilaos/tests/ProcessControlBlockTest.java`; modificar `AllTests.java`.

**Interfaces:** consume `ProcessDefinition` y enums. Produce:

- `ProcessControlBlock(long id, ProcessDefinition definition, long arrivalCycle)`: ID positivo, llegada no negativa; `deadlineAt = Math.addExact(arrivalCycle, definition.deadlineCycles())`, desbordamiento convertido a `IllegalArgumentException`.
- Lecturas: `id()`, `definition()`, `state()`, `blockReason()`, `pc()`, `mar()`, `remainingInstructions()`, `assignedMemory()`, `arrivalCycle()`, `deadlineAt()`, `firstRunCycle()`, `terminationCycle()`, `terminationReason()`, `userCycles()`, `kernelCycles()` y `deadlineRemaining(long boundaryCycle)`.
- `void admit()`: `NEW -> READY`, reserva lógica de memoria declarada. El administrador de RAM realizará la reserva real antes de invocarlo en el siguiente módulo.
- `void dispatch(long cycle)`: `READY -> RUNNING`; registra primera ejecución solo una vez. Requiere `arrivalCycle <= cycle < deadlineAt`.
- `void executeInstruction(CpuMode mode)`: exige RUNNING e instrucciones restantes; incrementa PC/MAR y contador del modo, resta una instrucción. `IDLE` y modo nulo se rechazan.
- `void preempt()`: `RUNNING -> READY` sin perder contexto ni RAM.
- `void block(BlockReason reason)` y `void unblock()`: `RUNNING -> BLOCKED -> READY`, sin perder contexto ni RAM; `NONE` no es motivo válido para bloquear.
- `void terminate(TerminationReason reason, long cycle)`: deja TERMINATED, memoria asignada 0 y motivo de bloqueo NONE. COMPLETED exige cero instrucciones restantes y `arrivalCycle <= cycle < deadlineAt`; DEADLINE_EXPIRED exige `cycle >= deadlineAt-1`. Registrar el ciclo ejecutado al cuyo cierre se finaliza. No permite finalizar dos veces ni modificar un terminado.

Toda operación de transición inválida lanza `IllegalStateException` sin cambiar atributos; argumentos inválidos, enums nulos y ciclos negativos lanzan `IllegalArgumentException`. `firstRunCycle/terminationCycle=-1` hasta que ocurra el evento y `terminationReason=null` hasta terminar. `deadlineRemaining(b)=max(0,deadlineAt-b)`; rechaza `b<0` y no muta el PCB.

- [x] **Step 1:** escribir `ProcessControlBlockTest.run()` para proceso CPU de dos instrucciones, memoria 8, deadline 10 y llegada 0: estado inicial NEW, memoria asignada 0, PC/MAR 0, restante 2; admitir, despachar, ejecutar USER, apropiar, volver a despachar y ejecutar KERNEL; comprobar PC/MAR 2, restante 0, primera ejecución conservada y contadores 1/1; terminar normalmente en ciclo 1 libera memoria.
- [x] **Step 2:** probar RUNNING -> BLOCKED(IO) -> READY; preservar memoria y PC, limpiar motivo al despertar. Probar vencimiento desde NEW, READY, RUNNING y BLOCKED con PCB independiente para cada caso.
- [x] **Step 3:** probar rechazo sin mutación al despachar NEW, ejecutar READY, bloquear con NONE, desbloquear READY, completar con trabajo pendiente o ejecutar/terminar de nuevo un terminado. Rechazar ID 0, llegada negativa y desbordamiento de deadline absoluto.
- [x] **Step 4:** comprobar llegada 5 y deadline relativo 1: deadlineAt 6, restante en límite 5 igual a 1 y en límite 6 igual a 0; una instrucción completada en ciclo 5 se acepta. Un proceso sin ejecutar vence al cierre del ciclo 5 y no puede despacharse en 6. El deadline de un bloqueado también disminuye al consultar límites posteriores.
- [x] **Step 5:** ejecutar `BUILD`, esperando fallo por ausencia del PCB; implementar los contratos; volver a ejecutar `BUILD` y exigir éxito completo.
- [x] **Step 6:** registrar `feat: implementa PCB y transiciones de estado` con los archivos de esta tarea.

Fragmento obligatorio de `ProcessControlBlockTest.run()`:

```java
ProcessDefinition definition = new ProcessDefinition("P1", 1, ProcessType.CPU_BOUND,
        1, 8, 0, 1, 0, 0, 0, 0, 0);
ProcessControlBlock pcb = new ProcessControlBlock(1, definition, 5);
TestSupport.equal(1L, pcb.deadlineRemaining(5));
pcb.admit();
pcb.dispatch(5);
pcb.executeInstruction(CpuMode.USER);
pcb.terminate(TerminationReason.COMPLETED, 5);
TestSupport.equal(ProcessState.TERMINATED, pcb.state());
TestSupport.equal(0, pcb.assignedMemory());
TestSupport.equal(0L, pcb.deadlineRemaining(6));
TestSupport.throwsType(IllegalStateException.class, () -> pcb.executeInstruction(CpuMode.USER));
```

## Task 5: arranque en Swing y comprobación en NetBeans

**Files:** crear `src/main/java/avilaos/{App.java,ui/MainWindow.java}`, `nbactions.xml`; modificar `pom.xml` y `README.md`.

**Interfaces:** `App.main(String[] args)` programa la apertura con `SwingUtilities.invokeLater`; `MainWindow extends JFrame` ofrece constructor sin argumentos y no depende del núcleo futuro. Ventana titulada «ÁvilaOS», texto «Simulador de sistemas operativos distribuidos» y cierre normal. No presenta métricas ficticias ni botones de simulación aún inexistentes.

- [x] **Step 1:** ejecutar la acción Maven de arranque con `exec.mainClass=avilaos.App`; debe fallar por clase inexistente antes de crear App. Conservar ese diagnóstico, distinto de los fallos del núcleo.
- [x] **Step 2:** implementar ventana y arranque sobre el hilo de eventos de Swing. Configurar las acciones Run y Debug de NetBeans para `avilaos.App`, manteniendo independiente el runner ligado a la fase test.
- [x] **Step 3:** actualizar README con capacidades reales del módulo, comandos de pruebas/arranque y pasos para abrir esta misma carpeta como proyecto Maven en NetBeans. Documentar selección de JDK 25 en propiedades del proyecto y cómo comprobarlo en la salida de construcción. No cambiar el Java global del estudiante.
- [x] **Step 4:** ejecutar `BUILD` y `mvn package` con el JDK explícito de este plan; exigir pruebas satisfactorias y JAR generado. No afirmar que el JAR es autónomo por compilar: el arranque acordado es NetBeans/Maven hasta configurar distribución en otro módulo.
- [ ] **Step 5:** abrir el proyecto en NetBeans, seleccionar plataforma 25, ejecutar Clean and Build y Run. Comprobar visualmente título/texto y cierre; conservar evidencia de ejecución. Si no es posible controlar la ventana desde las herramientas disponibles, pedir al estudiante que ejecute esas acciones y registre el resultado; la verificación GUI permanece pendiente hasta observarlo.
- [x] **Step 6:** revisar imports y uso de colecciones en fuentes/pruebas; confirmar ausencia de dependencias de simulación en Swing y que solo existen cambios del módulo. Registrar `feat: agrega arranque Swing compatible con NetBeans`.

## Cierre del módulo y continuación

- [x] Registrar comandos, versiones y resultados observados en `docs/verification/2026-10-19-foundation.md`, usando la fecha real de ejecución si cambia.
- [ ] Vincular el Issue y abrir PR de la rama funcional hacia `develop` cuando GitHub esté configurado. Sin remoto/autenticación, conservar commits locales y registrar la integración como pendiente, sin simular un PR.
- [x] Explicar al estudiante: diferencia entre definición y PCB, por qué las colas son propias, cuándo avanza PC/MAR y cómo se mide el deadline.
- [ ] Marcar el módulo 1 completo solo cuando pasen pruebas y ejecución en NetBeans; entonces diseñar el plan del módulo 2 contra estas interfaces ya implementadas.

Estado: tareas 1–4 completas y registradas en commits; tarea 5 implementada y verificada por construcción, pendiente de comprobación visual desde NetBeans. Ejecución nativa aprobada por el estudiante. Evidencia: [registro de verificación](../../verification/2026-10-19-foundation.md).

Referencias de construcción verificadas al preparar el plan: [Compiler Plugin](https://maven.apache.org/plugins/maven-compiler-plugin/usage.html) y [Exec Plugin](https://www.mojohaus.org/exec-maven-plugin/usage.html).
