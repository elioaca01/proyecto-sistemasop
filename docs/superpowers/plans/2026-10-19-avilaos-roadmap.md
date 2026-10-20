# ÁvilaOS — hoja de ruta y cobertura

**Diseño aprobado:** [especificación](../specs/2026-10-18-avilaos-design.md).

**Objetivo:** todos los requisitos del Proyecto 1, informe y demostración listos para el lunes 26 de octubre; martes 27 como margen de correcciones. La fecha académica es el viernes 30, antes de las 07:00 de Venezuela.

El proyecto se divide en módulos. Cada módulo tendrá un plan ejecutable con archivos, interfaces y pruebas, escrito contra el código que exista al comenzar ese módulo. Esta hoja de ruta conserva la cobertura total; no presenta los módulos posteriores como si ya estuvieran implementados o diseñados al nivel de cada método.

## Secuencia

| Módulo | Resultado verificable | Dependencias | Requisitos del enunciado |
| --- | --- | --- | --- |
| 1. Base y procesos | Proyecto Maven ejecutable en NetBeans, ventana básica, estructuras propias, enums, definición de procesos y PCB probado | JDK 25 y NetBeans instalados | RF 1: enums; RF 2: modelo y PCB; restricciones técnicas |
| 2. Un computador | CPU, RAM, núcleo, FCFS, admisión y terminación; procesos CPU bound e I/O bound con motivos de bloqueo | 1 | RF 1: componentes e interfaz de reloj; RF 2: estados/deadline; RF 3: FCFS; RF 5: memoria |
| 3. Políticas | EDF, RR y prioridades apropiativas; quantum y cambio de política en límites de ciclo | 2 | RF 3 completo; RF 7: políticas y quantum |
| 4. Clúster y reloj | Al menos dos instancias de Computer, hilo por computador, reloj coordinador, comandos e instantáneas protegidos por semáforos Java | 2–3 | RF 1: arquitectura, hilos y exclusión mutua; RF 3: asignación manual y sin migración |
| 5. Productor-consumidor | Buffers acotados en RAM, tres semáforos simulados por buffer, varios participantes, red y limpieza por deadline | 4 | RF 4 completo; RF 5: reserva RAM de buffers; instrucciones de núcleo del anexo |
| 6. Interfaz completa | Configuración, formularios, CPU/PCB/colas/RAM por máquina, buffers/semáforos, log, pausa/paso/reinicio y cambios en ejecución | 4–5; ventana básica desde 1 | RF 6 completo; RF 7: controles en ejecución y validación de entradas |
| 7. Escenarios JSON | Guardar y cargar parámetros, procesos y buffers con validación completa y sustitución segura | 5–6 | RF 7: persistencia y carga futura |
| 8. Métricas | Contadores por máquina/globales, fórmulas comprobadas, contadores por buffer y gráfico conjunto de utilización | 4–7 | RF 8 completo |
| 9. Integración y defensa | Pruebas integradas, ejecución limpia en NetBeans, informe PDF, dos diagramas UML y conclusiones comparativas | 1–8 | Documentación, entrega y defensa |

La interfaz y la documentación avanzan junto con el núcleo. El módulo 6 completa la GUI, no marca su primer día de trabajo.

## Planes disponibles

- [Módulo 1: base, estructuras y procesos](2026-10-19-avilaos-foundation.md): código implementado y 212 comprobaciones satisfactorias. El estudiante confirmó el arranque visual desde NetBeans mediante una captura; cierre, recompilación limpia en el IDE y Debug permanecen pendientes.
- [Módulo 2: un computador](2026-10-19-avilaos-single-computer.md): CPU, RAM, admisión, FCFS, E/S y deadlines implementados; 509 comprobaciones acumuladas y construcción limpia satisfactorias. [Guía de defensa](../../defensa/modulo-2-computador.md). La GUI conserva la ventana inicial.

## Reglas de avance

1. Escribir la prueba de comportamiento, comprobar el fallo esperado, implementar y comprobar el resultado.
2. Usar una rama por funcionalidad y commits descriptivos; integrar por PR cuando esté conectado GitHub.
3. Al cerrar cada módulo, conservar evidencia de pruebas y una explicación breve para la defensa.
4. Si aparece una contradicción del enunciado, registrarla en el diseño con la interpretación elegida antes de aplicarla.
5. No marcar un requisito como cumplido por tener una clase vacía, una ventana decorativa o una prueba que no lo ejercita.

## Comprobaciones transversales

- Java posterior a 21: JDK objetivo 25; seleccionar explícitamente esa plataforma porque la terminal aún usa Java 17 por defecto.
- Ninguna colección de Java para almacenar procesos, PCB, eventos o buffers; estructuras y ordenamiento propios también en las pruebas del proyecto.
- Distinguir semáforos Java de coordinación real y semáforos de ÁvilaOS con colas de PCB.
- Cada computador avanza una vez por ciclo global; un proceso bloqueado no detiene el reloj.
- Toda finalización libera RAM y cualquier reserva pendiente de buffer, incluso por deadline.
- Nunca actualizar Swing desde los workers ni modificar el modelo desde las tablas de la GUI.
- La prueba final incluye una copia limpia ejecutada en NetBeans, no solo compilación desde terminal.

## Pendientes externos

- Elegir o crear el repositorio GitHub y disponer de autenticación. Existen Git local, `main` y `develop`; aún no existe un remoto registrado.
- El envío académico, la inscripción en el spreadsheet y la asistencia a la defensa corresponden al estudiante. No se programan envíos ni automatizaciones.
