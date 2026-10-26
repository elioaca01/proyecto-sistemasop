# Módulo 9 - Integración, informe y defensa

## Qué hicimos

Reunimos la cobertura RF1–8, la evidencia manual del escenario local y comparaciones reproducibles del simulador. El informe explica las clases/métodos, enums/interfaces, reloj, políticas, RAM, sincronización y fórmulas; incluye UML de clases y estados. El resumen unificado tiene exactamente dos páginas y conserva las guías individuales para ampliar.

## Conceptos esenciales

- **Condiciones equivalentes:** misma carga, asignación, memoria y horizonte al comparar políticas. Solo cambia la política.
- **Población del promedio:** respuesta usa quienes recibieron CPU. Un promedio menor puede ocultar procesos nunca atendidos.
- **Horizonte y finalización:** medir hasta un ciclo fijo permite comparar utilización, pero hay que informar también cuándo termina cada carga.
- **Integración:** no basta una prueba por clase. Los experimentos recorren comandos, workers, núcleo, buffers y métricas reales.
- **Evidencia:** distinguir automatización, confirmación del estudiante y pendientes; no convertir un build terminal en prueba de Debug/IDE.

## Resultados que debes poder explicar

Con A8/D9, B2/D3 y C4/D7 en computador1, más D6/D20 en computador2, a horizonte12: FCFS/EDF/RR/prioridades completan **2/3/1/3** procesos. EDF/prioridades ordenan igual en esta carga. FCFS muestra respuesta0, pero respondieron solo2/4; no es prueba de mejor atención global.

El productor aislado local/remoto termina en6/8 con seis instrucciones. A horizonte8 ambos dan CPUglobal37.5%. La pareja C/P local/remota termina en12/13: usar otro computador permite que P empiece antes, compensando parte de la latencia. A horizonte16 ambas parejas ejecutan doce instrucciones y dan CPUglobal37.5%; espera de semáforo6/7.

## Tres preguntas probables

- **¿Cuál política es mejor?** Depende de la carga y del objetivo. Aquí EDF y prioridades completan más plazos cortos; no se extrapola a otros quantums/cargas.
- **¿Qué evidencia sustenta las conclusiones?** FinalAcceptanceTest ejecuta el motor real, comprueba resultados y exporta el JSON que leen las tablas del informe.
- **¿Ya está entregado el proyecto?** El paquete local y el PDF no son el envío académico. El historial ya está en GitHub; faltan los pasos del checklist que todavía no consten, Issues/PR y acceso de correctores.

**Explicación breve:** «Cerramos el proyecto con pruebas de integración y comparaciones de condiciones iguales. Las conclusiones citan poblaciones y horizontes para no confundir cifras. El informe y los dos UML permiten recorrer el código; el resumen de dos páginas organiza mi defensa».

## Cómo usar los documentos

1. Leer `resumen-unificado.md` y practicar una explicación sin memorizar solo números.
2. Abrir el informe y localizar clases, métricas, comparaciones y ambos UML.
3. Repetir demo local12 y remoto8 en NetBeans. Ver el promedio en Métricas → Buffers y esperas.
4. Completar el [checklist de entrega](../entrega/checklist.md), especialmente archivos, cierre, Debug/copia limpia y GitHub.
5. Consultar la guía del módulo correspondiente cuando una pregunta requiera detalle.

El build del estudiante con25454 y las capturas/local/promedio6 están registrados. La suite final incorporó96 comprobaciones de experimentos: total25550, sujeto a la verificación final del paquete.
