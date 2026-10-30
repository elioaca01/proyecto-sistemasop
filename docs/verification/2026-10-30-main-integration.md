# Integración y casos manuales — 30 de octubre de 2026

El estudiante autorizó completar develop/main y preparar los casos de prueba con resultados esperados. Se conservó la carpeta de NetBeans y el historial real, sin rebase ni squash y sin borrar ramas.

## Integración

- [PR #3](https://github.com/elioaca01/proyecto-sistemasop/pull/3), release/final-delivery → develop, fusionado como **ea43398615c9ad6f408b7838cdb006a26d71b722**.
- [PR #4](https://github.com/elioaca01/proyecto-sistemasop/pull/4), develop → main, preparado con descripción, comentarios de validación y autorización del estudiante. El estado/commit final y la verificación posterior a la fusión se registran en la conversación del PR.
- Main es la rama destinada a la entrega; los Issues #1 y #2 siguen abiertos porque integrar código no equivale a aceptación manual ni envío académico.

## Guía y escenarios

La [guía de16 casos](../entrega/casos-prueba-manuales.md) proporciona preparación, pasos, resultados esperados y casillas sin marcar para la ejecución real en NetBeans. Se añadieron ocho JSON en examples/manual/: cuatro cargas equivalentes por política, admisión con RAM8, deadline en CPU, deadline bloqueado y un escenario vacío para altas/validaciones.

Los ocho archivos se cargaron con JsonConfigurationStore y se ejecutaron con Simulation y su reloj/worker reales. Resultados observados por API:

- RAM: tras pasos1–5, Nuevos=1,1,0,0,0; terminados=0,0,1,1,2; RAM usada=8,8,8,8,0. B puede admitirse en el ciclo en que A termina.
- Deadline CPU a2 pasos: completados0, vencidos1, pendientes0, CPUglobal50%.
- Deadline bloqueado a2: completados0, vencidos1, pendientes0, CPUglobal25%, buffer vacío, una espera cerrada de1 ciclo.
- Políticas a12: completados FCFS/EDF/RR/prioridades=2/3/1/3; vencidos=2/1/3/1; CPUglobal58.33/62.50/62.50/62.50%; respuesta0/2/1.25/2.
- Vacío a1: sin procesos; CPU0 y respuesta sin denominador.

La sonda fue temporal y no añadió dependencias, colecciones Java ni código al simulador. La suite existente conserva25550 comprobaciones. Clean package previo a integrar, JDK25/Maven: **25550**, **BUILD SUCCESS**, código0,6.431s. Se comprobó el diff y el informe actualizado de10páginas; las páginas sin cambios se compararon por píxeles con la versión anterior. El resumen de defensa de2páginas no se modificó.

Los casos de ventana real permanecen sin marcar hasta que el estudiante los ejecute y registre el resultado. Una prueba por API o una compilación de terminal no se presenta como aceptación en NetBeans.
