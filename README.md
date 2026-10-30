# ÁvilaOS

Proyecto 1 de Sistemas Operativos: simulador de sistema operativo distribuido.

Repositorio privado: [elioaca01/proyecto-sistemasop](https://github.com/elioaca01/proyecto-sistemasop). La rama de entrega es **main**. La integración conserva el historial y está documentada en el [PR #3 hacia develop](https://github.com/elioaca01/proyecto-sistemasop/pull/3) y el [PR #4 hacia main](https://github.com/elioaca01/proyecto-sistemasop/pull/4); sus estados y commits se consultan en GitHub. Las ramas originales de cada módulo se conservan. Para seleccionar explícitamente la entrega:

```sh
git clone --branch main git@github.com:elioaca01/proyecto-sistemasop.git
```

Requiere JDK 25 y Apache NetBeans. El desarrollo sigue el [diseño aprobado](docs/superpowers/specs/2026-10-18-avilaos-design.md) y la [hoja de ruta](docs/superpowers/plans/2026-10-19-avilaos-roadmap.md).

## Abrir y ejecutar en NetBeans

1. En **File > Open Project**, seleccionar la carpeta que contiene `pom.xml`. NetBeans reconocerá el proyecto Maven.
2. En **Tools > Java Platforms**, registrar un JDK 25 si no aparece, seleccionando su carpeta de instalación.
3. En las propiedades del proyecto, **Build > Compile**, seleccionar JDK 25 como plataforma Java. El `pom.xml` fija además `release=25`.
4. Ejecutar **Clean and Build**. La salida debe mostrar `javac [debug release 25]`, `OK: N comprobaciones` y `BUILD SUCCESS`.
5. Ejecutar **Run Project** para abrir la ventana ÁvilaOS. La acción **Debug Project** inicia el mismo programa con el depurador de NetBeans.

La primera construcción descarga plugins de Maven y necesita conexión. La aplicación usa Gson 2.14.0 para leer/escribir JSON; Maven descarga la dependencia desde Maven Central.

## Verificación desde terminal

Con JDK 25 seleccionado y Maven disponible: `mvn test`, `mvn package` y `mvn compile exec:exec@run-app`.

En este Mac, Maven viene incluido en NetBeans. Este comando selecciona Java 25 solo para su ejecución y usa una caché local ignorada por Git:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -Dmaven.repo.local=.maven-cache test
```

Para empaquetar, sustituir `test` por `package`; para abrir la ventana, por `compile exec:exec@run-app`. La aplicación se ejecuta en una JVM separada de Maven para que su cierre no termine el proceso de construcción.

Las pruebas son ejecutables Java con comprobaciones propias; no requieren JUnit ni `-ea`. Maven puede mostrar `Tests run: 0` en el bloque de Surefire: el resultado de nuestras pruebas aparece después, en `exec (core-tests)`, como **`OK: N comprobaciones`**. Si una comprobación falla, la construcción también falla.

## Alcance actual

- Lista enlazada con ordenamiento estable, cola FIFO y buffer circular propios.
- Definiciones validadas de CPU bound, I/O bound, productores y consumidores.
- PCB con estados, PC/MAR, contexto, memoria asignada, contadores de servicio y reglas de deadline.
- Computador reutilizable con CPU, RAM acotada, núcleo y controlador de E/S.
- Admisión según memoria, ejecución de CPU bound/I/O bound y terminación automática por deadline.
- FCFS, EDF, Round Robin y prioridades apropiativas por interfaz; política/quantum configurables por computador con aplicación en el próximo ciclo válido.
- Orden de entrada a listos conservado al cambiar de política, empates estables y contexto/RAM preservados al apropiar.
- Ciclos lógicos consecutivos, una instrucción por CPU y ciclo, cancelación de E/S y liberación segura de RAM.
- Clúster de al menos dos computadores con hilo por máquina y reloj coordinador mediante semáforos Java.
- Pausa, paso y ejecución continua por API, comandos entre ciclos, IDs globales e instantáneas inmutables para lectores concurrentes.
- Cierre ordenado y propagación de fallos; un ciclo parcial fallido no se confirma como completado.
- Productores/consumidores locales y remotos: buffers FIFO en RAM anfitriona, semáforos simulados, latencia por elemento y limpieza por deadline.
- Comandos e instantáneas de buffers, reservas, fases y eventos; 25550 comprobaciones acumuladas del motor y la interfaz, incluidos ocho experimentos finales.
- Interfaz Swing con configuración, formularios, vistas de CPU/PCB/colas/RAM, buffers/semáforos, registro y controles. Sesiones y cierre asíncronos; aceptación nativa local/remota, tamaño mínimo y cierre de Maven comprobados.
- Guardar/cargar escenarios JSON versionados y validados desde Configuración inicial; archivos de ejemplo en `examples/`. Aceptación del selector nativo pendiente por falta de respuesta de la herramienta de interfaz.
- Pestaña Métricas: throughput, CPU usuario/núcleo, respuesta, deadlines, Jain, esperas por buffer e historial completo de utilización. Renders revisados y escenario local/promedio de espera confirmados por el estudiante. Otros pendientes manuales están en el checklist de entrega.

`ProcessDefinition` conserva los atributos iniciales inmutables; `ProcessControlBlock` mantiene el estado vivo del proceso. Apropiar o bloquear conserva PC/MAR y la RAM asignada; ejecutar una instrucción avanza PC/MAR. El núcleo invoca estas operaciones y MemoryAdmission controla las reservas de RAM simulada.

El deadline es relativo al ciclo de llegada. Llegar en ciclo 5 con plazo 1 permite ejecutar en ciclo 5 y vence al cierre de ese ciclo si el proceso no termina. Los bloqueos no detienen el avance del plazo global.

## Explicación y evidencia por módulo

- [Guías de defensa por módulo](docs/defensa/README.md): módulos 1–9, con conceptos y preguntas; [resumen unificado](output/pdf/avilaos-defensa-resumen.pdf) de dos páginas.
- [Verificación del módulo 1](docs/verification/2026-10-19-foundation.md).
- [Verificación del módulo 2](docs/verification/2026-10-20-single-computer.md).
- [Verificación del módulo 3](docs/verification/2026-10-20-scheduling.md).
- [Verificación del módulo 4](docs/verification/2026-10-22-cluster.md).
- [Verificación del módulo 5](docs/verification/2026-10-23-buffers.md).
- [Verificación del módulo 6](docs/verification/2026-10-23-interface.md).
- [Verificación del módulo 7](docs/verification/2026-10-23-scenarios-json.md).
- [Verificación del módulo 8](docs/verification/2026-10-25-metrics.md).
- [Integración y aceptación final](docs/verification/2026-10-25-final-acceptance.md).
- [Revisión independiente final](docs/verification/2026-10-25-final-review.md).

## Informe y entrega

- [Casos para confirmar el funcionamiento en NetBeans](docs/entrega/casos-prueba-manuales.md): 16 pruebas con pasos, ocho escenarios adicionales y resultados esperados.

- [Informe PDF](output/pdf/avilaos-informe.pdf): arquitectura, UML de clases/estados, fórmulas y comparaciones reproducibles de políticas y red.
- [Fuentes y regeneración del informe](docs/informe/README.md); [fuentes UML](docs/uml/README.md).
- [Preparar el ZIP y comprobar sus hashes](docs/entrega/README.md).
- [Checklist manual y envío](docs/entrega/checklist.md): pruebas de archivos, Debug/copia limpia en NetBeans, acceso GitHub y entrega académica que aún requieren confirmación.

Las conclusiones comparativas se limitan a las cargas y horizontes publicados en `docs/verification/data/final-comparisons.json`. La generación del informe usa herramientas Python separadas; no son necesarias para compilar ni ejecutar el simulador.

Auditoría del 29 de octubre: [estándares de desarrollo, interfaz e informe](docs/verification/2026-10-29-development-standards.md). GitHub contiene 12 ramas con nombres funcionales; falta registrar Issues e integrar mediante Pull Requests comentados.
