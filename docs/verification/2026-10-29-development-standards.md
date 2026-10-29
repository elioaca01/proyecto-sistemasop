# Auditoría de estándares, interfaz e informe — 29 de octubre de 2026

Revisión solicitada por el estudiante contra el enunciado original, páginas 9–11, y RF6–8. Estado inicial: commit `adb66d6`, sin cambios locales. Se distingue lo implementado y probado de lo que todavía requiere comprobación manual o integración en GitHub.

## Resultado prioritario

Hay **12 ramas reales**, locales y publicadas en el repositorio privado `elioaca01/proyecto-sistemasop`; `origin/HEAD` es una referencia simbólica y no una decimotercera rama. Se corrigió el recuento anterior de la documentación. Existen `develop`, `main` y ramas funcionales desde los primeros módulos.

El problema obligatorio pendiente es **gestión de tareas e integración**: en GitHub autenticado se observaron **0 Issues abiertos, 0 cerrados, 0 Pull Requests abiertos y 0 cerrados**. El historial local tampoco contiene commits de fusión. El código completo está en `release/final-delivery`; `main` y `develop` conservan `85694c0`, el diseño inicial. Tener ramas no sustituye los Issues ni los PR comentados que pide el enunciado.

## GitHub y desarrollo

| Exigencia del enunciado | Evidencia comprobada | Estado |
| --- | --- | --- |
| Repositorio en GitHub | Acceso SSH y navegación autenticada al repositorio privado | Cumple existencia; acceso de correctores pendiente |
| Ramas por funcionalidad y develop | Doce refs de rama, incluidas develop y ocho feat/* | Cumple |
| Issues para tareas pendientes y errores | Vistas autenticadas de GitHub con 0 abiertos y 0 cerrados | **Falta** |
| Fusiones mediante PR comentados | Vistas autenticadas con 0 abiertos y 0 cerrados; sin merges en Git | **Falta** |
| Mensajes descriptivos y cambios acotados | 85 commits al iniciar esta auditoría, con mensajes feat/fix/test/docs/build y separación por tareas | Mensajes comprobados; el enunciado no fija un máximo numérico de líneas |
| Contribución de integrantes | Un solo autor del historial, elioaca01; proyecto individual declarado | Compatible con desarrollo individual |
| Java posterior a 21 y NetBeans | release=25, pom.xml y nbactions.xml para Run/Debug | Configurado; prueba final limpia en IDE pendiente |
| Restricción de librerías/estructuras | Gson solo JSON streaming; gráfico Java2D; estructuras y ordenamiento propios | Búsqueda de colecciones/DOM/sorters de Java sin coincidencias en producción |

El 29 de octubre, por instrucción expresa del estudiante, se renombraron las ramas de escenarios, métricas y entrega, tanto localmente como mediante la acción Rename branch de GitHub. Nombres actuales y hashes conservados antes de esta actualización documental:

| Rama actual | Commit preservado |
| --- | --- |
| feat/scenario-json | 09ff0aedc0f4f352a02991c2267789bc4c2c7bc1 |
| feat/performance-metrics | e2c489d0b1022c1dce515d746b860ec3ad7da860 |
| release/final-delivery | adb66d6a9fa71053a892cd7b8947129cb05b4b5b |

No se reescribieron commits ni se fusionó código. La rama por defecto sigue siendo la de entrega, ahora con su nombre funcional. Se actualizaron los upstreams y origin/HEAD. [Captura de las ramas en GitHub](images/branches-functional-2026-10-29.png).

Para cerrar el requisito de integración, corresponde registrar pendientes reales y revisar un PR comentado hacia develop; después, un PR de entrega desde develop hacia main. Los PR se crearían con su fecha real. No corresponde inventar Issues/PR históricos ni presentar los planes Markdown como si fueran Issues de GitHub. Esta auditoría no crea Issues, PR ni fusiones que el estudiante solo pidió verificar.

## Interfaz gráfica y validación

| Requisito | Dónde se observa | Evidencia actual |
| --- | --- | --- |
| Proceso CPU, PC/MAR, prioridad y deadline por computador | ComputerPanel, selector de computador e inspector PCB | UiIntegrationTest / ComputerPanelTest |
| Modo usuario/sistema operativo y ciclo global | Última instrucción/modo y encabezado | UiIntegrationTest / UiControllerTest |
| Nuevos, listos, bloqueados con motivo y terminados | Cuatro tablas y detalle de PCB; selección por ID | UiTableModelTest / ComputerPanelTest |
| RAM usada/libre y política modificable | ComputerPanel, comandos de política/quantum | UiIntegrationTest / SchedulingChangeTest |
| Buffers: contenido, capacidad, semáforos y colas | BufferPanel y modelos propios | BufferPanelTest / UiIntegrationTest |
| Registro de decisiones | EventPanel / EventLog | EventPanelTest / EventLogTest |
| Parámetros, altas en ejecución, pausa/paso/reinicio | DashboardPanel, formularios y ConfigurationPanel | UiFormTest / UiControllerTest / UiSessionTest |
| Guardar/cargar inicial JSON | Selector y acciones de sesión fuera del EDT | ScenarioControlsTest / UiSessionPersistenceTest |
| Métricas y un gráfico común de utilización | MetricsPanel / UtilizationChart | MetricsPanelTest / MetricsIntegrationTest |

La validación comparte InputParser y los constructores del modelo: campos vacíos, texto en números, fracciones, negativos, mínimos e overflow se rechazan con mensajes. Las entradas específicas del tipo de proceso se validan antes de enviar la orden. UiFormTest comprueba estos casos; ScenarioControlsTest comprueba cancelación, errores de archivo, conservación de la ejecución y descarte del editor anterior al cargar. Un ordenamiento propio no se debe confundir con Collections.sort: LinkedList/OrderedProcessQueue son clases del proyecto.

La GUI está separada de las decisiones del núcleo: observa snapshots y envía órdenes mediante UiController/UiSession. La suite incluye trazas local/remota con botones y tablas de producción, y layout 960×640, además de los renders ya versionados.

**Límite de esta comprobación:** el 25 de octubre el estudiante confirmó Run en NetBeans y resultados visuales locales con métricas y buffers; esa evidencia se conserva en [aceptación final](2026-10-25-final-acceptance.md). Hoy no se logró enlazar la herramienta a NetBeans/ÁvilaOS por su identidad Java compartida; no se atribuye eso a un fallo de la aplicación ni se afirma una prueba interactiva nueva. Siguen pendientes selector/extensión/sobrescritura, JSON corrupto en ventana, remoto final, cierre, Debug y copia limpia ejecutada desde el IDE. Están desglosados en el [checklist](../entrega/checklist.md).

## Informe y documentación

Se leyó el informe PDF y se renderizaron sus diez páginas. El contenido requerido por el enunciado está presente:

- Página 2: responsabilidades y métodos de las clases principales; enums e interfaces con su uso. No se requiere documentar todo el código.
- Páginas 3–5: ciclo, cuatro políticas, RAM, deadlines, semáforos/red, persistencia y fórmulas con denominadores.
- Página 6: comparación FCFS, EDF, RR y prioridades, con cargas equivalentes, tablas y conclusiones acotadas al experimento.
- Página 7: comparación productor/consumidor local/remoto, latencia, horizontes iguales y límites de atribución de resultados.
- Páginas 8–9: dos UML, de clases y de estados, con fuentes DOT/SVG/PNG en docs/uml.
- Página 10: reproducción en NetBeans, enlace real de GitHub y pendientes explícitos.

El informe se actualizó al 29 de octubre con los nombres funcionales de ramas, el recuento correcto de doce y el resultado de la consulta de Issues/PR. Conserva diez páginas; el resumen de defensa conserva dos. El resumen no sustituye el informe. Se revisó de nuevo la maquetación después de regenerar el PDF.

Se añadió clasificación binaria de PDF para que Git no interprete bytes del documento como texto con espacios sobrantes, resolviendo el pendiente menor registrado en la revisión final. Las guías por módulos 1–9 y el resumen unificado están disponibles. El PDF y las guías no equivalen al envío académico.

## Construcción repetida y cierre

Comando ejecutado el 29 de octubre con JDK 25 y Maven de NetBeans:

```sh
/usr/bin/env JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -o -B -Dmaven.repo.local=.maven-cache clean package
```

Resultado observado: **OK: 25550 comprobaciones**, JAR generado, **BUILD SUCCESS**, código 0, 6.734 s. No se modificó código Java en esta auditoría. Son assertions de AllTests, no 25550 casos independientes; Surefire=0 no sustituye ese bloque. La construcción terminal no se presenta como Debug o aceptación de una copia limpia en NetBeans.

Prioridad antes de la entrega: Issues y PR comentados reales; comprobaciones manuales restantes en NetBeans; acceso de correctores al repositorio privado; PDF y enlace por el canal académico/spreadsheet. La fecha acordada con el estudiante es viernes 30 de octubre antes de las 07:00 de Venezuela. El envío y la defensa corresponden al estudiante.

## Actualización tras instalar el conector en la cuenta correcta

El estudiante completó la instalación de ChatGPT Codex Connector en elioaca01, limitada al repositorio del proyecto. Se comprobó mediante get_repo el acceso y la visibilidad privada. Se registraron tareas actuales en [Issue #1](https://github.com/elioaca01/proyecto-sistemasop/issues/1), aceptación final en NetBeans, y [Issue #2](https://github.com/elioaca01/proyecto-sistemasop/issues/2), acceso de correctores/envío académico. Ambos permanecen abiertos.

Se creó el [PR #3](https://github.com/elioaca01/proyecto-sistemasop/pull/3), release/final-delivery → develop, en borrador, con descripción del resultado, alcance y límites, y comentario de verificación. El recuento cero de la auditoría inicial corresponde al estado anterior; no se alteró retrospectivamente. Las fusiones hacia develop/main siguen pendientes de revisión y aprobación.

Verificación local adicional de 77ff7e2: clean package con JDK25, **25550 comprobaciones**, **BUILD SUCCESS**, código0, 6.447s. No se modificó Java ni se atribuyó este resultado a CI de GitHub. El informe conserva diez páginas tras actualizar el estado de los Issues/PR; el resumen se conserva sin cambios.
