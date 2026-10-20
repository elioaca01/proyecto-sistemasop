# Módulo 2 — Un computador

## Qué hicimos

Integramos CPU, RAM limitada, núcleo, planificación FCFS y E/S. El computador avanza por ciclos lógicos y termina los procesos al completar su trabajo o vencer su deadline.

## Conceptos esenciales

| Concepto | Qué debes entender |
| --- | --- |
| Admisión | Reserva RAM antes de pasar NEW → READY. |
| Planificación | Elige cuál de los procesos listos usa CPU. |
| FCFS | Atiende por entrada a listos, sin retirar por la fuerza al que ejecuta; lo conserva hasta terminar o bloquearse. |
| CPU bound / I/O bound | El primero concentra cómputo; el segundo alterna cómputo y solicitudes de E/S. |
| Bloqueo | El proceso conserva contexto y RAM mientras espera; otro puede usar CPU. |
| Ciclo lógico | Una unidad discreta: cada CPU ejecuta como máximo una instrucción por ciclo. |
| Deadline | Llegada + plazo relativo. La espera en RAM, CPU o E/S también consume plazo. |

COMPUTE cuenta en modo USER; IO_REQUEST en KERNEL; sin instrucción, IDLE. Son modos simulados. La RAM se representa en unidades de capacidad, sin paginación ni direcciones físicas.

## Dónde está en el código

- `Computer` ensambla componentes independientes por máquina; `Tickable` define su avance por ciclos.
- `Ram` entrega reservas; `MemoryAdmission` las relaciona con su PCB y las libera al terminar.
- `OrderedProcessQueue`, `SchedulingPolicy`, `FcfsPolicy` y `Scheduler` organizan y seleccionan listos mediante una interfaz intercambiable.
- `LinearProgram` determina la instrucción; `Cpu` la ejecuta.
- `IoRequest` guarda el vencimiento de la E/S; `IoController` entrega completadas o las cancela.
- `Kernel` coordina las colas, transiciones y deadlines.

Cada ciclo: completa E/S → admite → despacha si CPU libre → ejecuta una instrucción → bloquea o termina → revisa deadlines → readmite con la RAM liberada. La readmisión al cierre espera al próximo ciclo para ejecutar.

## Ejemplo para explicarlo

A tiene 3 instrucciones, ráfaga 1 y E/S de duración 2. B tiene 2 instrucciones de CPU. Ambos caben en RAM y tienen plazo suficiente.

| Ciclo | CPU | Situación de A |
| --- | --- | --- |
| 0 | A: cómputo | PC=1 |
| 1 | A: solicitud de E/S | BLOCKED, PC=2 |
| 2 | B: cómputo | Sigue esperando |
| 3 | B: cómputo y termina | Sigue esperando |
| 4 | A: cómputo y termina | PC=3 |

La E/S emitida en t espera t+1…t+d y completa al inicio de t+d+1. A conserva RAM mientras espera y vuelve al final de listos.

## Tres preguntas probables

- **¿Y si la última instrucción solicita E/S?** Queda bloqueado con cero instrucciones pendientes hasta que complete la operación. Si vence antes, se cancela la E/S.
- **¿Cómo evitan liberar RAM ajena?** Cada reserva pertenece a su RAM y PCB; solo se libera una vez. Un NEW sin reserva no libera memoria.
- **¿Qué pasa con un deadline de 1 al llegar en 0?** Puede ejecutar en 0. Si completa, termina normalmente; si sigue vivo, vence al cierre de ese ciclo.

## Cómo lo comprobamos y qué puedes decir

Pasaron 509 comprobaciones acumuladas y una construcción limpia. Se verificaron FCFS, espera por RAM, E/S, deadlines y dos computadores independientes. La revisión independiente no encontró defectos. Los hilos, otras políticas y GUI completa corresponden a módulos posteriores; la ventana sigue siendo la inicial.

**Explicación breve:** «El núcleo admite según memoria, selecciona por FCFS y ejecuta una instrucción por ciclo. Durante una E/S, otro proceso aprovecha la CPU. Al terminar o vencer se cancelan las esperas y se libera la reserva propia».
