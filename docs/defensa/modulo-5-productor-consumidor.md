# Módulo 5 — Productor, consumidor y red

## Qué hicimos

El motor ya ejecuta productores y consumidores que comparten buffers locales o remotos. Cada buffer reserva RAM del anfitrión, conserva tokens FIFO y tiene tres semáforos simulados. Añadimos latencia por elemento, bloqueo sin detener los hilos Java y limpieza al vencer el deadline. La GUI completa sigue para el módulo 6.

## Conceptos esenciales

| Concepto | Qué debes entender |
| --- | --- |
| Buffer acotado | Cola con capacidad fija. Una posición reserva una unidad de RAM del anfitrión. |
| Productor / consumidor | Uno inserta tokens; el otro extrae el más antiguo. Cada token identifica su productor y computador. |
| Vacíos / llenos | Permisos para reservar un espacio o un elemento. Si no hay permiso, el proceso espera en una cola FIFO. |
| Mutex / sección crítica | Un solo dueño puede modificar el contenido. Apropiar al dueño conserva su reserva. |
| Concesión directa | signal entrega el permiso al primero que espera. Continúa en el ciclo siguiente sin repetir wait. |
| Red simulada | Una espera por elemento remoto antes de adquirir permisos. Con L=2, iniciar en 1 permite retomar en 3. |
| Limpieza por deadline | Devuelve permisos sin usar, libera mutex y completa señales pendientes; conserva inserciones/extracciones ya realizadas. |

Un semáforo **Java** protege datos entre hilos reales. Un semáforo **simulado** guarda PCB bloqueados: no duerme el worker ni congela el reloj. Un wait bloqueante sí cuenta como instrucción; la espera posterior no avanza PC/MAR.

## Dónde está en el código

- `BufferProgram` determina cómputo y las cinco instrucciones de núcleo por elemento; `ProcessProgram` selecciona el programa según el tipo.
- `BoundedBuffer`, `BufferRegistry` y `SimulatedSemaphore` mantienen contenido, RAM, permisos y colas propias.
- `BufferService` conserva las reservas por proceso, coordina red/despertares y limpia terminaciones.
- `Kernel` aplica las transiciones; `Simulation` entrega avisos antes de avanzar los computadores.
- `AddBufferCommand` crea buffers entre ciclos; los snapshots publican valores sin exponer el estado mutable.

## Ejemplo para explicarlo

Buffer local de capacidad 1, intervalo 1 y cuota 1; el consumidor llega primero:

| Ciclo | Trabajo |
| --- | --- |
| 0 | Consumidor ejecuta wait(llenos) y se bloquea. |
| 1–6 | Productor: cómputo, wait(vacíos), wait(mutex), insertar, signal(mutex), signal(llenos). |
| 7–11 | Consumidor: wait(mutex), extraer, signal(mutex), signal(vacíos), cómputo. |

El consumidor no repite el wait de 0. Al final hay 0 elementos, vacíos=1, llenos=0 y mutex=1. La RAM de los procesos se libera; la posición del buffer sigue reservada.

## Tres preguntas probables

- **¿Por qué vacíos + llenos puede ser menor que la capacidad?** Hay permisos reservados o modificaciones pendientes de señal. También debemos contar esas reservas, no solo los contadores libres.
- **¿Qué pasa si vence después de insertar?** Conservamos el token y el contador de producción. Liberamos el mutex si todavía es suyo y señalamos llenos si faltaba. No añadimos instrucciones al proceso vencido.
- **¿Se paga red en cada wait y signal?** No: una vez por elemento, antes de los cinco pasos. No retiene permisos durante esa espera; acceder al buffer anfitrión no paga red.

## Cómo lo comprobamos y qué puedes decir

La suite acumulada pasa **24401 comprobaciones**. Incluye trazas, cuatro políticas, apropiación entre fases, FIFO, RAM, copias retenidas y deadlines. Son comprobaciones de escenarios e invariantes, no 24401 casos independientes. Consulta la [evidencia](../verification/2026-10-23-buffers.md).

**Explicación breve:** «Cada operación reserva un espacio o un elemento, toma el mutex, modifica el buffer y libera los permisos correspondientes. Si debe esperar, guardamos su contexto y otra CPU puede trabajar. La red se espera antes de reservar; un deadline limpia los recursos sin deshacer trabajo confirmado».
