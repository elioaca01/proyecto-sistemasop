# Módulo 2: un computador — guía para la defensa

## Qué construimos

Un computador simulado capaz de ejecutar procesos CPU bound e I/O bound con memoria limitada, planificación FCFS y deadlines. El programa avanza cuando recibe un ciclo lógico: no depende de la velocidad real del Mac.

`Computer` reúne CPU, RAM, controlador de E/S y núcleo. Cada instancia posee sus componentes y colas. Podemos crear dos computadores independientes, pero el reloj coordinador y los hilos reales se incorporarán en el módulo 4. La ventana sigue siendo la inicial; estas capacidades se ejercitan en las pruebas Java.

## Clases y responsabilidades

| Clase | Qué hace | Por qué está separada |
| --- | --- | --- |
| `Ram` | Cuenta capacidad, memoria usada y libre; entrega reservas que solo puede liberar su RAM de origen y una sola vez | Evita liberar memoria inexistente o ajena |
| `MemoryAdmission` | Reserva RAM antes de pasar NEW → READY; relaciona cada PCB con su reserva | La admisión depende de memoria, no del orden de uso de CPU |
| `OrderedProcessQueue` | Cola de listos sobre nuestra lista enlazada; evita IDs duplicados y puede ordenarse de forma estable | Permite otras políticas sin colecciones de Java |
| `SchedulingPolicy` | Define selección, reordenamiento y decisión de apropiación | Es el contrato común de los planificadores |
| `FcfsPolicy` | Selecciona la cabeza de listos y nunca apropia | Implementa FCFS sin mezclarlo con hardware |
| `Scheduler` | Delega la selección en la política inyectada | Usa polimorfismo; una prueba inyecta otra política para demostrarlo |
| `LinearProgram` | Determina la próxima instrucción a partir de tipo, ráfaga y PC | Calcular la instrucción no modifica el proceso |
| `Cpu` | Ejecuta una instrucción por ciclo y registra modo, instrucción y proceso ejecutado | No decide bloqueos, admisión ni terminación |
| `IoRequest` | Guarda el proceso y el ciclo de finalización de su solicitud | Es un ticket inmutable de E/S |
| `IoController` | Avanza las solicitudes, entrega completadas y permite cancelarlas | La E/S progresa mientras otro proceso usa CPU |
| `Kernel` | Coordina el ciclo, las colas, los estados, los deadlines y la liberación de RAM | Centraliza las transiciones para mantener coherencia |
| `Tickable` | Interfaz `onTick(cycle)` común | Permite avanzar componentes con el mismo concepto de ciclo |
| `Computer` | Ensambla una unidad reutilizable con FCFS | Evita variables globales compartidas entre máquinas |

Los paquetes están en `src/main/java/avilaos/`. `ProcessDefinition` y `ProcessControlBlock` provienen del módulo 1.

## Conceptos que debes dominar

**Programa y proceso.** Un programa describe instrucciones; un proceso es una instancia en ejecución con identidad, contexto y recursos. Aquí `ProcessDefinition` conserva los parámetros inmutables y el PCB conserva el estado cambiante.

**PCB.** Process Control Block: registro con ID, estado, PC/MAR, llegada, deadline, memoria asignada y contadores. Cuando se bloquea, conserva el avance: al volver continúa, no empieza de nuevo.

**Estados.** NEW espera admisión; READY tiene RAM y espera CPU; RUNNING está asignado a CPU; BLOCKED espera un evento, como E/S; TERMINATED terminó normalmente o venció. Un bloqueado no puede ejecutar hasta que se desbloquee.

**CPU bound e I/O bound.** El primero concentra trabajo en CPU. El segundo alterna cómputo y solicitudes de E/S. En nuestro modelo lineal, una ráfaga de 1 genera COMPUTE, IO_REQUEST, COMPUTE, IO_REQUEST… hasta consumir el presupuesto total. La solicitud también consume una instrucción y un ciclo de CPU.

**FCFS / FIFO.** First Come, First Served selecciona según el orden de entrada a listos. No retira por la fuerza al que ejecuta: lo conserva hasta terminar o bloquearse. Si vuelve de E/S, entra al final. Puede producir efecto convoy: procesos cortos esperan detrás de uno largo. No hay quantum en FCFS.

**Admisión y planificación.** La admisión pregunta «¿cabe en RAM?». La planificación pregunta «¿qué listo usa CPU?». Un proceso grande que aún no cabe no impide admitir otro pequeño. FCFS se aplica a listos, no a toda la cola NEW.

**Memoria simulada.** La RAM se representa como unidades de capacidad, sin paginación, particiones ni direcciones físicas. Todo admitido conserva su reserva durante READY, RUNNING y BLOCKED; se libera al terminar. El contador del PCB es informativo: la reserva real la controla `MemoryAdmission`.

**Bloqueo frente a espera en listos.** Un listo podría ejecutar si le dieran CPU. Un bloqueado necesita primero que ocurra un evento. La E/S no consume instrucciones de CPU durante la espera; esa CPU puede atender a otro proceso.

**Modo usuario y modo núcleo.** COMPUTE cuenta como USER; IO_REQUEST cuenta como KERNEL. Son modos de la CPU simulada; nuestro programa Java no cambia de nivel de privilegio real. Al no ejecutar una instrucción, el modo es IDLE.

**PC y MAR.** El PC registra cuántas instrucciones se han consumido y determina la siguiente del programa lineal. El MAR se incrementa también por instrucción en este modelo simplificado. En hardware real, MAR contiene una dirección de memoria y no tiene por qué coincidir con el PC.

**Ciclo lógico.** Es una unidad discreta de simulación, empezando en 0. Cada componente acepta exactamente el siguiente ciclo. Repetir, retroceder o saltar ciclos se rechaza antes de modificarlo. Un ciclo no equivale todavía a un segundo real ni a una instrucción de máquina del Mac.

**Deadline.** Límite absoluto = llegada + plazo relativo. Llegada 0 y plazo 4 permite los ciclos 0,1,2,3; si al cierre del 3 sigue vivo, vence. Esperar CPU, RAM o E/S no detiene el plazo. Completar cómputo en el último ciclo permitido cuenta como éxito antes de revisar vencimientos.

**Interfaz y polimorfismo.** `Scheduler` conoce `SchedulingPolicy`, no una implementación concreta. Hoy `Computer` ensambla FCFS; las otras políticas y el soporte de apropiación del núcleo se desarrollan en el siguiente módulo. La existencia de `shouldPreempt` en la interfaz no significa que ya haya RR o prioridades funcionando.

## Qué ocurre en un ciclo

1. Avanza E/S y recoge completadas. Si no quedan instrucciones, termina el proceso; si quedan, pasa BLOCKED → READY.
2. Recorre todos los NEW e intenta reservar memoria; los que caben pasan a listos.
3. Si CPU está libre, selecciona un listo y hace READY → RUNNING.
4. CPU ejecuta como máximo una instrucción.
5. Una IO_REQUEST lo pasa a BLOCKED y libera la CPU. Una última COMPUTE lo termina.
6. Revisa deadlines de todos los vivos, incluidos NEW y BLOCKED. Terminar cancela E/S y libera únicamente la reserva propia.
7. Reintenta admisión con la memoria liberada; los recién admitidos esperan al próximo ciclo para ejecutar.

Si la RAM se libera por una E/S completada al comienzo, sí puede aprovecharse para ejecutar otro proceso en ese mismo ciclo. Si se libera al cierre, no se concede una segunda instrucción.

## Ejemplo que puedes explicar sin memorizar código

RAM de 10 unidades. A: I/O bound, 3 instrucciones, 8 de RAM, ráfaga 1 y E/S de 2 ciclos. B: CPU bound, 2 instrucciones y 2 de RAM. Ambos llegan en 0 con plazo 10.

| Ciclo | CPU | A al cierre | B al cierre | RAM usada |
| --- | --- | --- | --- | --- |
| 0 | A: COMPUTE, USER | RUNNING, PC=1 | READY, PC=0 | 10 |
| 1 | A: IO_REQUEST, KERNEL | BLOCKED, PC=2 | READY, PC=0 | 10 |
| 2 | B: COMPUTE, USER | BLOCKED, PC=2 | RUNNING, PC=1 | 10 |
| 3 | B: COMPUTE, USER | BLOCKED, PC=2 | TERMINATED, PC=2 | 8 |
| 4 | A: COMPUTE, USER | TERMINATED, PC=3 | TERMINATED | 0 |

Convención explícita: E/S emitida en t con duración d espera t+1…t+d y completa al inicio de t+d+1. Por eso la emitida en 1, duración 2, completa en 4. Las E/S tienen temporizadores independientes; aún no modelamos una cola exclusiva de un dispositivo físico.

Si A tuviera solo 2 instrucciones, IO_REQUEST sería la última. Quedaría BLOCKED con restante 0 hasta el inicio del 4; no ejecutaría una instrucción extra. Con plazo 4, vencería al cierre del 3, se cancelaría su E/S y no reaparecería en 4.

## Preguntas probables

- **¿Por qué un proceso bloqueado sigue ocupando RAM?** Porque conserva su contexto y recursos para continuar; no implementamos swapping.
- **¿Cómo evitan una doble liberación?** RAM valida la identidad de origen y la vigencia de la reserva; admisión guarda la relación con el mismo objeto PCB y la retira una sola vez.
- **¿Qué pasa si vence antes de entrar en RAM?** Sale de NEW y pasa a terminados, pero no libera nada porque nunca obtuvo reserva.
- **¿Por qué la CPU no cambia el proceso a BLOCKED?** El hardware comunica qué ejecutó; el núcleo coordina la solicitud, las colas y la transición.
- **¿Cómo sabes que FCFS se respeta?** Las pruebas verifican A,A,B para procesos de 2 y 1 instrucciones, y la reinserción al final después de un bloqueo.
- **¿Ya hay paralelismo real?** No en este módulo. Hay avance lógico de CPU y E/S; los hilos y semáforos Java corresponden al reloj del clúster.
- **¿Cómo se comprueba?** `mvn test` ejecuta `AllTests`; `ComputerTest` reproduce estas trazas y comprueba estados, RAM, PC, tiempos y cancelaciones. `clean package` recompila y genera el JAR.

## Repaso breve del módulo 1

Construimos lista enlazada, cola FIFO, buffer circular, enums, definición validada, PCB y ventana Swing inicial. Debes conocer nodo/enlace, cabeza/cola, FIFO, capacidad acotada, ordenamiento estable, enum, encapsulación e inmutabilidad. Las estructuras son propias porque el enunciado prohíbe las colecciones estándar. Swing se inicia en su hilo de eventos; todavía no presenta los estados del motor.

Una explicación de 30 segundos: «Primero modelamos los procesos y estructuras. En el módulo 2 armamos un computador con CPU, RAM, E/S y núcleo. El núcleo avanza por ciclos, admite según memoria, selecciona por FCFS, ejecuta una instrucción y atiende bloqueos y deadlines. Las pruebas comprueban el orden y que todos los recursos se liberen correctamente».
