# Módulo 8 — Métricas y gráfico de utilización

## Qué hicimos

Agregamos la pestaña **Métricas**, con resultados del sistema y de cada computador, contadores por buffer y un gráfico con una serie por máquina. El motor conserva una muestra por ciclo confirmado; la pantalla puede saltar refrescos sin perder muestras.

Reiniciar o cargar otro escenario comienza en cero. Cambiar los milisegundos modifica la velocidad de visualización; las métricas usan ciclos lógicos. Un fallo a mitad de ciclo conserva las últimas métricas confirmadas.

## Conceptos y fórmulas

| Métrica | Fórmula y población |
| --- | --- |
| Throughput | Completados normalmente / ciclos transcurridos. Unidad: procesos/ciclo. |
| Utilización | Instrucciones ejecutadas / ciclos disponibles de CPU. Global: dividir por computadores × ciclos; incluir usuario y núcleo, mostrar ambos aparte. |
| Respuesta promedio | Suma de primera ejecución − llegada / procesos que ya recibieron CPU. No exige que hayan terminado. |
| Cumplimiento | Completados / (completados + vencidos). Los pendientes se muestran aparte. |
| Equidad de Jain | `(suma de servicio)² / (admitidos × suma de servicio²)`. Servicio = usuario + núcleo. Incluir admitidos sin servicio y terminados; excluir quienes vencieron todavía NEW. |
| Espera de semáforo | Ciclos de episodios cerrados / episodios cerrados, incluidos los cerrados por deadline. Los abiertos y su tiempo aparecen aparte; excluir IO y red. |

**Sin datos** significa que falta el denominador. Cero ciclos no implica CPU al 0 %. Después de un ciclo sin trabajo sí hay utilización 0 %. Jain no garantiza ausencia de inanición: mide cómo se repartió el servicio de esa carga.

La instrucción wait ocupa CPU; la espera comienza en el límite posterior. Si un permiso se concede durante el ciclo k, el proceso puede volver en k+1. Un episodio se cierra una sola vez, también si el deadline cancela el despertar.

## Un cálculo manual

Carga `examples/local-producer-consumer.json` y avanza doce pasos. C y P ejecutan seis instrucciones cada uno en computador 1; computador 2 permanece inactivo.

- Throughput global: `2/12 = 0.167` procesos/ciclo.
- CPU global: `12/(2×12) = 50 %`; computador 1: `100 %`; computador 2: `0 %`.
- Servicio: usuario 2, núcleo 10; global usuario `8.33 %`, núcleo `41.67 %`.
- Respuesta: C comienza en 0 y P en 1; promedio `(0+1)/2 = 0.5` ciclos.
- Cumplimiento: `2/(2+0) = 100 %`. Jain: `12²/(2×(6²+6²)) = 1`.
- B1: producido/consumido 1/1, ocupación 0; una espera cerrada de 6 ciclos, promedio 6; ninguna abierta.

## Dónde está en el código

`PerformanceMetrics` y `MetricsCalculator` contienen contadores y fórmulas. `everAdmitted` conserva la población de equidad después de liberar RAM. `BufferService` registra los episodios y `SemaphoreWaitMetrics` separa abiertos/cerrados. `Simulation` confirma `MetricsSnapshot` e historial después de todos los workers. `MetricsPanel` muestra tablas propias y `UtilizationChart` dibuja con Java2D; no calcula servicio observando el Timer.

## Tres preguntas probables

- **¿Por qué no promediar el throughput o las respuestas de cada máquina?** Las poblaciones pueden ser distintas. Sumamos contadores base y recalculamos con el denominador global.
- **¿Un consumidor bloqueado consume CPU durante toda la espera?** Solo al ejecutar sus instrucciones, incluido wait. Durante el bloqueo no recibe servicio; el reloj y su deadline siguen avanzando.
- **¿Por qué el gráfico conserva puntos si la GUI refresca más despacio?** Las muestras pertenecen al coordinador y se guardan una por ciclo completo. Swing dibuja ese historial inmutable.

**Explicación breve:** «Medimos el trabajo y las transiciones del motor en ciclos lógicos. Cada fórmula tiene una población y un denominador explícitos. Agregamos contadores para calcular el sistema y guardamos el historial por ciclo, sin depender de la velocidad de la pantalla».

## Prueba manual en NetBeans

1. **Clean and Build:** buscar `OK: 25454 comprobaciones` y `BUILD SUCCESS`.
2. **Run → Configuración inicial → Cargar escenario:** elegir el ejemplo local, ir a Métricas y comprobar **Sin datos** antes del primer ciclo.
3. Avanzar doce pasos y comparar con el cálculo anterior. Desplazar horizontalmente la tabla para ver respuesta, deadlines y Jain. En **Buffers y esperas**, comprobar B1 y el promedio 6.
4. En ventana pequeña, desplazar el gráfico o arrastrar el divisor entre gráfico/tablas para ver los ejes. La leyenda identifica cada computador; el eje X usa ciclos globales completados y el eje Y utilización acumulada en porcentaje.
5. **Reiniciar** debe vaciar el gráfico y volver a Sin datos. Cargar `remote-producer.json` y dar ocho pasos: CPU global **37.50 %**, usuario 1/núcleo 5, un completado y espera de semáforo **Sin datos** (el bloqueo fue de red).
6. Cerrar por la ventana debe terminar el proceso. La aceptación interactiva y Run/Debug en NetBeans siguen pendientes; los [renders y las pruebas automatizadas](../verification/2026-10-25-metrics.md) no sustituyen ese paso.
