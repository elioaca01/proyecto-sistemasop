# Verificación del módulo 7 — 23 de octubre de 2026

Estado: implementación y aceptación automatizada terminadas. Aceptación interactiva del selector pendiente: el Mac se bloqueó durante la navegación. No se declara completa la aceptación nativa ni la comprobación en NetBeans. Revisión independiente realizada; un hallazgo importante corregido con regresión.

## Entorno y construcción

macOS arm64, Temurin 25.0.4.1 y Maven 3.9.16 incluido en NetBeans. Rama `codex/scenario-json`, base `235abc1`, misma carpeta del IDE. Única dependencia de aplicación incorporada: `com.google.code.gson:gson:2.14.0`, para JsonReader/JsonWriter en modo STRICT. No se usa DOM/reflexión ni colecciones Gson para almacenar los objetos del simulador.

```sh
/usr/bin/env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -o -B -Dmaven.repo.local=.maven-cache clean package
```

Resultado final observado: **OK: 25234 comprobaciones**, JAR y **BUILD SUCCESS**, código 0. Incluye las 24775 comprobaciones previas; el total son assertions, incluidas invariantes repetidas, no casos independientes. Surefire muestra cero; el ejecutor efectivo es `exec (core-tests)` / AllTests.

La descarga inicial de Maven Central falló por DNS dentro del sandbox y después se completó con ejecución autorizada. Las construcciones posteriores pasan offline. [Versión oficial de Gson](https://github.com/google/gson/releases/tag/gson-parent-2.14.0), [modo estricto](https://google.github.io/gson/Troubleshooting.html).

## Evidencia por tarea

| Tarea | Total acumulado | Commit |
| --- | --- | --- |
| Codec JSON estricto | 24886 | `0791589` |
| Archivos UTF-8 y reemplazo | 24947 | `4bafb8a` |
| Sesión asíncrona | 25058 | `2aa1fb8` |
| Controles y selector | 25098 | `f1aab65` |
| Archivos de ejemplo e integración | 25220 | `5c9e58e` |

Las tareas 1–4 observaron RED por API ausente antes de implementar y GREEN después. La integración de tarea 5 no descubrió un defecto de producción: se corrigió un nombre de getter mal escrito en la prueba y pasó al compilarla; no se cuenta como RED funcional. Una primera fixture de tarea 1 tampoco había eliminado la segunda máquina por un desajuste de espacios: se corrigió esa prueba, sin modificar producción.

## Comportamientos comprobados

- **Formato:** equivalencia de todos los campos y orden con los cuatro tipos de proceso y políticas; Unicode, escapes, arrays vacíos, deadline Long.MAX_VALUE y capacidades int máximas sin preasignar su contenido. Rechazo de versiones desconocidas, campos ausentes/duplicados/desconocidos, null, tipos incorrectos, fracción/exponente, overflow, JSON truncado/comentado y contenido posterior. Referencias y sumas de RAM se validan antes de crear el motor. Reader/Writer prestados no se cierran por el codec.
- **Archivos:** UTF-8 con errores informados; rutas con espacios/acentos; sobrescritura completa; padre inexistente/destino directorio sin destrucción de contenido. Un sustituto Unicode aislado fuerza error de escritura/cierre: el archivo anterior sigue cargable y no quedan temporales. Escritura y cierre completos antes de ATOMIC_MOVE, sin fallback no atómico.
- **Sesión:** guardar en orden FIFO incluye altas/cambios iniciales, excluye borradores y cambios vivos; también funciona RUNNING. Cargar exige pausa confirmada, reemplaza generación tras validar y vuelve a 0 sin tick oculto. Fallos de lectura/escritura conservan ACTIVE, configuración, generación y ciclo. Un almacén controlado por Semaphore demuestra latido del EDT, cierre durante E/S, cancelación de órdenes pendientes y ausencia de instalación posterior a CLOSING.
- **Controles:** panel/botones reales con selector inyectado; cancelar no envía orden, errores visibles, borrador 321 exporta 500 y solo exporta 321 después de Aplicar. Carga válida limpia PCB previo; corrupto conserva ciclo. Cargar deshabilitado RUNNING; ambos botones deshabilitados mientras hay acción pendiente; no se muestra éxito antes de completar E/S.
- **Archivos reales:** `ScenarioIntegrationTest` abre los tres archivos de `examples/` por los botones y UiSession reales. Local: doce pasos, dos terminados, 1/1 producido/consumido, ocupación 0 y semáforos 1/0/1; guardar/recargar repite esa traza. Archivo corrupto mantiene ciclo 12 y permite Paso 13. Remoto L2: PC 1,1,1,2,3,4,5,6, tabla Red y RAM liberada al terminar. Mixto: selección inicial FCFS/EDF/RR/prioridades 1/4/5/8, RR rota a 6 en segundo paso, ocho procesos terminan sin error ni RAM residual de procesos.

## Classpath, interfaz nativa e IDE

`nbactions.xml` sigue enlazando Run y Debug con `run-app` y `debug-app`; ambos declaran `<classpathScope>runtime</classpathScope>` y `<classpath/>`. Una clase de diagnóstico temporal ejecutada mediante **exec:exec@run-app** cargó JsonReader desde `.maven-cache/com/google/code/gson/gson/2.14.0/gson-2.14.0.jar` y terminó con BUILD SUCCESS/0. No se incorporó al código ni al JAR; la siguiente construcción limpia la elimina. Esto comprueba la resolución de runtime; no equivale a iniciar el depurador del IDE.

Por la identidad compartida de Java/NetBeans se reutilizó el envoltorio temporal jpackage `edu.avilaos.validation` del módulo 6, copiando el JAR actual y Gson y agregando ambos al classpath temporal. Se lanzó desde Maven y se observó ventana pausada en 0 con los controles previos. Durante el acceso a Configuración inicial la herramienta agotó tiempo y posteriormente indicó **Mac bloqueado**; se solicitó desbloqueo.

Pendiente verificar interactivamente: selector de carga, extensión automática y confirmación al guardar, archivo inválido en pantalla, recarga a cero, remoto, accesibilidad de la nueva banda en 1180×780/960×640 y cierre por ventana con salida Maven. Las trazas automatizadas no sustituyen esta lista. La compilación/Run/Debug en NetBeans por el estudiante permanece separada.

## Revisión y cierre

Revisión independiente de `235abc1..ed87b5f`, con construcción propia en una copia temporal del código, pasó 25220 comprobaciones y encontró un hallazgo importante, ninguno crítico ni menor. **Editor de tabla anterior:** editar RAM a 777 sin confirmar, cargar un archivo con RAM 128 y luego Aplicar podía confirmar el editor antiguo en el modelo nuevo. La carga debe descartar el contexto de la generación anterior.

`ScenarioControlsTest.loadingDiscardsActiveCellDraft` falló primero con `Esperado: false; recibido: true` para edición activa después de cargar. `ConfigurationPanel.load` ahora cancela la edición antes de reemplazar los datos; la prueba comprueba carga → Aplicar → Guardar y equivalencia del archivo, conservando RAM 128. Suite completa **25234**, BUILD SUCCESS/0. Corrección `eebf7a1` en un único pase, sin segundo revisor ni hallazgos menores diferidos.

`git diff --check` sin incidencias y búsqueda de imports/DOM/sorters prohibidos sin coincidencias en el código nuevo. Evidencia de ciclo de vida mediante semáforos y esperas acotadas, sin sleeps para sincronizar.

El módulo persiste escenarios iniciales, no checkpoints. Un sistema sin reemplazo atómico informa error y conserva destino; no se garantiza durabilidad ante corte eléctrico. Permisos/rutas inválidos se informan al usuario. Las operaciones ya iniciadas pueden terminar al cerrar, pero no instalan otra generación.

Se conserva la rama local y la carpeta NetBeans, sin remoto, PR ni fusión. El proyecto completo aún requiere módulo 8 (métricas/gráfico) y módulo 9 (integración, informe/UML y defensa). Esta guía no marca esos requisitos como cumplidos.

## Alcance confirmado en revisión

El revisor dejó fuera dos comportamientos que el plan excluye expresamente. Se mantiene ese alcance: el archivo reconstruye un escenario inicial; si se necesitara un checkpoint, habría que persistir progreso y estado vivo. El reemplazo atómico protege ante fallos de escritura antes del cambio, pero no promete durabilidad ante apagado abrupto; si esta fuera necesaria, el último guardado podría perderse sin un protocolo adicional de persistencia.

Última construcción limpia tras la corrección: **25234 comprobaciones**, JAR, BUILD SUCCESS y código 0. La aceptación nativa sigue pendiente; las tareas 5–6 conservan ese pendiente y el registro de ejecución para retomarlo al desbloquear el Mac.
