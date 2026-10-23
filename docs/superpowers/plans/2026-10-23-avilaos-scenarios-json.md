# ÁvilaOS — Module 7: JSON Scenarios Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** guardar y cargar escenarios iniciales completos desde Swing, sin perder la configuración actual ante archivos inválidos ni bloquear la interfaz.

**Architecture:** un codec streaming convierte JSON versionado a SimulationConfig, que conserva la validación común del modelo. Un almacén se ocupa de UTF-8 y escritura temporal/reemplazo atómico. El Thread existente de UiSession realiza el acceso a archivos y la reconstrucción; Swing selecciona rutas y presenta resultados.

**Tech Stack:** Java 25, Maven/NetBeans, Swing, Gson 2.14.0 exclusivamente mediante JsonReader/JsonWriter y Strictness.STRICT; arreglos y estructuras propias.

**Spec:** [diseño aprobado](../specs/2026-10-18-avilaos-design.md), secciones 2 y 8; [hoja de ruta](2026-10-19-avilaos-roadmap.md), módulo 7. Base `ce87c94`, módulo 6 verificado con 24775 comprobaciones. Las decisiones de escenarios frente a checkpoints y Gson streaming ya pertenecen al diseño aprobado; este plan concreta su implementación.

**Execution:** método nativo conservado, misma carpeta compartida con NetBeans; al implementar crear rama `codex/scenario-json`. Una revisión independiente del rango completo al final, sin implementadores ni revisores por tarea. Aprobado el 23 de octubre: «Apruebo, implementa». Ejecución nativa en curso.

## Global Constraints

- Java 25 sin preview; ejecución normal y Debug desde NetBeans mediante las acciones Maven existentes.
- No usar List, Map, Set, Collections, PriorityQueue, streams con colecciones, JsonObject/JsonArray ni deserialización reflectiva. Gson se limita al análisis/emisión de tokens; listas y ordenamiento del simulador siguen siendo propios.
- Thread y Semaphore para concurrencia real. E/S de archivos, cierre y reconstrucción fuera del EDT. Reutilizar el worker de UiSession.
- Guardar el escenario inicial aplicado, incluyendo altas y cambios realizados antes del primer Iniciar/Paso; excluir altas/cambios vivos y borradores aún sin aplicar.
- Cargar se permite cuando la sesión está ACTIVE y el motor confirma PAUSED, incluso después de haber ejecutado ciclos. Una carga válida sustituye el escenario y queda pausada en ciclo 0, con otra generación e IDs nuevos desde 1 al aplicar las cargas. No reanudar automáticamente ni avanzar ciclos ocultos.
- Validar el archivo completo antes de cerrar el motor anterior. JSON/sintaxis/validación/E/S rechazados conservan escenario, generación y ciclo anteriores; no marcar FAILED por un error de archivo del usuario.
- Mantener la guía breve por módulo y evidencia separada de las comprobaciones del estudiante en NetBeans. JSON no incorpora métricas ni checkpoints del motor.

## Review Focus

1. Solicitar cargar/reiniciar/cerrar mientras hay E/S pendiente: EDT disponible, sin instalar una generación después de cerrar ni mezclar avisos de motores distintos; prueba en tarea 3.
2. Archivo válido guardado tras cambios vivos o con campos en edición: reproducir exactamente el escenario inicial aplicado, sin colar el estado vivo o los borradores; pruebas en tareas 3–5.
3. Valores numéricos precisos, duplicados, campos inesperados, JSON truncado o contenido posterior: rechazar sin coerciones ni reemplazo parcial; pruebas en tareas 1 y 3.
4. Sobrescribir archivo con un fallo antes del reemplazo o un destino inválido: conservar el anterior sin truncarlo y retirar el temporal; pruebas en tarea 2.
5. Cancelación/errores de selector, nuevas dependencias en Run y ventana mínima: sin órdenes al cancelar, classpath completo y controles accesibles; pruebas en tareas 4–5.

## Formato versión 1

Todas las propiedades son obligatorias; se acepta cualquier orden, se rechazan propiedades repetidas/desconocidas y null. Nombres de enums iguales a los del modelo. Los números son tokens NUMBER con representación entera decimal, sin fracción/exponente ni comillas; el parseo int/long rechaza overflow. Los arreglos mantienen orden; buffers/procesos pueden estar vacíos. Los nombres de procesos admiten Unicode y los escapes JSON normales. No se guarda ID global del PCB: lo asigna el motor al cargar la definición.

```json
{
  "formatVersion": 1,
  "cycleMillis": 500,
  "networkLatency": 2,
  "computers": [
    {"id": 1, "ramCapacity": 64, "policy": "FCFS", "quantum": 2},
    {"id": 2, "ramCapacity": 64, "policy": "ROUND_ROBIN", "quantum": 1}
  ],
  "buffers": [{"id": 1, "hostId": 1, "capacity": 1}],
  "processes": [{
    "name": "Productor remoto", "computerId": 2, "type": "PRODUCER",
    "instructionCount": 6, "memoryRequired": 2, "priority": 0,
    "deadlineCycles": 100, "cpuBurst": 0, "ioCycles": 0,
    "bufferId": 1, "operationInterval": 1, "targetItems": 1
  }]
}
```

Decisiones de implementación derivadas del diseño: importar estando pausado reemplaza también una ejecución que ya avanzó; guardar no aplica borradores; formato 1 estricto para hacer visibles errores tipográficos. Si un sistema de archivos no admite reemplazo atómico, informar el fallo y conservar el destino, sin fallback que pueda dejar un archivo incompleto. No prometer persistencia frente a corte eléctrico.

## Task 1: codec versionado y dependencia

**Files:** modificar `pom.xml`, `src/test/java/avilaos/tests/AllTests.java`; crear `src/main/java/avilaos/config/{ScenarioJson,ConfigurationException}.java`, `src/test/java/avilaos/tests/ScenarioJsonTest.java`.

**Interfaces:** `ConfigurationException extends IOException`, constructor `(String message)` y `(String message,Throwable cause)`. `ScenarioJson.read(Reader) throws IOException` devuelve SimulationConfig; `ScenarioJson.write(Writer,SimulationConfig) throws IOException`. Ambos usan recursos prestados: no cierran Reader/Writer; write vacía su salida. `public static final int FORMAT_VERSION=1`.

- [x] Escribir primero `ScenarioJsonTest.run()`: leer un literal del formato anterior y comprobar cada campo; leer CPU/IO/productor/consumidor y las cuatro políticas; guardar/leer equivalencia campo a campo y orden. Comprobar Unicode, comillas/escapes, campos desordenados, arrays vacíos y deadline Long.MAX_VALUE sin perder precisión. Registrar en AllTests. Ejecutar BUILD y observar fallo por codec ausente.
- [x] Añadir dependencia fijada `com.google.code.gson:gson:2.14.0`. La caché del proyecto aún no contiene Gson: permitir descarga normal desde Maven Central en la primera construcción; después comprobar construcción offline. No copiar bibliotecas al árbol fuente ni cambiar el Java global.
- [x] Implementar codec streaming estricto. Acumular arrays con LinkedList propia y copyInto; detectar propiedades por máscara de campos, sin Set/Map. Construir ComputerConfig/BufferDefinition/ProcessDefinition y, al terminar todo el documento, SimulationConfig. No ordenar procesos ni aceptar sus estados vivos. Emitir JSON indentado en orden fijo para facilitar la defensa.
- [x] Añadir casos inválidos: vacío, truncado, comentarios, comillas simples, coma final, null, tipos incorrectos, boolean/string numérico, fracción/exponente, overflow int/long, versión 0/2, propiedad ausente/repetida/desconocida, dos valores raíz; enums inexistentes, menos de dos máquinas, IDs duplicados, referencias rotas, suma de buffers sobre RAM, proceso demasiado grande y presupuesto productor insuficiente. Envolver errores de tokens/representación/validación como ConfigurationException con ruta/campo y causa; no convertir fallos del lector en valores predeterminados. Leer un Writer/Reader controlado comprueba propiedad de recursos y propagación de IOException.
- [x] BUILD pasa con pruebas previas. Commit `feat: define formato JSON estricto de escenarios`.

## Task 2: archivos UTF-8 y guardado seguro

**Files:** crear `src/main/java/avilaos/config/{ConfigurationStore,JsonConfigurationStore}.java`, `src/test/java/avilaos/tests/ConfigurationStoreTest.java`; modificar AllTests.

**Interfaces:** `ConfigurationStore` interfaz con `SimulationConfig load(Path file) throws IOException` y `void save(Path file,SimulationConfig config) throws IOException`; `JsonConfigurationStore()` implementación final que consume ScenarioJson. Sus métodos no crean ni modifican motores.

- [x] Pruebas RED: guardar en directorio temporal y volver a cargar, nombres/rutas con espacios y acentos; sobrescribir escenario A por B y comprobar B completo; leer inexistente/UTF-8 inválido; padre inexistente y destino directorio rechazan sin destruir contenido. Salida de prueba va a temporal y se limpia en finally. BUILD falla por almacén ausente.
- [x] Implementar UTF-8 con codificador/decodificador que informen entradas mal formadas. Guardar en temporal único del mismo directorio, cerrar correctamente writer/archivo y solo entonces Files.move con ATOMIC_MOVE y REPLACE_EXISTING. En finally retirar el temporal si queda. Si no se admite la operación atómica, devolver error explícito sin fallback. No crear padres automáticamente ni borrar primero el destino.
- [x] Verificar conservación del destino ante error de escritura usando un nombre con sustituto Unicode aislado y codificador REPORT (o una reproducción equivalente de IOException si Gson lo escapase); comprobar que no quedan temporales tras fallo y que el archivo anterior aún se carga. Guardar no cambia el objeto SimulationConfig. BUILD pasa; commit `feat: guarda escenarios UTF-8 mediante reemplazo atomico`.

## Task 3: carga/guardado en la sesión fuera del EDT

**Files:** modificar `src/main/java/avilaos/ui/session/UiSession.java`; crear `src/test/java/avilaos/tests/UiSessionPersistenceTest.java`; modificar AllTests.

**Interfaces:** conservar constructor UiSession(SimulationConfig), delegando a sobrecarga `UiSession(SimulationConfig,ConfigurationStore)`. Añadir `long loadScenario(Path)` y `long saveScenario(Path)`. Resultados mediante SessionNotice existente, con engineRequestId=-1; no fingir CommandResult del motor. Mantener SessionView sin otro estado mutable público.

- [ ] RED: almacenar un escenario inicial con CPU/IO/productor/consumidor, avanzar y modificar altas/política/duración vivos, solicitar guardar y comprobar que el archivo/almacén recibe exactamente la configuración inicial. Antes del primer ciclo las altas sí se incorporan. Validar FIFO: guardar tras una alta inicial incluye esa alta, sin ejecutar el motor.
- [ ] RED: cargar válido antes de empezar y después de varios ciclos pausados: cambia generación, nextCycle=0, started=false, motor PAUSED, no PCB viejo y cargas nuevas pendientes hasta Paso. Cargar mientras RUNNING se rechaza sin pausar/reemplazar. Entrada inválida/IOException conserva configuración, generación, estado y ciclo previos. Mantener replaceScenario original restringido a antes de empezar; carga usa ruta explícita distinta.
- [ ] Implementar acciones LOAD/SAVE en la cola de sesión. Rechazar carga si fase/estado no permiten; leer y validar entero antes de replace. IOException esperada produce aviso de archivo y conserva ACTIVE. Guardar captura initial al procesar su acción, sin serializar SimulationSnapshot ni construir un motor.
- [ ] Pruebas con ConfigurationStore de prueba controlado por Semaphore: bloquear carga/guardado, demostrar latido EDT mientras pendingActions>0, pedir cierre y liberar E/S; no instalar el escenario cargado después de CLOSING y cerrar todos los hilos. La operación de archivo ya iniciada puede terminar; las pendientes se cancelan. Comprobar también carga seguida de reinicio y avisos/IDs de generación. Sin sleeps, tiempos acotados y finally.
- [ ] BUILD pasa; commit `feat: integra persistencia asincrona con el ciclo de vida de la sesion`.

## Task 4: controles y selector de archivos

**Files:** crear `src/main/java/avilaos/ui/{ScenarioFilePicker,SwingScenarioFilePicker}.java`, `src/test/java/avilaos/tests/ScenarioControlsTest.java`; modificar `ui/{DashboardPanel,UiController}.java` y AllTests.

**Interfaces:** ScenarioFilePicker declara `Path chooseOpen(Component parent)` y `Path chooseSave(Component parent)`, null significa cancelar. SwingScenarioFilePicker usa JFileChooser con filtro JSON, añade `.json` si falta al guardar y confirma sobrescritura del destino elegido. DashboardPanel() delega a DashboardPanel(ScenarioFilePicker). UiController añade `loadScenario(Path)` y `saveScenario(Path)`, delegaciones EDT no bloqueantes.

- [ ] RED con paneles/botones reales y picker controlado: cancelar no envía acciones ni modifica archivos; guardar/cargar envían la ruta elegida; selección de destino sin extensión y confirmación de sobrescritura quedan en el picker nativo. No abrir JFileChooser en pruebas headless.
- [ ] Añadir banda fija superior dentro de la pestaña Configuración inicial con botones `scenario.load` «Cargar escenario» y `scenario.save` «Guardar escenario inicial». Texto: «Guardar usa la configuración inicial aplicada. Cargar vuelve al ciclo 0». No ampliar la barra global de controles. Debajo sigue ConfigurationPanel desplazable.
- [ ] Guardar habilitado ACTIVE y sin acciones de sesión pendientes, también durante ejecución. Cargar solo ACTIVE/PAUSED y sin pendientes. Deshabilitar ambos al reemplazar/cerrar; el worker revalida aunque se haya abierto un selector con otro estado. Error de ruta/archivo visible, sin diálogo que bloquee esperando E/S.
- [ ] Pruebas: borrador ms=321 no aplicado exporta el inicial 500; al aplicar y guardar exporta 321. Altas vivas no aparecen en el archivo; tras cargar se limpian selecciones/contexto anterior. El detalle de errores indica archivo/campo, sin afirmar guardado/carga antes del aviso de finalización. BUILD pasa; commit `feat: agrega controles Swing para guardar y cargar escenarios`.

## Task 5: ejemplos y aceptación integrada/nativa

**Files:** crear `examples/{local-producer-consumer,remote-producer,io-and-scheduling}.json`, `src/test/java/avilaos/tests/ScenarioIntegrationTest.java`; modificar AllTests y correcciones solo tras reproducción.

- [ ] Escribir ejemplos del formato 1: local C/P en computador1, capacidad1, instrucciones6, RAM2, intervalo/cuota1; remoto productor en computador2/host1/L2; ejemplo con CPU e IO y políticas iniciales distintas. Prueba lee cada archivo real con JsonConfigurationStore, no otra copia literal inventada.
- [ ] Ejecutar las mismas trazas tras cargar/guardar/recargar: local12 pasos, producidos/consumidos1/1, semáforos1/0/1; remoto PC[1,1,1,2,3,4,5,6]. Carga inválida mantiene la simulación pausada que ya avanzó; dar otro Paso comprueba que sigue utilizable. Conservar las pruebas de 24775 comprobaciones previas. Registrar un RED solo si ocurre.
- [ ] Construcción con dependencia resuelta y luego offline. `clean package`, diff --check y revisión de imports sin colecciones/sorters prohibidos. Comprobar que Run/Debug mantienen Gson en classpath mediante el `<classpath/>` runtime ya configurado; no basta que el codec pase dentro de Maven test.
- [ ] Abrir interfaz real: cargar ejemplo local con selector, dar12 pasos, guardar a un destino de prueba, reiniciar/cargar ese archivo y comprobar ciclo0; error de archivo corrupto preserva la ejecución. Revisar ejemplo remoto y controles dentro de Configuración inicial en 1180×780 y 960×640. Si vuelve la ambigüedad Java, copiar JAR y dependencia a envoltorio temporal de validación con identidad exclusiva, documentando diferencia frente a NetBeans. Cerrar por ventana y comprobar salida normal del proceso Maven.
- [ ] Commit `test: verifica escenarios JSON desde archivos y controles reales`.

## Task 6: defensa, evidencia y revisión final

**Files:** crear `docs/defensa/modulo-7-escenarios-json.md`, `docs/verification/2026-10-23-scenarios-json.md`; actualizar README, índice de defensa y hoja de ruta.

- [ ] Guía breve: configuración inicial frente a checkpoint, JSON versionado, validación previa, escritura temporal/atómica y E/S fuera del EDT. Ejemplo guardar/cargar y tres preguntas de defensa. Lista manual para NetBeans con resultado esperado; no inferir validación del IDE desde terminal/copia temporal.
- [ ] Registrar totales reales, pruebas nativas realizadas, errores corregidos, dependencia/versiones y límites. Si un control no pudo verificarse visualmente, decir cuál y por qué; no declarar aceptación completa por tests headless.
- [ ] Una revisión independiente final del rango completo, en modelo más capaz disponible, usando Review Focus literal y ledger. Corregir Critical/Important en un pase con regresiones RED→GREEN y suite completa; menores diferidos explícitos, sin segundo revisor.
- [ ] Suite, clean package y diff --check satisfactorios; commit `docs: explica y verifica escenarios JSON para la defensa`. Conservar carpeta y rama local; sin PR/fusión inventados. Retirar solo scratch propio después de conservar evidencia. Siguiente módulo: métricas y gráfico.

## Verificación y fuentes

Comando habitual tras resolver la nueva dependencia (primera vez sin `-o`):

```sh
/usr/bin/env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -o -B -Dmaven.repo.local=.maven-cache test
```

`clean package` sustituye test para construcción limpia. En cada verificación leer `OK: N comprobaciones`, BUILD SUCCESS y código0. Surefire=0 no es el total de AllTests.

Fuentes técnicas oficiales consultadas: [Gson 2.14.0](https://github.com/google/gson/releases/tag/gson-parent-2.14.0) y [modo estricto de Gson](https://google.github.io/gson/Troubleshooting.html). No se descargó ni incorporó todavía la dependencia.

Autorrevisión del plan: cobertura del diseño §8 y RF7 distribuida en tareas1–5; firmas del codec/almacén/sesión/controlador alineadas; tests de las cinco condiciones de Review Focus ubicados; carga de escenarios distinta de edición previa al inicio; Gson aislado de las estructuras del simulador; documentación y aceptación del IDE diferenciadas. No se rediseñan memoria, planificación ni métricas.
