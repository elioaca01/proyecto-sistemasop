# Módulo 1 — Base y procesos

## Qué hicimos

Creamos el proyecto Java/Maven para NetBeans, la ventana inicial Swing, las estructuras propias y el modelo de procesos. Esta base permite que el núcleo de los módulos siguientes gestione procesos sin usar colecciones estándar de Java.

## Conceptos esenciales

| Concepto | Qué debes entender |
| --- | --- |
| Lista enlazada | Guarda elementos en nodos conectados; permite insertar y retirar procesos. |
| Cola FIFO | El primero que entra es el primero que sale. |
| Buffer circular | Usa un arreglo de capacidad fija y reutiliza posiciones al avanzar sus índices. |
| Ordenamiento estable | Los elementos con la misma prioridad conservan su orden previo. |
| Enum | Representa opciones válidas, como READY o BLOCKED, evitando estados escritos como texto libre. |
| PCB | Es el registro del proceso: conserva identidad, estado, avance y recursos asignados. |
| Inmutabilidad | Los parámetros iniciales se conservan en ProcessDefinition; el estado cambiante queda en ProcessControlBlock. |

Los estados son: **NEW** espera admisión; **READY** espera CPU; **RUNNING** ejecuta; **BLOCKED** espera un evento; **TERMINATED** terminó.

## Dónde está en el código

- `LinkedList`, `LinkedQueue` y `RingBuffer`: almacenamiento propio.
- `ProcessDefinition`: valida los datos iniciales del proceso.
- `ProcessControlBlock`: controla transiciones, PC/MAR, contadores y deadline.
- `App` y `MainWindow`: arrancan la ventana en el hilo de eventos de Swing.

El PC registra las instrucciones consumidas. En este modelo, MAR también avanza por instrucción; en hardware real contiene una dirección de memoria y no tiene por qué coincidir con el PC.

## Ejemplo para explicarlo

Un proceso ejecuta dos instrucciones y se bloquea: conserva PC=2. Al volver a ejecutarse, continúa desde ese avance. Bloquearlo no equivale a terminarlo ni a reiniciarlo.

## Tres preguntas probables

- **¿Por qué estructuras propias?** Porque el enunciado prohíbe usar las colecciones estándar para estos datos.
- **¿Por qué separar definición y PCB?** Una contiene la configuración inicial; el otro representa cómo cambia el proceso.
- **¿Qué evita una transición inválida?** El PCB comprueba el estado anterior y rechaza la operación si no corresponde.

## Cómo lo comprobamos y qué puedes decir

Las pruebas verificaron 212 comprobaciones de estructuras, validaciones y PCB. La captura del estudiante confirmó la ventana desde NetBeans. La ejecución automática de CPU y memoria se añadió en el módulo 2.

**Explicación breve:** «Construimos las estructuras y el registro de cada proceso. El PCB conserva su contexto y valida sus cambios de estado. Esa base permite ejecutar, bloquear y continuar procesos de forma coherente».
