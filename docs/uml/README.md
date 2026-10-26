# Dos diagramas UML

- `clases.dot` es la fuente de la vista selectiva de clases principales. CPU, RAM y controlador de E/S figuran como atributos de Computer; la vista no pretende enumerar todo el código.
- En clases: rombo relleno = composición; línea dirigida = asociación; discontinua = dependencia; triángulo vacío = realización de interfaz. Las multiplicidades se escriben junto a la relación. BufferService gestiona BoundedBuffer a través de BufferRegistry; no se dibuja una propiedad directa ficticia.
- `render_states.py` define una sola tabla de estados/transiciones y sus posiciones; produce `estados.dot` (topología), `estados.svg` y `estados.png`. Se fijó el trazado para evitar que el diseño automático cruzara etiquetas.
- En estados: rojo discontinuo = deadline desde cualquiera de los cuatro estados activos. Una operación IO puede terminar el proceso al completarse, si ya no quedan instrucciones. La red puede bloquear antes del despacho, sin instrucción ni primera respuesta.

Los SVG permiten ampliar sin perder resolución. Los PNG de alta resolución se incluyen en el informe.

Para regenerar desde cualquier carpeta:

```sh
python3 /ruta/al/proyecto/docs/uml/render_all.py
```

La herramienta requiere Graphviz y Pillow; para rasterizar estados busca Arial o DejaVu Sans en ubicaciones habituales de macOS/Linux/Windows. Estas son herramientas de documentación, no dependencias de ÁvilaOS. Las imágenes ya generadas están versionadas y el simulador no ejecuta estos scripts.
