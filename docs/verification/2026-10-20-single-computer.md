# Verificación del módulo 2 — 20 de octubre de 2026

Estado: seis tareas implementadas y verificadas. El motor de un computador funciona mediante ciclos manuales; la GUI sigue siendo la inicial.

## Entorno y construcción

Temurin JDK 25.0.4.1, Maven 3.9.16 incluido en NetBeans, macOS arm64. Rama `feat/single-computer` desde `81a073a`; se conservó la carpeta compartida con NetBeans según el flujo aprobado. No se modificó el Java global de la terminal.

Desde la raíz:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -o -B -Dmaven.repo.local=.maven-cache clean package
```

Resultado observado: `OK: 509 comprobaciones`, `BUILD SUCCESS` y generación de `target/avilaos-1.0-SNAPSHOT.jar`. Las 212 comprobaciones del módulo 1 se mantienen; el módulo 2 añade 297. El runner es `AllTests` mediante exec/core-tests, no Surefire; su bloque `Tests run: 0` no cuenta estas comprobaciones.

## Prueba primero, implementación después

En cada tarea se escribió la prueba, se ejecutó y se observó la compilación fallar por las clases ausentes. Después de implementar se ejecutó toda la suite, se guardó un commit y se volvió a ejecutar mediante task-done.

| Tarea | Fallo observado antes de implementar | Comprobaciones acumuladas | Commit |
| --- | --- | --- | --- |
| RAM | Ram ausente | 237 | `3565bd1` |
| FCFS | OrderedProcessQueue/Scheduler/políticas ausentes | 265 | `efc111f` |
| Admisión | MemoryAdmission ausente | 288 | `1f1d213` |
| CPU | Cpu/Tickable/LinearProgram ausentes | 331 | `d717e36` |
| E/S | IoController/IoRequest ausentes | 371 | `6f13bd9` |
| Computador | Kernel/Computer ausentes | 509 | `f789d09` |

## Trazas comprobadas por ComputerTest

- FCFS: A de 2 instrucciones y B de 1 ejecutan A,A,B en ciclos 0,1,2. La RAM queda totalmente libre y primera ejecución/terminación coinciden con la traza.
- RAM 10: A pide 8, B pide 4, C pide 2. A y C entran; B queda NEW. A termina en 1; B entra detrás de C sin ejecutar otra instrucción en ese ciclo. C ejecuta en 2 y B en 3. Se liberan las 10 unidades.
- E/S: A con 3 instrucciones, ráfaga 1 y duración 2 ejecuta cómputo en 0, solicita en 1, espera en 2/3 y finaliza en 4. B usa CPU en 2/3. A conserva PC=2 y su reserva mientras espera.
- Última E/S: A con 2 instrucciones queda BLOCKED con restante 0 tras ciclo 1 y termina al inicio de 4 sin instrucción extra. Un proceso de llegada 4 puede aprovechar esa RAM al comienzo del mismo ciclo.
- Deadline de 1: una instrucción completa en 0; dos instrucciones vencen tras ejecutar una en 0.
- Vencimiento de NEW no libera la RAM del proceso activo. También se verifica vencimiento de READY y RUNNING.
- Última E/S con plazo 4 vence en 3, cancela su ticket y nunca reaparece en 4/5; terminados no se duplica.
- Entradas incompatibles y ciclos inválidos conservan PC, RAM, colas y siguiente ciclo. Se rechaza reutilizar un ID incluso después de terminar.
- Dos instancias de Computer conservan CPU, RAM, E/S y colas independientes, incluso con el mismo ID de proceso en máquinas distintas.

Las pruebas de componentes verifican además FIFO y ordenamiento estable, inyección de una política distinta, identidad y doble liberación de reservas, sumas sin overflow, E/S concurrentes con empates y cancelación antes/después de completar.

## Revisión final independiente

Un revisor con contexto nuevo revisó `81a073a..f789d09`, las seis tareas y los cinco focos del plan. Compiló todas las fuentes con `javac --release 25` en `/tmp` y ejecutó AllTests: 509 comprobaciones aprobadas. No encontró defectos críticos, importantes ni menores. No se requirió un pase de correcciones. Los documentos de cierre fueron revisados por el implementador, fuera de esa revisión independiente.

Decisiones registradas ante los puntos que el revisor dejó fuera de juicio:

1. Mutación externa del PCB/hardware administrado por el Kernel: se mantiene la propiedad del núcleo; los accesores permiten inspección, no autorizan cambios independientes. Si alguien ignora este contrato, puede desincronizar ciclos/colas. La GUI futura enviará comandos mediante el coordinador.
2. Apropiación, otras políticas, hilos, buffers y GUI completa: siguen los módulos aprobados. El costo es que esas funciones aún no están disponibles; no se declara completo el proyecto.
3. Documentación de cierre: el implementador cotejó README, hoja de ruta, guía de defensa y esta evidencia con el código/pruebas. El costo de excluirla de la revisión independiente es que un error explicativo podría no detectarse por una segunda persona.

No hay hallazgos menores diferidos. Se conserva la rama funcional; no hay remoto GitHub y no se creó ni simuló un PR.

## Explicación al estudiante y límites

La [guía de defensa](../defensa/modulo-2-computador.md) describe qué se construyó, la responsabilidad de cada clase, conceptos base, el orden de ciclo y preguntas probables. El módulo 1 tiene su [guía separada](../defensa/modulo-1-base-y-procesos.md); el [índice](../defensa/README.md) organiza las guías y el futuro resumen final.

No se atribuye a este módulo una simulación visual interactiva, un clúster sincronizado ni políticas apropiativas. Tampoco se dan por probados Clean and Build, cierre y Debug dentro de NetBeans: permanecen las comprobaciones específicas del IDE registradas en el módulo 1. El siguiente módulo incorpora EDF, RR y prioridades apropiativas.

## Construcción desde NetBeans confirmada por el estudiante

El estudiante compartió la salida de su construcción finalizada el 20 de octubre de 2026 a las 13:13:32, hora de Venezuela: `OK: 509 comprobaciones`, generación del JAR, instalación en el repositorio Maven local y `BUILD SUCCESS`. Confirma construcción y pruebas desde su IDE. El fragmento comienza durante las pruebas, por lo que no acredita por sí solo la fase Clean ni una recompilación sin artefactos previos. Cierre y Debug siguen pendientes de comprobación específica.
