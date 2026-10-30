# Casos de prueba para confirmar ÁvilaOS

Estos son los casos que debes ejecutar tú en la ventana real de NetBeans. Las casillas quedan sin marcar: preparar escenarios y comprobarlos por API no demuestra que ya los ejecutaste en el IDE. Conserva una captura o una nota del resultado de cada caso.

## Preparación y orden

Usa JDK25. Abre la carpeta que contiene pom.xml y ejecuta Clean and Build. Espera **OK: 25550 comprobaciones** y **BUILD SUCCESS**; el bloque anterior de Surefire puede mostrar cero. Después ejecuta Run Project.

Para cargar un caso, abre **Configuración inicial → Cargar escenario JSON** y elige el archivo indicado dentro de examples/. Espera a que termine la carga: debe quedar pausado en ciclo0. No pulses Iniciar cuando debas contar pasos. Cada pulsación de Paso aumenta el ciclo global en uno; espera a que se refleje antes de volver a pulsarlo. Los índices internos empiezan en0: la última ejecución tras el primer Paso puede decir «Último ciclo0» aunque el encabezado diga «Ciclo1».

Carga de nuevo el archivo antes de cada caso independiente. Para localizar PC/MAR usa **Computadores → computador → Inspeccionar CPU / última ejecución**, o selecciona el proceso en una cola. Los comandos en ejecución pueden mostrar «pendiente del próximo ciclo»: pulsa Paso para confirmar su aplicación.

Para una primera pasada, haz **01, 03, 04, 06, 12 y 14**. Después completa el resto. Si falla algo, anota: caso, archivo, ciclo, pasos exactos, resultado esperado y observado; no marques el caso como aprobado.

## 01. Construcción y arranque

- [ ] Clean and Build termina con25550 comprobaciones y BUILD SUCCESS; Run abre ÁvilaOS sin excepciones.
- [ ] Están las pestañas Computadores, Buffers y semáforos, Configuración inicial, Registro y Métricas.

**Comprueba:** empaquetado, dependencias, acciones de NetBeans y arranque.

## 02. Pausa, paso, velocidad y reinicio

Carga `manual/politicas-fcfs.json`. Da dos pasos: encabezado Ciclo2. Sin tocar nada durante unos segundos, el ciclo debe permanecer en2. Pulsa Iniciar / continuar, después Pausar y espera a que el estado diga Pausada. A partir de ese momento el ciclo debe estabilizarse; un ciclo que ya estaba en curso puede terminar al solicitar pausa.

Cambia Ciclo(ms) a100 y pulsa Aplicar duración; la duración aplicada debe cambiar, conservando el avance lógico. Pulsa Reiniciar: vuelve al escenario inicial, ciclo0, sin el historial anterior. La duración inicial del archivo es500ms.

- [ ] Paso avanza uno; pausa estabiliza; duración cambia el ritmo; reinicio borra la ejecución y las métricas anteriores.

## 03. Productor-consumidor local y métricas

Carga `local-producer-consumer.json`. Da exactamente **12 pasos**. En Métricas → CPU y procesos y en Buffers y semáforos, compara:

| Dato | Esperado |
| --- | --- |
| Completados / vencidos / pendientes | 2 / 0 / 0 |
| CPU global / computador1 / computador2 | 50% / 100% / 0% |
| Servicio usuario / núcleo | 2 / 10 instrucciones |
| Throughput | 2/12 =0.167 procesos/ciclo |
| Respuesta media / cumplimiento / Jain | 0.500 / 100% / 1 |
| B1 producidos / consumidos / ocupación | 1 / 1 / 0 |
| Vacíos / llenos / mutex libres | 1 / 0 / 1 |
| Espera cerrada de semáforo | 1 episodio, 6 ciclos; promedio6.000 |

En Métricas → Buffers y esperas debe verse ese promedio. El gráfico muestra una curva por computador y12 muestras; incluye las instrucciones de usuario y de núcleo. La RAM del anfitrión conserva la reserva del buffer aun cuando ambos procesos terminan: buffer vacío no significa buffer eliminado.

- [ ] Coinciden estos valores y ambos procesos aparecen terminados por Completado.

## 04. Productor remoto y espera de red

Carga `remote-producer.json`. Selecciona computador2 e inspecciona el productor después de cada uno de **8 pasos**. El PC debe seguir **1,1,1,2,3,4,5,6**. PC/MAR no avanzan por el mero paso de tiempo de red.

Al final: completado1, vencidos0, producido1, consumido0, ocupación1/1; CPUglobal37.5%, computador1 sin servicio y computador2 con75%. No hay episodios de espera de semáforo: mostrar Sin datos en su promedio es correcto. El buffer está lleno; la operación remota ya terminó.

- [ ] Coinciden la traza PC, el contenido del buffer y las métricas; no se confunde red con espera de semáforo.

## 05. Las cuatro políticas y E/S en una ventana

Carga `io-and-scheduling.json` y avanza paso a paso, cambiando el selector de computador:

- Computador1, FCFS: FCFS A recibe CPU antes de FCFS B; A ejecuta sus6 instrucciones antes de que B ejecute las suyas.
- Computador2, EDF: EDF corto se ejecuta antes del proceso E/S. Después, E/S alterna cómputo, solicitud de E/S y bloqueo. Mientras está bloqueado, PC se conserva y el reloj continúa. Al completar E/S regresa y conserva su contexto.
- Computador3, Round Robin con quantum1: RR A y RR B alternan mientras ambos están listos.
- Computador4, prioridades: Prioridad alta (0) se ejecuta antes de Prioridad baja (4).

Da hasta30 pasos: los ocho procesos deben estar completados, sin deadlines vencidos. Si sigues dando pasos una vez terminados, el tiempo aumenta y la utilización acumulada baja: es tiempo inactivo, no pérdida de instrucciones.

- [ ] Se observan FCFS, EDF, RR, prioridades, bloqueo E/S y reanudación conservando PC/MAR.

## 06. Comparación exacta de políticas

Carga por separado los cuatro archivos siguientes, siempre desde ciclo0, y da **12 pasos** en cada uno. Son las mismas cargas y condiciones del informe; solo cambia la política.

| Archivo en examples/manual/ | Completados | Vencidos | CPU global | Respuesta media |
| --- | --- | --- | --- | --- |
| politicas-fcfs.json | 2 | 2 | 58.33% | 0.000 |
| politicas-edf.json | 3 | 1 | 62.50% | 2.000 |
| politicas-rr.json | 1 | 3 | 62.50% | 1.250 |
| politicas-prioridades.json | 3 | 1 | 62.50% | 2.000 |

Todos tienen pendientes0. En FCFS recibieron CPU solo2 de los4 procesos; respuesta0 no significa mejor atención de toda la carga. EDF y prioridades coinciden en esta carga; eso no los hace universalmente superiores.

- [ ] Coinciden las cuatro filas. Consulta el informe para throughput, cumplimiento y Jain derivados de la misma medición.

## 07. Cambiar política sin borrar contexto

Carga `io-and-scheduling.json`. Da2 pasos; en computador1, FCFS A tiene PC2. Cambia a Round Robin, quantum1, y pulsa Aplicar política/quantum. Da el siguiente Paso.

La política aplicada debe pasar a Round Robin. El contexto de A no vuelve a0: según quién reciba la CPU, permanece en2 o sigue avanzando. La RAM no se libera por la apropiación. Sigue hasta completar A y B: sus PC finales deben ser6 y3.

- [ ] Cambio aplicado entre ciclos, sin reiniciar el proceso ni perder su reserva de RAM.

## 08. Admisión por RAM y liberación

Carga `manual/admision-ram.json`: computador1 tiene RAM8; A y B piden8 cada uno, con3 y2 instrucciones, respectivamente. Observa al final de cada Paso:

| Paso | Nuevos | Terminados | RAM usada de computador1 |
| --- | --- | --- | --- |
| 1 | 1 | 0 | 8 |
| 2 | 1 | 0 | 8 |
| 3 | 0 | 1 | 8 |
| 4 | 0 | 1 | 8 |
| 5 | 0 | 2 | 0 |

En el paso3 A libera RAM y B puede admitirse en ese mismo ciclo; por eso la RAM sigue en8 aunque ya haya un terminado. No es una fuga. B empieza a ejecutar después. Al paso5 ambos completaron, sin vencidos.

- [ ] B espera en Nuevos mientras no cabe y la RAM acaba libre.

## 09. Deadline con proceso en CPU

Carga `manual/deadline-cpu.json`: CPU de10 instrucciones con plazo2. Da2 pasos. Debe terminar por **Deadline vencido**, con PC2, completados0, vencidos1, pendientes0 y RAM del proceso liberada. Un proceso vencido no cuenta como completado normalmente.

- [ ] El deadline termina el proceso antes de ejecutar sus10 instrucciones.

## 10. Deadline mientras espera un semáforo

Carga `manual/deadline-bloqueado.json`: consumidor sin productor, buffer vacío y plazo2. Después del primer Paso se observa el bloqueo por Llenos y el reloj sigue avanzando. Después del segundo: Deadline vencido, sin procesos pendientes; B1 sigue vacío, vacíos/llenos/mutex1/0/1 y sin PCB esperando.

Las métricas tienen1 episodio cerrado de espera,1 ciclo y promedio1.000. La RAM final del anfitrión conserva solo la unidad del buffer. Esto comprueba limpieza de la espera sin inventar un elemento ni liberar el buffer existente.

- [ ] Vence aunque esté bloqueado y desaparece de la cola del semáforo.

## 11. Agregar procesos/buffer y validaciones

Carga `manual/vacio.json`. Agrega un proceso CPU bound al computador1: nombre Nuevo,4 instrucciones, RAM2, prioridad0, deadline100. Confirma y usa Paso cuando la operación lo requiera. Debe aparecer con ID global, avanzar PC y terminar normalmente tras ejecutar4 instrucciones.

Agrega un buffer al computador1, capacidad1; espera a que aparezca confirmado. Crea un productor asociado a ese buffer:6 instrucciones, RAM2, prioridad0, deadline100, cómputo por elemento1 y objetivo1. Al ejecutar, debe producir un elemento; el contenido se muestra en el buffer y la RAM anfitriona incluye su reserva.

Prueba estos errores **de uno en uno**, corrigiendo el campo antes del siguiente: nombre vacío; instrucciones0; RAM-1; prioridad-1; deadline0; letras o1.5 en un entero; quantum0; duración0; cantidad de computadores1 en Configuración inicial. Debe aparecer un mensaje y no aceptarse el valor inválido. Cancela los formularios de prueba para no añadir cargas accidentales. Un rechazo no debe destruir la simulación anterior.

- [ ] Altas válidas aparecen sin duplicar IDs; entradas inválidas se rechazan sin cerrar la aplicación.

## 12. Guardar, recargar, extensión y sobrescritura

Carga `local-producer-consumer.json` y da3 pasos. En Configuración inicial, guarda como `prueba-local` en una carpeta de pruebas: debe crearse `prueba-local.json`. Guarda otra vez sobre ese archivo; debe pedir confirmación de sobrescritura. Primero cancela y comprueba que no cambia el archivo; después repite y confirma sobre esa copia de prueba.

Carga el guardado: debe volver a **ciclo0 y Pausada**, no al ciclo3. El archivo guarda el escenario inicial aplicado, no un checkpoint de la ejecución. Da12 pasos y comprueba el caso03 de nuevo. Cambiar un campo en edición sin Aplicar configuración no lo incluye automáticamente en lo guardado.

- [ ] Extensión, cancelación, sobrescritura y recarga funcionan; el escenario guardado reproduce los resultados.

## 13. JSON inválido y cancelación del selector

Haz una **copia** de prueba-local.json, borra su última llave y guárdala como corrupto.json. Carga el local válido y da3 pasos. Intenta cargar corrupto.json: debe mostrar error, conservar ciclo3 y los procesos/buffers anteriores. Otro Paso debe llevar a ciclo4. Abre el selector y pulsa Cancelar: también conserva la ejecución.

No modifiques los ejemplos originales. Opcional: en otra copia pon formatVersion99 o un computador inexistente; ambos deben rechazarse por validación.

- [ ] Archivo corrupto, versión inválida y cancelación no sustituyen ni destruyen el motor válido.

## 14. Tablas, PCB y gráfico

Con el caso03, desplaza horizontalmente las tablas para leer deadline, estado y métricas. Cambia entre computadores, buffers, Registro y las dos subpestañas de Métricas. Selecciona un proceso terminado: deben verse contexto, tipo y razón de terminación. Redimensiona la ventana hacia su tamaño mínimo y vuelve a ampliarla.

- [ ] Las tablas permiten desplazamiento, los controles siguen accesibles y las curvas se corresponden con sus leyendas.
- [ ] Al Reiniciar se limpian selección y gráfico anteriores; no quedan curvas de la generación previa.

## 15. Debug y cierre

En NetBeans, coloca un breakpoint dentro de Kernel.onTick y ejecuta Debug Project. Carga un escenario y pulsa Paso: debe detenerse allí; reanuda con Continue y comprueba que acaba el ciclo. Quita el breakpoint antes de las demás pruebas.

Con Run Project, pulsa Iniciar y cierra la ventana con el botón de cierre. Comprueba que Maven termina y que no queda la ejecución activa. Ocultar o minimizar la ventana no cuenta como cerrar.

- [ ] El depurador detiene/reanuda y el cierre termina la ejecución correctamente.

## 16. Copia limpia y entrega

Descarga main o descomprime el ZIP final en una carpeta nueva. Abre su pom.xml como proyecto en NetBeans, selecciona JDK25 y ejecuta Clean and Build y Run. Puede descargar dependencias la primera vez. Repite03 y04 desde esa copia.

- [ ]25550 comprobaciones, BUILD SUCCESS, arranque y casos local/remoto correctos desde la copia limpia.
- [ ] Los correctores pueden acceder al repositorio privado y al informe. El estudiante completa el envío académico/spreadsheet y practica la defensa.

## Registro de resultados

| Caso | Aprobado / falló / no ejecutado | Ciclo y observación | Captura o evidencia |
| --- | --- | --- | --- |
| 01–16 | Completar una fila por caso | | |

Los ocho JSON de examples/manual/ se validaron con JsonConfigurationStore y se ejecutaron contra el motor real: RAM hasta paso5, deadlines hasta paso2, políticas hasta paso12 y vacío hasta paso1. Coincidieron los resultados descritos. Esa verificación por API es preparación de estos casos; las casillas de ventana real corresponden al estudiante.
