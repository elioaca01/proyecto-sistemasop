# Módulo 7 — Guardar y cargar escenarios JSON

## Qué hicimos

Agregamos **Cargar escenario** y **Guardar escenario inicial** en Configuración inicial. El archivo contiene duración del ciclo, latencia, computadores con RAM/política/quantum, buffers y definiciones de procesos. Cargar un archivo válido sustituye la ejecución y la deja pausada en ciclo 0.

Guardar usa el escenario inicial **aplicado**: incluye cambios anteriores al primer Paso/Iniciar; excluye borradores y cambios realizados durante la ejecución. No guarda PC, colas, tokens actuales ni métricas. Puedes guardar durante la ejecución; para cargar debes pausar primero.

## Conceptos esenciales

| Concepto | Qué debes entender |
| --- | --- |
| Escenario / checkpoint | El escenario describe cómo empezar otra ejecución. Un checkpoint permitiría continuar el estado exacto; este módulo guarda escenarios. |
| JSON versionado | Texto UTF-8 con campos definidos. `formatVersion: 1` identifica el formato que sabemos interpretar. |
| Codec streaming | Gson lee/escribe tokens; las estructuras del simulador siguen siendo propias, sin mapas ni listas de Gson. |
| Validación previa | Primero se lee y valida todo; solo entonces se cierra el motor anterior. Un archivo rechazado conserva ciclo y escenario. |
| Guardado atómico | Se escribe y cierra un temporal junto al destino y luego se reemplaza. Si falla antes de reemplazar, el archivo anterior queda intacto. |
| E/S fuera del EDT | El hilo de sesión procesa los archivos; Swing sigue atendiendo eventos. Encolar una orden todavía no significa completarla. |

Los seis campos raíz y todos los campos de cada objeto son obligatorios. Se rechazan duplicados, campos desconocidos, null, referencias inválidas y números fuera de rango. Los enteros se escriben sin comillas, fracción ni exponente. Los ejemplos completos están en `examples/`.

## Dónde está en el código

- `ScenarioJson`: contrato del formato 1 y conversión a `SimulationConfig`; reutiliza las reglas del modelo.
- `JsonConfigurationStore`: archivos UTF-8, temporal y reemplazo atómico; no administra motores.
- `UiSession`: serializa guardar/cargar con las otras acciones y comprueba el cierre antes de instalar otro motor.
- `SwingScenarioFilePicker`: selector JSON, extensión `.json` y confirmación de sobrescritura.
- `DashboardPanel` / `UiController`: controles y avisos de resultado; cargar limpia el contexto de la generación anterior.

## Ejemplo para explicarlo

Carga `examples/local-producer-consumer.json` y avanza doce pasos: se produce y consume un elemento. Guarda, vuelve a cargar ese archivo y observa ciclo 0; al repetir doce pasos obtienes otra vez el mismo resultado. El archivo conserva las definiciones iniciales, no el elemento ni los PCB de la primera ejecución.

## Tres preguntas probables

- **¿Por qué no se guardan los procesos agregados después de iniciar?** Son cambios de esa ejecución. Exportamos el escenario inicial aplicado, que también usa Reiniciar; así se puede repetir la carga original.
- **¿Qué ocurre si el JSON está dañado?** Se informa el archivo y el motivo sin sustituir el motor. La simulación sigue pausada en el mismo ciclo y puede recibir otro Paso.
- **¿Por qué no escribir directamente encima del archivo?** Un fallo podría truncarlo. Escribimos un temporal completo, lo cerramos y después pedimos reemplazo atómico. Si el sistema no lo admite, informamos el fallo; no prometemos resistencia a un corte eléctrico.

**Explicación breve:** «Persistimos la configuración inicial en JSON, con versión y validación estricta. La sesión lee fuera de Swing y reemplaza el motor únicamente después de validar. Al guardar usamos un temporal para proteger el archivo anterior. Cargar comienza otra ejecución pausada desde cero».

## Prueba manual en NetBeans

La [evidencia técnica](../verification/2026-10-23-scenarios-json.md) separa las pruebas automáticas de la aceptación nativa y del IDE. Al cerrar su implementación se observaron **25234 comprobaciones**; la [guía del módulo8](modulo-8-metricas.md) indica el total posterior.

1. **Clean and Build:** buscar `OK: N comprobaciones` y `BUILD SUCCESS`. La primera construcción necesita conexión para descargar Gson 2.14.0 desde Maven Central.
2. **Run → Configuración inicial → Cargar escenario:** elegir `examples/local-producer-consumer.json`. Debe quedar pausada en ciclo 0. Cancelar el selector no cambia nada.
3. Pulsar **Paso** doce veces. En Buffers y semáforos: producidos/consumidos **1/1**, ocupación **0**, vacíos/llenos/mutex **1/0/1**. Hay dos procesos terminados y queda una unidad de RAM para B1.
4. **Guardar escenario inicial:** usar un nombre de prueba sin extensión. Debe añadir `.json`; repetir el guardado debe pedir confirmación de sobrescritura. Volver a cargarlo deja ciclo 0 y permite repetir el paso anterior.
5. En una copia del archivo, borrar el final del JSON. Cargar esa copia estando pausado debe mostrar un error y conservar el ciclo anterior; **Paso** debe seguir funcionando.
6. Cargar `examples/remote-producer.json`; observar computador 2. Tras ocho pasos, PC **1,1,1,2,3,4,5,6**. En el segundo/tercer paso está bloqueado por Red. Al final B1 contiene un elemento, producido **1** y RAM del productor liberada.
7. Cargar `examples/io-and-scheduling.json` para explorar las cuatro políticas y E/S. Durante **Iniciar**, Guardar sigue disponible y Cargar queda deshabilitado; Pausar permite cargar de nuevo. Cerrar debe terminar el proceso.

La suite ejecutó estos archivos mediante paneles y botones reales en modo headless. La aceptación interactiva del selector todavía está pendiente: al retomar, la herramienta quedó sin responder; tampoco se ha confirmado este módulo desde el IDE del estudiante.
