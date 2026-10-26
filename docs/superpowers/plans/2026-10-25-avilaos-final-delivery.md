# ÁvilaOS — Module 9: Integration and Delivery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** reunir evidencia de integración, comparaciones reproducibles, informe PDF con UML y resumen de defensa de dos páginas; preparar la entrega sin fingir los pendientes externos.

**Architecture:** pruebas Java ejecutan el simulador real y exportan mediciones JSON; documentación y diagramas se derivan del código actual. Un generador de documentación local produce PDFs con esos resultados. La aplicación Java no incorpora otra dependencia.

**Tech Stack:** Java25/Maven/Swing/Gson existentes; Graphviz instalado para UML; ReportLab/pypdf/Poppler del runtime de documentos para generar y revisar PDFs. Las herramientas de informe no son dependencias de ejecución del simulador.

**Spec:** diseño aprobado `docs/superpowers/specs/2026-10-18-avilaos-design.md` §10 y enunciado pp10–11. Continúa el trabajo solicitado en la misma carpeta NetBeans, método nativo con una revisión final. Rama `codex/final-delivery`, base `e2c489d`. No crear/fusionar/publicar un repositorio sin resolver la selección del usuario. Envío académico y spreadsheet pertenecen al estudiante.

## Global Constraints

- Mantener código de simulación Java25, Thread/Semaphore, estructuras propias y única dependencia Gson. No reescribir comportamiento para mejorar resultados de las comparaciones.
- Desarrollo individual: no inventar otros integrantes, contribuciones, Issues, PR ni evidencias de ejecución.
- Separar evidencia automática, confirmación manual del estudiante y pendientes. Build25454 y escenario local12/CPU50/2completados/espera6 confirmados por el estudiante el25. Esto no prueba Debug, copia limpia en el IDE, guardado/sobrescritura/JSONinválido interactivos ni cierre actual.
- Comparar políticas con carga, memoria, horizontes y asignación iguales; definir qué cambia. Local/remoto con misma definición de productor y buffer, cambiando solo localización del productor y latencia efectiva.
- Informe detalla clases/métodos principales, enums, interfaces y dos UML (clases y estados). Conclusiones limitadas a las cargas medidas, sin proclamar una política universalmente superior.
- Resumen final de defensa: dos páginas, sintetiza guías1–9 y no las copia completas.
- PDFs deben renderizarse y revisarse visualmente, con texto legible y resultados concordantes con JSON. No incluir capturas personales completas del escritorio en el informe.

## Review Focus

1. Denominadores y respuestas sesgadas por procesos que nunca reciben CPU: resultados medidos y conclusiones sin confundir menor promedio con mejor atención.
2. Igualdad de condiciones de las cuatro políticas y local/remoto: no confundir costo de red con cambios de carga o comparar horizontes distintos sin explicarlo.
3. UML frente al código: composición real, interfaces usadas, IO antes/después de última instrucción, deadline desde NEW y red antes de despacho.
4. Copia limpia/documentación de ejecución: no exigir rutas privadas ni cachés preexistentes para el destinatario; no prometer pruebas de IDE, PR o envío no realizados.
5. PDFs/paquete final: cifras, número de páginas de resumen, diagramas legibles, enlaces y pendientes explícitos; regeneración de resultados sin colecciones prohibidas en Java.

## Task 1: evidencia y matriz de cobertura

**Files:** crear `docs/verification/2026-10-25-final-acceptance.md`, `docs/entrega/checklist.md`; actualizar evidencias7/8 y roadmap.

- [x] Registrar literalmente lo comprobado por el estudiante: build25454/BUILD SUCCESS y capturas23:03, valores locales visibles y confirmación textual de promedio6. Conservar hashes/nombres de capturas sin copiarlas al repo ni publicar el resto del escritorio.
- [x] Matriz RF1–8 con archivos/pruebas que los ejercitan y validación manual parcial. Checklist práctico pendiente de guardado/recarga/corrupto/remoto/cierre/Debug/copia limpia y GitHub; no marcar entrega total completada.
- [x] Verificar estado de Git/remoto y dependencias de generación; commit `docs: registra evidencia manual y cobertura de entrega`.

## Task 2: experimentos reproducibles e integración final

**Files:** crear `src/test/java/avilaos/tests/FinalAcceptanceTest.java`, modificar AllTests; crear `docs/verification/data/final-comparisons.json` con CLI de la misma clase.

**Interfaces:** `FinalAcceptanceTest.run()` verifica resultados del simulador; `main(String[] args)` exporta JSON streaming UTF8 a args[0]. No generar el archivo durante la suite ordinaria.

- [x] Escribir primero comprobaciones de cuatro políticas bajo carga idéntica: computador1 A8/D9/prio2, B2/D3/prio0, C4/D7/prio1; computador2 D6/D20/prio1. RAM64 cadauno, quantum2, llegada0, medir12 ciclos. Esperados completados FCFS2, EDF3, RR1, prioridades3; vencidos2/1/3/1. Medir servicio, respuesta/población, Jain y cumplimiento desde MetricsSnapshot real. Si pasan inicialmente, no inventar RED.
- [x] Productor6/RAM2/plazo100/intervalo1/cuota1 y B1(host1,cap1), dos computadores, latencia2: local productor1 completa en6 pasos y remoto productor2 en8; a horizonte común8 servicio6 para ambos, producido1, consumido0. Local se queda inactivo los dos ciclos adicionales; distinguir makespan de utilización en horizonte común. Añadir pareja C/P en horizonte16: C1/P1 local y C1/P2 remoto; terminación normal esperada en12/13, espera6/7, explicando disponibilidad de CPU y solapamiento además de la latencia.
- [x] Exportar bases/fórmulas/definiciones y condiciones al JSON, sin colecciones ni resultados escritos a mano. Suite y CLI satisfactorios; commit `test: mide politicas y red con cargas equivalentes`.

## Task 3: dos UML derivados del código

**Files:** crear `docs/uml/{clases,estados}.dot`, SVG y PNG correspondientes.

- [ ] Leer clases/métodos reales y dibujar vista selectiva de clases, con composición Computer y relación de workers/reloj, PCB, políticas y servicio de buffers. Leyenda distingue dependencias de llamadas, composición y realización de interfaces.
- [ ] Estados NEW/READY/RUNNING/BLOCKED/TERMINATED; admisión, apropiación, IO/semáforo/red, red previa al despacho y deadline desde cualquier estado activo. Permitir terminar al concluir IO si no quedan instrucciones.
- [ ] Generar con dot; inspeccionar PNGs y comprobar nombres/transiciones contra fuentes. Commit `docs: documenta clases y estados con UML`.

## Task 4: informe y defensa unificada

**Files:** crear `docs/informe/build_report.py`, `docs/informe/README.md`, `output/pdf/avilaos-informe.pdf`, `output/pdf/avilaos-defensa-resumen.pdf`; `docs/defensa/modulo-9-integracion.md` y actualizar índice.

- [ ] Generar informe en español, autor individual Elio Colmenares, con alcance, arquitectura/clases/métodos, enums/interfaces, ciclo global, políticas/RAM, buffers/red, persistencia/métricas, dos UML, pruebas y conclusiones basadas en el JSON. Pendientes externos explícitos; no inventar URL de GitHub, institución ni número de estudiante.
- [ ] Generar resumen exactamente2páginas con recorrido oral del sistema, conceptos, fórmulas, ejemplo local, preguntas clave y demo breve. Mantener guías por módulo para ampliar.
- [ ] Usar PDF skill: marcar operación antes de primera autoría, renderizar todas las páginas con Poppler y revisar imágenes, texto/paginación/cortes, cifras y legibilidad; corregir distribución hasta aprobar. Commit `docs: prepara informe PDF y resumen de defensa`.

## Task 5: copia limpia, revisión y preparación final

**Files:** documentación de entrega/README/roadmap, evidencia final y paquete de entrega local reproducible.

- [ ] git archive de una revisión comprometida a directorio temporal limpio; Maven desde esa copia con caché explícita externa comprueba independencia de target y archivos no versionados. Esto es construcción terminal, no comprobación de esa copia en NetBeans.
- [ ] Una revisión independiente del rango con plan/spec/Review Focus/ledger, incluidos JSON y UML/PDF; corregir Important/Critical en un pase, regresiones para cambios de comportamiento y render de PDFs para cambios documentales. Sin segunda revisión; menores registrados explícitos.
- [ ] Construcción final, PDF completo/resumen2páginas, enlaces/diffcheck/códigoJava sin colecciones. Empaquetar fuente versionada e informe, con instrucciones y checksums, excluyendo cachés/target/datos personales. No incluir el ZIP dentro de sí mismo.
- [ ] Conservar rama/carpeta. Publicación GitHub, Issues/PR, URL real y pruebas manuales que faltan permanecen pendientes hasta ejecutarse; pedir la información necesaria tras preparar lo independiente. No declarar el proyecto entregado por generar el paquete local.

## Verificación

Comando base: JDK25 explícito y Maven de NetBeans con `-o -B -Dmaven.repo.local=.maven-cache test` o clean package. Éxito requiere OK:N, BUILD SUCCESS y código0. CLI FinalAcceptanceTest con classpathScope=test y args rutaJSON. Autoría/render de PDF usa runtime local indicado por load_workspace_dependencies. Revisión del plan: RF1–8 matrices y experimentos en1/2; informe/UML/defensa en3/4; copia limpia y honestidad de entrega en5. Carga comparativa fija y mismos horizontes evitan conclusiones injustificadas.
