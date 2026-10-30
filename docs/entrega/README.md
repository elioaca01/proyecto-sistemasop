# Preparar y comprobar la entrega

Para confirmar el funcionamiento en la ventana real, sigue los [16 casos de prueba manuales](casos-prueba-manuales.md). Incluyen ocho escenarios adicionales en examples/manual/ y resultados concretos para comparar.

El ZIP contiene exclusivamente los archivos guardados en el commit actual: fuentes, pom.xml, ejemplos, documentación, UML y ambos PDF. Excluye cachés, target y el historial .git. El repositorio GitHub conserva ese historial aparte.

Desde una copia del repositorio con Git y Python3, guardar primero los cambios y ejecutar:

```sh
python3 docs/entrega/build_package.py
```

El resultado está en `output/entrega/`, con el commit en el nombre y `manifest.json` con los hashes SHA256 del ZIP y de los PDF. El directorio de salida está ignorado para que el ZIP no se incluya dentro de sí mismo. El script exige un árbol de trabajo limpio y lee los PDF del commit, no versiones locales sin guardar.

Para usar el ZIP recibido:

1. Descomprimir en una carpeta nueva y abrirla como proyecto Maven en NetBeans.
2. Registrar y seleccionar JDK25. No hacen falta las rutas del Mac del autor.
3. Ejecutar Clean and Build con conexión para descargar Gson/plugins si es la primera construcción; después Run Project.
4. Verificar `OK: 25550 comprobaciones` y `BUILD SUCCESS`. Surefire puede mostrar cero tests; las comprobaciones propias se ejecutan en el bloque `core-tests` posterior.
5. Revisar `output/pdf/`, `examples/` y el [checklist manual](checklist.md). La compilación por terminal de una copia limpia ya tiene evidencia; la apertura de esa copia en el IDE requiere confirmación manual.

Para comparar la descarga, calcular SHA256 del ZIP (por ejemplo, `shasum -a 256 archivo.zip`) y cotejarlo con manifest.json. El manifiesto identifica la revisión empaquetada; no es evidencia de envío académico ni de acceso de los correctores a GitHub.
