# Verificación del módulo 3 — 20 de octubre de 2026

Estado: políticas, apropiación, quantum y cambios de configuración implementados, verificados y revisados independientemente.

## Entorno y resultado

Temurin Java 25, Maven 3.9.16 incluido en NetBeans, macOS arm64. Rama `feat/scheduling-policies`, base `03fd3ab`; se conserva la carpeta compartida con NetBeans aprobada por el estudiante.

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -o -B -Dmaven.repo.local=.maven-cache clean package
```

Resultado observado: `OK: 916 comprobaciones`, `BUILD SUCCESS`, generación de `target/avilaos-1.0-SNAPSHOT.jar`. Se mantienen las 509 comprobaciones anteriores y se añaden 407. El runner propio se ejecuta en exec/core-tests; el bloque Surefire con cero tests no representa ese resultado.

## Evidencia por tarea

Cada prueba nueva se ejecutó antes de implementar y falló por la API/clase ausente. Después se ejecutó toda la suite; task-done la repitió tras el commit.

| Tarea | Fallo observado | Total acumulado | Commit |
| --- | --- | --- | --- |
| Historial de listos | restoreFifo ausente | 543 | `f6f2cbd` |
| Políticas | clases/API de políticas ausentes | 609 | `49f5795` |
| Apropiación del núcleo | constructor y accesores extendidos ausentes | 768 | `dabe0ca` |
| Cambios entre ciclos | configuración y constructor Computer ausentes | 916 | `a0b4061` |

## Comportamientos comprobados

- **Orden de entrada:** se recupera A,B,C tras ordenar C,A,B. Si se retira y reintroduce B, el FIFO es A,C,B. Los IDs no se usan como sustituto del orden de entrada.
- **EDF/prioridades:** claves 30,10,10 o 2,0,0 seleccionan B,C,A; los empates usan la entrada a listos, incluso tras otra política. Se verifican extremos numéricos y deadlines absolutos con llegadas distintas.
- **Apropiación por llegada:** A usa ciclo 0; B llega en 1 con mejor deadline/prioridad y ejecuta ese ciclo. A sigue en 2/3/4, sin perder PC/MAR, RAM ni firstRunCycle. Un empate conserva al actual.
- **RR:** A de 3 instrucciones y B de 2 dan A,A,B,B,A con quantum 2 y A,B,A,B,A con quantum 1. A solo ejecuta cinco ciclos consecutivos con quantum 2, sin ciclos IDLE ni reiniciar la primera ejecución; completa en el último ciclo permitido por su deadline.
- **Desbloqueo:** A solicita E/S en 1, B usa 2/3 y A lo desplaza al volver en 4 con una clave mejor. B conserva su avance y reserva.
- **Cambio de política:** la solicitud no altera inmediatamente política activa ni PC; entra al comienzo del próximo tick válido. FCFS no apropia al actual aunque un listo tenga un plazo más cercano.
- **Regreso a FIFO:** prioridades deja C,B en listos; cambiar a FCFS/RR recupera B,C según entrada, no según el orden previamente presentado.
- **Quantum nuevo:** solicitar RR/1 tras consumir un ciclo con RR/2 reinicia el turno en el siguiente tick, antes de evaluar apropiación. La última pareja válida solicitada reemplaza otras pendientes.
- **Rechazo sin mutaciones:** configuración null o quantum no positivo y ticks inválidos conservan PC, RAM, colas, turno y configuración pendiente; el próximo tick válido aplica la solicitud anterior.
- **E/S al agotar turno:** la última IO_REQUEST queda únicamente BLOCKED con restante cero y un ticket. Cambiar de política mientras espera conserva su finalización en 4. Con deadline 4, vence en 3, cancela y no revive en 4/5; no duplica terminados y devuelve toda RAM.
- **Independencia/inactividad:** políticas y quantum son por computador. También se aplican cambios sin procesos o con todos bloqueados, sin conceder instrucciones extra.

## Revisión final

Un revisor con contexto nuevo examinó `03fd3ab..a0b4061`, la especificación, el plan, las pruebas y los documentos de cierre. Compiló las fuentes por separado con JDK 25 en `/tmp` y ejecutó AllTests: **916 comprobaciones satisfactorias**. También verificó cambios sucesivos con un solo proceso, conservación de tiempos de E/S y primera ejecución, terminación en el último ciclo permitido y cambios integrados EDF → FCFS/RR: PASS.

No encontró hallazgos críticos, importantes ni menores. Su lista de comportamientos descartados de juicio está vacía; no hubo decisiones materiales fuera del plan ni correcciones diferidas. `git diff --check` no informó errores, los enlaces documentales se verificaron y los únicos imports de java.util son Comparator.

Se siguió el plan aprobado sin desviaciones materiales. La ejecución nativa y el uso de la misma carpeta de NetBeans conservan la preferencia del estudiante. El registro temporal de este plan se elimina al cerrar; esta evidencia y los commits conservan el resultado.

## Alcance y verificación en el IDE

Existe API del motor para configuración inicial y cambios en límites de ciclo. La ventana inicial sigue intacta; aún no hay controles gráficos, workers ni reloj global con hilos. La exclusión mutua real y el canal de comandos corresponden al módulo 4.

El estudiante ya compartió una construcción satisfactoria del módulo 2 desde NetBeans, finalizada a las 13:13:32 del 20 de octubre: 509 comprobaciones, JAR e instalación local, BUILD SUCCESS. La [evidencia anterior](2026-10-20-single-computer.md) distingue lo visible del fragmento de Clean, cierre y Debug no acreditados. La construcción de este módulo 3 desde el IDE aún debe comprobarse por separado.

Se conserva la rama funcional y los commits. Sin remoto GitHub, no se ha creado PR ni realizado fusión para sustituirlo.

La [guía breve de defensa](../defensa/modulo-3-planificacion.md) resume conceptos, clases, ejemplo y tres preguntas, conforme a la preferencia del estudiante.
