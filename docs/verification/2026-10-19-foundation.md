# Verificación del módulo 1 — 19 de octubre de 2026

Estado: código implementado, construcción y pruebas verificadas; arranque visual desde NetBeans confirmado mediante la captura del estudiante. Permanecen pendientes las comprobaciones específicas del IDE indicadas al final.

## Entorno

- macOS arm64; Temurin JDK y javac 25.0.4.1.
- Maven 3.9.16 incluido en Apache NetBeans.
- Rama `feat/core-foundation`, basada en `1737e2f`.
- Caché de plugins en `.maven-cache/`, ignorada por Git. No se cambió la selección global de Java 17 de la terminal.

## Evidencia observada

Comando de construcción, desde la raíz del proyecto:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -B -Dmaven.repo.local=.maven-cache clean package
```

Resultado: `javac [debug release 25]`, `OK: 212 comprobaciones` y `BUILD SUCCESS`. Se generó `target/avilaos-1.0-SNAPSHOT.jar` y se inspeccionó su contenido: incluye App, ventana, estructuras y modelo; no incluye las clases de pruebas.

| Tarea | Fallo observado antes de implementar | Resultado posterior |
| --- | --- | --- |
| 1. Lista | LinkedList no existía; falló compilación de sus pruebas | 31 comprobaciones acumuladas; BUILD SUCCESS |
| 2. Cola y buffer circular | LinkedQueue y RingBuffer no existían | 71 comprobaciones acumuladas; BUILD SUCCESS |
| 3. Definición | ProcessDefinition y ProcessType no existían | 103 comprobaciones acumuladas; BUILD SUCCESS |
| 4. PCB | ProcessControlBlock no existía | 212 comprobaciones acumuladas; BUILD SUCCESS |
| 5. Arranque | ClassNotFoundException: avilaos.App | App y ventana compiladas y empaquetadas; arranque visual desde NetBeans confirmado |

Comprobaciones adicionales:

- `mvn test -Dexec.mainClass=does.not.Exist` pasó las 212 comprobaciones: elegir otra clase de arranque no reemplaza el runner de pruebas.
- `mvn exec:exec@run-app -Dexec.mainClass=--version` ejecutó la JVM hija y mostró Java 25.0.4.1. Verifica la configuración de lanzamiento, no sustituye ejecutar App y observar su ventana.
- `javap -verbose` sobre el PCB generado mostró `major version: 69`, correspondiente al objetivo Java 25.
- Revisión de fuentes: el único import de `java.util` es Comparator; no se encontraron colecciones estándar para almacenar datos.
- `git diff --check` no informó errores de whitespace.
- Los logs originales permanecen en el workspace ignorado `.superpowers/sdd/2026-10-19-avilaos-foundation/` mientras siga pendiente el cierre.

## Revisión del código

Se revisaron los contratos entre estructuras, definición/PCB y arranque, además de los cinco casos de Review Focus del plan: operaciones inválidas sin mutación, presupuesto sin overflow, transiciones válidas, deadline de un ciclo y selección de JDK 25.

Revisión propia del implementador, sin un revisor independiente: se conservó la ejecución nativa aprobada y la indicación del plan de no iniciar agentes adicionales. No se identificaron defectos críticos o importantes en el código del módulo. No hay correcciones menores diferidas registradas. Esta revisión no verifica por sí misma el comportamiento visual ni la integración real del IDE.

Decisión de ejecución: se mantuvo la carpeta compartida con NetBeans y se aisló el trabajo en una rama funcional. Una rama conserva el mismo árbol de archivos; no ofrece el aislamiento físico de otra carpeta, pero en este repositorio inicialmente limpio mantiene el flujo acordado sin duplicar el proyecto.

## Confirmación visual del estudiante

La [captura aportada](</Users/eliocolmenares/Desktop/Screenshot 2026-10-19 at 23.49.56.png>) muestra Apache NetBeans IDE 31, la salida `Output - Run (ÁvilaOS)` con la acción `exec:3.6.4:exec (run-app)` y la ventana «ÁvilaOS» con la descripción esperada. Esto confirma el arranque integrado desde el IDE.

La captura muestra `JDK 26 (Default)`. Es una versión posterior a Java 21, admitida por el enunciado; no prueba que se haya seleccionado JDK 25 en las propiedades. El objetivo del proyecto sigue siendo `release=25` y la construcción limpia con Temurin 25 está verificada por separado. La salida visible dice «Nothing to compile»; no se atribuye a esta imagen una recompilación limpia ni una prueba de cierre.

## Comprobaciones pendientes

- Ejecutar Clean and Build desde NetBeans para comprobar una recompilación sin archivos previos; el arranque Run y su ventana ya están confirmados.
- Comprobar el cierre normal de la ventana y el fin del proceso hijo.
- Probar Debug Project desde NetBeans. La acción está configurada, pero no se declara verificada.
- Conectar GitHub y registrar Issue/PR hacia develop. Aún no hay remoto configurado; no se ha realizado ninguna fusión local para sustituir ese PR.

Se pidió al estudiante la comprobación en NetBeans porque las herramientas reconocen el proceso Java del IDE pero no permiten vincular su ventana para operarla. La captura resuelve la comprobación del arranque visual; no sustituye las otras comprobaciones del listado.

## Próximo módulo

CPU, RAM, núcleo y admisión de un computador; FCFS y E/S simulada. Los contadores de RAM y la finalización automática por reloj todavía no existen: el PCB solo encapsula los cambios de estado y contexto que ese núcleo invocará.
