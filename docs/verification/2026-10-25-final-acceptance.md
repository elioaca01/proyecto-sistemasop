# Aceptación final — evidencia al 25 de octubre de 2026

## Confirmación manual del estudiante

El estudiante compartió salida de build: **OK: 25454 comprobaciones**, JAR generado e instalado, **BUILD SUCCESS**, 6.354s, finalización `2026-10-25T22:57:50-04:00`. Después de las instrucciones de Run y carga local, aportó dos capturas de la ventana real, pausada en ciclo12:

- Métricas: completados2, vencidos0, pendientes0, throughput0.167, CPUglobal50.00%, usuario8.33%, núcleo41.67%, respuesta0.500. Computador1CPU100%, computador2CPU0%.
- B1: anfitrión1, capacidad1, producido1, consumido1, ocupación0, vacíos/llenos/mutex1/0/1, sin esperas ni operaciones activas.
- Al indicarle la subpestaña Métricas → Buffers y esperas, confirmó «si esta ahi» respecto al promedio **6.000 ciclos**. Esa última comprobación es una confirmación textual, no un dato visible en las dos capturas.

Las imágenes completas incluyen el escritorio del estudiante: se conservan referencias/hashes, sin copiarlas a Git ni incluirlas completas en el informe.

- `Screenshot 2026-10-25 at 23.03.07.png`: SHA-256 `c7d96a832ec6cd457a00633e3e987b6c3e5f7494e9b28357b5f3e09ed5d10caa`.
- `Screenshot 2026-10-25 at 23.03.36.png`: SHA-256 `c1609f40d816e21b8547447885b56febbb2d1c66e64904cac1503d0b00f9e91a`.

La evidencia confirma build y resultado visual local siguiendo el procedimiento del estudiante. No demuestra por sí sola Debug, compilación desde una copia limpia en el IDE, cada paso del selector de archivos ni cierre de la ejecución actual.

## Matriz funcional

| RF | Implementación | Evidencia automatizada | Manual actual |
| --- | --- | --- | --- |
| 1 Arquitectura | Computer ensambla CPU/RAM/IO/Kernel; Tickable, GlobalClock y ComputerWorker | ComputerTest, ComputerWorkerTest, GlobalClockTest, ClusterIntegrationTest | Arranque/ventana y varios módulos visuales anteriores |
| 2 Procesos/PCB | ProcessDefinition, ProcessControlBlock, ProcessState/Type/BlockReason | ProcessDefinitionTest, ProcessControlBlockTest, ComputerTest | Estados/PCB comprobados en módulo6; resultado local confirmado |
| 3 Planificación | FCFS, EDF, RR, prioridades, cambios por comandos | PolicyTest, PreemptiveKernelTest, SchedulingChangeTest, UiIntegrationTest, FinalAcceptanceTest | RR y selección visual módulo6; comparaciones finales medidas en JSON |
| 4 Productor-consumidor | BufferService, tres SimulatedSemaphore, NetworkTransfer, BufferProgram | ProducerConsumerIntegrationTest, BufferNetworkTest, BufferOperationTest | Local12, contadores/sems1/0/1; remoto visual anterior módulo6 |
| 5 Memoria | Ram, MemoryAdmission, reservas de buffer/limpieza | RamTest, MemoryAdmissionTest, BufferSimulationTest | RAM local/buffers verificados en módulos anteriores |
| 6 GUI | Dashboard, formularios, paneles/tablaPCB, registro y controles | UiFormTest, UiControllerTest, UiIntegrationTest, MetricsPanelTest | Run y pestañas local/métricas confirmados por estudiante |
| 7 Configuración/JSON | SimulationConfig, ScenarioJson, JsonConfigurationStore y UiSession | ScenarioJsonTest, ConfigurationStoreTest, UiSessionPersistenceTest, ScenarioControlsTest | Resultado local tras instrucciones de carga; selector/sobrescritura/error interactivos siguen pendientes |
| 8 Métricas/gráfico | MetricsSnapshot/Calculator, esperas, historial y Java2D | PerformanceMetricsTest, SemaphoreWaitMetricsTest, MetricsHistoryTest, MetricsIntegrationTest | CPUglobal50%, completados2, respuesta0.5 y promedioespera6 confirmados |

Toda la suite usa AllTests; Surefire=0 no significa ausencia de comprobaciones. La evidencia anterior detallada permanece en cada documento de verificación de módulos1–8.

## Integración y copia limpia

FinalAcceptanceTest añade96 comprobaciones a la base25454: **25550** en total. Ejecuta cuatro políticas a horizonte12, un productor local/remoto a horizonte8 y dos parejas C/P a horizonte16. El JSON `data/final-comparisons.json` conserva configuraciones, denominadores y resultados exportados de las instantáneas del motor real. El informe lee ese archivo.

Se extrajo `git archive` del commit **774e58054b6541c0896e90b9fec5b676ca4f5cc8** a una carpeta temporal nueva: target no existía. Desde allí, JDK25/Maven ejecutaron `clean package` con caché de dependencias externa explícita. Resultado: **OK:25550 comprobaciones**, **BUILD SUCCESS**, código0,6.248s. Se recompilaron las fuentes de la copia; no se copiaron target ni archivos ignorados. La caché evita una descarga de dependencias, no sustituye la compilación.

Esto prueba independencia de artefactos de construcción locales. No afirma una descarga de Maven Central desde cero ni apertura/Debug/Run de esa copia en NetBeans.

El informe tiene10páginas; el resumen de defensa, exactamente2. Ambos se generaron desde fuentes versionadas y se revisaron visualmente tras renderizar cada página. Los UML de clases/estados tienen fuentes y SVG/PNG, con formato amplio en el informe para conservar legibilidad.

## Pendientes para cerrar aceptación y entrega

Usar el [checklist](../entrega/checklist.md). No se presenta la compilación terminal de un archive como ejecución limpia en NetBeans. El 26 de octubre se publicó por SSH el historial y las 12 ramas en [elioaca01/proyecto-sistemasop](https://github.com/elioaca01/proyecto-sistemasop); release/final-delivery contiene la versión completa. Issues/PR, integración y envío académico siguen pendientes; no se simulan. La integración GitHub requiere acceso a este repositorio privado.
