# Módulo 6 — Interfaz y observación del simulador

## Qué hicimos

Sustituimos la ventana inicial por configuración, formularios y cuatro vistas: computadores, buffers/semáforos, escenario inicial y registro. Los controles permiten iniciar, pausar, avanzar un ciclo y reiniciar. Se pueden agregar procesos/buffers y cambiar política, quantum o duración durante la ejecución.

El motor conserva sus decisiones y la interfaz muestra sus valores; no decide qué proceso ejecuta. La aceptación automatizada pasó; la inspección visual real está pendiente mientras el Mac permanece bloqueado.

## Conceptos esenciales

| Concepto | Qué debes entender |
| --- | --- |
| EDT | Hilo de eventos de Swing. Actualiza componentes y atiende botones; no espera el cierre de los hilos del motor. |
| Sesión | Dueña de una ejecución. Su Thread serializa solicitudes y crea/cierra/reconstruye Simulation; conserva el motor hasta confirmar el cierre, aunque tarde. |
| Comando pendiente | Encolar no significa aplicar. El motor confirma o rechaza al avanzar el ciclo. |
| Snapshot | Copia inmutable que la pantalla observa; no recibe PCB/colas mutables. |
| Generación | Identifica un motor reconstruido. Evita mostrar una publicación antigua después de reiniciar. |
| Escenario inicial / vivo | Reiniciar reconstruye la carga y los parámetros iniciales. Las altas y cambios hechos durante ejecución no se incorporan a ellos. |

Una pausa se confirma al terminar el ciclo en curso. El modo de la última instrucción y el proceso actualmente en CPU son datos distintos: terminar puede liberar la CPU en ese mismo ciclo.

## Dónde está en el código

- `SimulationConfig` valida y copia la configuración inicial; será la base de JSON en el módulo 7.
- `EventLog` recibe decisiones de Kernel/Simulation y conserva los eventos entre refrescos de pantalla; los copia recorriendo una sola vez la lista propia.
- `UiSession` administra el motor y sus hilos fuera del EDT; `SessionView` publica el contexto de una generación.
- `UiController` usa un Timer Swing, descarta generaciones antiguas y entrega acciones tipadas.
- `MainWindow` y `DashboardPanel` ensamblan formularios/paneles; los modelos propios de JTable conservan el orden del núcleo.
- El detalle de PCB sigue el ID al cambiar de cola o CPU, sin confundir la identidad con el índice de una fila. El refresco respeta la pestaña elegida.

## Ejemplo para explicarlo

Al pulsar **Paso**, Swing envía una solicitud a la sesión y sigue atendiendo eventos. La sesión pide un paso al motor. El reloj aplica comandos y avanza los computadores; publica el resultado completo. El siguiente refresco muestra ese ciclo, las colas y los eventos. Ningún botón incrementa el PC por su cuenta.

## Tres preguntas probables

- **¿Por qué no ejecutar la simulación dentro del botón?** Bloquearía el EDT y congelaría la ventana; además mezclaría interfaz con planificación. El botón solo envía una solicitud.
- **¿Cómo evitan perder eventos si la pantalla refresca más despacio?** El motor conserva el registro. La ventana puede saltar muestras visuales, pero recibe los eventos intermedios en la siguiente instantánea.
- **¿Qué hace Reiniciar?** Cierra el motor anterior fuera del EDT y reconstruye el escenario inicial desde ciclo cero. Cambia la generación, limpia avisos del motor anterior y no reutiliza sus PCB.

**Explicación breve:** «La interfaz observa copias y envía órdenes. Un hilo de sesión maneja la ejecución y el cierre; Swing permanece disponible. Los cambios se confirman entre ciclos y reiniciar reconstruye el escenario inicial, sin mezclar datos de dos ejecuciones».

## Prueba manual en NetBeans

La [evidencia técnica](../verification/2026-10-23-interface.md) distingue pruebas automatizadas, inspección visual y validación del IDE. La suite actual espera **24770 comprobaciones**; no son casos independientes, pues incluye invariantes repetidas.

1. **Clean and Build:** buscar `OK: 24770 comprobaciones` y `BUILD SUCCESS`.
2. **Run:** comprobar las cuatro pestañas y que la ventana permite navegar/editar sin congelarse. No debe avanzar sola al abrir.
3. Antes de iniciar, agregar buffer **B1**, anfitrión **1**, capacidad **1**. Agregar primero un **consumidor C** y luego un **productor P**, ambos en computador **1**, instrucciones **6**, RAM **2**, prioridad **0**, deadline **100**, buffer **1**, cómputo por elemento **1** y cuota **1**.
4. Pulsar **Paso** doce veces. Cada click añade un ciclo. Al final: ciclo **12**, dos terminados, producidos **1**, consumidos **1**, ocupación **0**, vacíos **1**, llenos **0**, mutex **1**. En el primer paso el consumidor se bloquea en llenos; al despertar no repite wait.
5. Probar **Iniciar → Pausar**: el ciclo queda estable al confirmarse la pausa. Seleccionar un PCB y comprobar que su identidad se conserva al cambiar de cola. Revisar eventos con ciclo/ID en Registro y un error de formulario antes de enviarlo.
6. **Reiniciar:** vuelve a ciclo **0**, conserva la carga inicial y deja sus altas pendientes para el primer ciclo. Cerrar la ventana debe terminar la ejecución sin dejar el proceso Maven esperando.

El escenario remoto y los casos de políticas/deadline también están cubiertos automáticamente. La prueba interactiva completa, incluido el remoto y el tamaño mínimo de ventana, sigue pendiente del desbloqueo del Mac.
