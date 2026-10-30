# Módulo 3 — Políticas de planificación

## Qué hicimos

Completamos las cuatro políticas y permitimos elegirlas por computador, cambiar de política y ajustar el quantum entre ciclos. Los procesos conservan su contexto y RAM cuando pierden la CPU.

## Conceptos esenciales

| Política | Selección | Cuándo desplaza al actual |
| --- | --- | --- |
| FCFS | Orden de entrada a listos | Nunca por decisión del planificador |
| EDF | Menor deadline absoluto | Si hay un listo con deadline estrictamente menor |
| Round Robin (RR) | Turnos FIFO | Al consumir el quantum |
| Prioridades apropiativas | Menor número = mayor prioridad | Si hay un listo con número estrictamente menor |

**Apropiación:** el planificador retira la CPU al proceso y lo devuelve a READY; continúa luego desde su PC. **Bloqueo:** el proceso espera un evento y pasa a BLOCKED. La [guía del módulo 2](modulo-2-computador.md) explica estados, RAM y E/S.

**Quantum:** máximo de instrucciones por turno en RR. Tanto USER como KERNEL consumen turno. Un quantum pequeño alterna más a menudo; uno grande mantiene más tiempo al mismo proceso. Nuestro simulador no añade un costo de cambio de contexto.

**Empate:** se conserva el orden de entrada a listos; igual prioridad/deadline no desplaza al actual. **Deadline absoluto:** llegada + plazo relativo; EDF compara ese límite, no solo el plazo inicial.

## Dónde está en el código

- `SchedulingPolicy` define el contrato; `FcfsPolicy`, `EdfPolicy`, `RoundRobinPolicy` y `PreemptivePriorityPolicy` lo implementan.
- `SchedulingPolicies` crea la implementación elegida mediante el enum.
- `OrderedProcessQueue` conserva el orden de entrada aunque se ordene por otra clave; permite recuperar FIFO al cambiar a FCFS/RR.
- `Scheduler` delega las decisiones; `Kernel` realiza las transiciones, cuenta el turno y aplica cambios pendientes.
- `Computer` permite elegir política/quantum iniciales y solicitar una nueva configuración.

## Ejemplo para explicarlo

A necesita 3 instrucciones y B necesita 2; ambos están listos desde el comienzo y tienen plazo suficiente. Con RR y quantum 2:

| Ciclo | Ejecuta | Qué sucede |
| --- | --- | --- |
| 0 | A | Consume 1 del turno |
| 1 | A | Consume 2; agota el turno |
| 2 | B | A vuelve al final de listos; B consume 1 |
| 3 | B | Completa sus instrucciones |
| 4 | A | Continúa y termina |

La secuencia es A,A,B,B,A. Con quantum 1 es A,B,A,B,A. La apropiación ocurre al comienzo del siguiente ciclo; no ejecuta una instrucción adicional.

## Tres preguntas probables

- **¿EDF y prioridades son lo mismo?** Usan claves distintas: tiempo límite absoluto y número de prioridad. Ambos solo apropian ante una clave estrictamente mejor.
- **¿Qué pasa al cambiar de política?** La solicitud queda pendiente hasta el siguiente ciclo válido; se reinicia el turno, se reordenan listos y la nueva política evalúa al proceso actual. El quantum debe ser positivo. Cambiar también el quantum inicia un turno nuevo.
- **¿Y si una E/S coincide con el fin del quantum?** Se bloquea normalmente, conserva RAM y espera su E/S. No se añade simultáneamente a listos. Al terminar o vencer se liberan sus recursos.

## Cómo lo comprobamos y qué puedes decir

Las pruebas verifican las secuencias anteriores, apropiación por llegada/desbloqueo, empates, cambios de política, rechazo sin mutaciones e independencia entre computadores. Pasaron 916 comprobaciones y la revisión independiente no encontró defectos; la evidencia está en [verificación](../verification/2026-10-20-scheduling.md).

Se prueba mediante el motor Java; la GUI conserva su ventana inicial. Los hilos y reloj del clúster pertenecen al módulo siguiente.

**Explicación breve:** «Las políticas deciden quién debe ejecutar, pero el núcleo conserva el control de los estados. EDF y prioridades desplazan solo ante un proceso mejor; RR reparte turnos. Cambiar la política conserva el avance del proceso y se aplica entre ciclos».
