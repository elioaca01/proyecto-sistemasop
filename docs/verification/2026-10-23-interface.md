# Verificación del módulo 6 — 23 de octubre de 2026

Estado: código de interfaz y aceptación automatizada implementados. Revisión independiente realizada y sus tres hallazgos importantes corregidos. Inspección de la ventana real parcialmente realizada; faltan navegación/edición y escenario local/remoto interactivo. No se declara concluida la aceptación visual.

## Entorno y construcción observada

macOS arm64, Temurin 25.0.4.1 y Maven 3.9.16 incluido en NetBeans. Rama `feat/swing-interface`, base `71715d4`, misma carpeta NetBeans autorizada. No se añadió biblioteca ni cambió el Java global.

```sh
/usr/bin/env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -o -B -Dmaven.repo.local=.maven-cache clean package
```

Resultado observado después de corregir la revisión: **OK: 24770 comprobaciones**, BUILD SUCCESS y JAR `target/avilaos-1.0-SNAPSHOT.jar`. El total incluye las comprobaciones previas y assertions de invariantes repetidas por ciclo, no 24770 casos independientes. Las pruebas del proyecto se ejecutan en AllTests desde `exec (core-tests)`; el bloque Surefire con cero tests no es el resultado de esta suite.

## Progreso y evidencia por tarea

| Tarea | Total acumulado | Commit |
| --- | --- | --- |
| Escenario inicial | 24489 | `e7844cf` |
| Decisiones del motor | 24516 | `ef75c3e` |
| Sesión y cierre asíncrono | 24577 | `52c33c6` |
| Modelos propios de tablas | 24602 | `3781d16` |
| Formularios | 24628 | `a5709c6` |
| Computador y PCB | 24648 | `ebf57cd` |
| Buffers/semáforos/órdenes | 24664 | `62e1554` |
| Ventana y controlador | 24676 | `99642a3` |
| Integración de controles | 24753 | `82c90a6` |

Cinco JVM independientes, con límite de quince segundos por ejecución, repitieron las mismas **24753 comprobaciones** y finalizaron normalmente.

Las tareas 1–8 comenzaron con pruebas fallidas por API/funcionalidad ausente y terminaron con la suite satisfactoria. La integración de tarea 9 pasó inicialmente: no se inventó un RED. Las pruebas headless instancian los paneles y botones de producción, con UiSession/Simulation y workers reales; no simulan el motor mediante mocks.

Al introducir Swing, una ejecución de AWT nativo dentro del sandbox terminó en el bloque de pruebas sin marcadores OK/BUILD. Las mismas clases en una JVM headless y Maven con headless explícito pasaron 24602. AllTests ahora selecciona headless antes de inicializar Swing, conforme al contrato de pruebas sin ventanas. La aplicación normal se abre en otra JVM, con interfaz gráfica; no se cambió ese arranque.

EventPanelTest reprodujo un defecto de asociación: después de reiniciar, los avisos anteriores podían figurar como órdenes pendientes del motor nuevo, cuyos IDs vuelven a empezar. Se eliminan los avisos al instalar una generación y se publican las solicitudes iniciales reconstruidas. La regresión comprueba que quede pendiente solo la carga inicial correspondiente, sin las altas de la ejecución anterior; pasó junto con la suite de 24664.

## Comportamientos automatizados comprobados

- **Configuración:** copia defensiva; mínimo dos máquinas; IDs/referencias válidos; reservas de buffers sumadas en long; no se exige que todos los procesos quepan a la vez. Un intento inválido conserva el escenario vigente.
- **Eventos:** copia estable y escritores concurrentes; altas/rechazos, admisión, selección, apropiación, cambio de política, IO/red/semáforos, desbloqueo, inserción/extracción, terminación y limpieza. Se conservan decisiones cuando transcurren ciclos sin leer la pantalla.
- **Sesión:** preparación pausada en 0 sin ticks ocultos; FIFO de acciones; distinción inicial/vivo; cierre durante preparación/reinicio; cinco reconstrucciones; IDs y políticas iniciales restaurados; ningún hilo propio restante en la JVM de pruebas tras cerrar las sesiones.
- **Presentación:** tablas propias de solo lectura; orden del núcleo; selección por ID al reordenar o pasar a CPU/terminados; PCB con atributos iniciales/contexto/operación; deadline restante de un terminado se muestra al finalizar, no consumido por ciclos posteriores.
- **Formularios:** tipos CPU, IO, productor y consumidor; parámetros ajenos al tipo puestos en cero; vacío, fracciones, negativos y overflow numérico rechazados. La carga inicial puede editarse antes de empezar y los borradores no se sobrescriben al llegar otro refresco.
- **Buffers:** tokens FIFO/origen, anfitrión, capacidad/ocupación, dueño, tres valores y colas, fases y reservas. Integer.MAX_VALUE con ocupación cero no crea un componente por posición ni preasigna ese contenido en Java.
- **Controlador:** botones reales envían solicitudes; Paso concede un ciclo, Iniciar/Pausar cambia los controles según el estado observado; latido EDT mientras se solicita reinicio; una SessionView de generación vieja no sustituye el nuevo ciclo 0. Cierre del motor fuera del EDT y confirmación asíncrona.
- **Traza local desde controles:** consumidor primero, capacidad 1, intervalo/cuota 1: wait(llenos) en 0; productor ejecuta sus seis instrucciones en 1..6; consumidor continúa sus cinco restantes en 7..11. Final ciclo 12, dos terminados, producidos/consumidos 1/1, ocupación 0, vacíos/llenos/mutex 1/0/1.
- **Remoto L=2 desde controles:** productor calcula en 0, permanece con PC 1 y motivo Red en 1/2; ejecuta las cinco instrucciones de núcleo en 3..7. La tabla mantiene el PC durante la red.
- **Cuatro políticas:** carga inicial A/B/C, memoria suficiente y prioridades/deadlines distintos. FCFS/RR seleccionan A primero; EDF/prioridades seleccionan B y muestran C/A en listos. RR quantum 1 rota A→B conservando el contexto inspeccionado.
- **Deadline en semáforo:** consumidor espera en llenos y vence con PC 1; sale de bloqueados, aparece en terminados, conserva su selección por ID y se retira de la espera del buffer.

## Inspección visual e IDE

El primer intento de arranque nativo dentro del sandbox terminó con exit 134; el arranque autorizado fuera del sandbox permaneció abierto. La primera inspección no pudo continuar con el Mac bloqueado. El estudiante confirmó después el desbloqueo.

Al reanudar, la herramienta no pudo enlazar las aplicaciones NetBeans/Java por su identidad compartida `net.java.openjdk.java`. Se creó con el jpackage oficial del JDK una copia temporal de validación en `/private/tmp/avilaos-native-validation-20261023/AvilaOSValidation.app`, identidad exclusiva `edu.avilaos.validation`, sin modificar el proyecto ni añadir dependencias. Se abrió con la herramienta de UI. El JAR empaquetado coincide exactamente con el del proyecto: SHA-256 `c62616690bc515aeff00b4e7f172466af70204beddcfe747cc419a161f6f0450`.

**Observado en la ventana real:** tamaño inicial 1180×780 lógico, ampliación mediante control nativo, cuatro etiquetas de pestañas, formularios modales de buffer/proceso, estado inicial pausado en ciclo 0. Se agregaron un buffer de capacidad 1 y un proceso CPU con los valores predeterminados; Paso mostró ciclo 1, PC/MAR 1 y RAM usada 5 (buffer 1 + proceso 4). Inspeccionar CPU mostró el PCB completo. Iniciar avanzó a ejecución; Pausar confirmó ciclo 7 y permaneció estable durante la lectura/scroll siguiente. Paso mostró exactamente ciclo 8 y PC/MAR 8. El scroll vertical del PCB cambió su posición.

[Evidencia de ventana y PCB](images/module6-native-pcb.png).

**Límite de la herramienta:** AX setValue rechaza los campos Java como no editables por accesibilidad; intentos de clic/teclado/pegado no modificaron sus valores. La pestaña expone un tipo AX desconocido y setValue devuelve `unknownType(Optional(179))`; los intentos de navegación tampoco produjeron cambios. El estudiante confirmó que Configuración inicial se abre manualmente; la herramienta observó esa vista. Después de esa activación humana fue posible editar latencia 2→0, quitar el proceso inicial y aplicar el escenario. Reiniciar previamente volvió de ciclo 8 a 0 y habilitó la edición inicial. Al abrir un nuevo diálogo la entrada de teclado volvió a carecer de foco: se canceló esa carga incompleta; no se atribuye a un fallo del producto.

El botón nativo de cierre hizo desaparecer la ventana; al comprobar su PID 31078 con ps ya no existía el proceso, por lo que no retuvo sus hilos Java. La lectura AX posterior agotó su espera porque la aplicación había terminado. No se infiere de ello una salida de Maven: esta validación se lanzó como aplicación temporal.

Quedan sin acreditar: mínimo 960×640, las otras pestañas mediante interacción nativa, edición completa de diálogos, selección de filas durante cambios y traza productor-consumidor local/remota introducida manualmente. Las pruebas headless cubren la lógica de estas trazas, pero no sustituyen esa aceptación.

Nueva ejecución de Maven tras reanudar: **24770 comprobaciones, BUILD SUCCESS**.

El estudiante aún no aportó evidencia de construcción del módulo 5 desde su IDE. La última evidencia de NetBeans disponible es el módulo 4: 1246 comprobaciones, JAR/install y BUILD SUCCESS el 22 de octubre a las 19:54:55 de Venezuela. La prueba de este módulo 6 desde NetBeans también está pendiente; la [guía](../defensa/modulo-6-interfaz.md) conserva los pasos.

## Revisión final y continuidad

La revisión independiente del rango `71715d4..9f935d8`, con compilación propia en `/private/tmp`, pasó las 24753 comprobaciones anteriores y encontró tres problemas importantes (ninguno crítico ni menor adicional). Se corrigieron en un único pase, sin otro revisor:

1. **Cierre lento:** la reproducción original dejaba FAILED tras agotar dos segundos y luego CLOSING permanente. Una regresión retiene mediante un semáforo la publicación final más de dos segundos: primero falló `Esperado: CLOSING; recibido: FAILED`. La sesión ahora conserva el motor y espera por intervalos fuera del EDT, también durante reemplazo; publica CLOSED solo tras confirmar su salida. La prueba comprueba el latido EDT, un segundo pedido de cierre y el callback final.
2. **Registro creciente:** `LinkedList.get(i)` dentro de cada copia volvía a recorrer la cabeza. Se añadió `copyInto` secuencial O(n) a la estructura propia, usado por eventos, resultados y avisos de sesión. La prueba del nuevo recorrido con 80000 elementos falló primero por API ausente y luego pasó preservando orden e independencia del arreglo. La reproducción independiente, repetida tras el cambio, midió aproximadamente 0.09/0.19/0.29/0.25 ms para 10k/20k/40k/80k eventos frente a 92/384/1168/4951 ms anteriores. Son mediciones puntuales, no un límite garantizado.
3. **Navegación:** la regresión selecciona un PCB, abre Bloqueados y refresca; primero falló `Esperado: 2; recibido: 1`. El detalle y la fila siguen el ID, pero el refresco conserva la pestaña elegida. Inspeccionar CPU limpia la selección previa para que la fila resaltada no contradiga el detalle.

Resultado tras las tres correcciones: **24770 comprobaciones**, `clean package` y JAR satisfactorios. No hay hallazgos menores diferidos. La aceptación nativa continúa pendiente: el segundo intento de acceso a la UI también indicó Mac bloqueado. La validación nativa posterior se hizo con el JAR corregido mediante la copia temporal documentada arriba; la ventana anterior de Maven no se usó como prueba de esas correcciones.

git diff --check y búsqueda de colecciones/sorters prohibidos no informaron incidencias. Los tests usan EDT, barreras/esperas acotadas y cierre en finally; no Thread.sleep para sincronizar.

El trabajo permanece en la rama funcional y la carpeta compartida. Sin remoto no se inventó PR ni fusión. Guardar/cargar JSON y métricas/gráfico conservan sus módulos 7 y 8; aún son necesarios para completar el proyecto total.
