# Informe y resumen de defensa

Fuentes editables: `informe.md` y `../defensa/resumen-unificado.md`. El generador inserta tablas de `../verification/data/final-comparisons.json` y las dos imágenes UML de `../uml/`. No modifica el simulador ni añade dependencias a pom.xml.

```sh
python3 docs/informe/build_report.py
```

Requiere ReportLab y pypdf. Produce `output/pdf/avilaos-informe.pdf` y `output/pdf/avilaos-defensa-resumen.pdf`; comprueba que el resumen tenga exactamente dos páginas. El texto normal usa A4; los dos UML usan páginas A3 para conservar legibilidad. Los SVG se pueden ampliar aparte. La fecha visible corresponde al estado documentado del proyecto; la generación binaria usa metadatos invariantes.

Para regenerar mediciones después de compilar tests:

```sh
mvn exec:java -Dexec.mainClass=avilaos.tests.FinalAcceptanceTest -Dexec.classpathScope=test -Dexec.args=docs/verification/data/final-comparisons.json
```

Seleccionar JDK25 y Maven antes de usar ese comando. El JSON se exporta solo después de comprobar los resultados de los ocho experimentos. No se usa un benchmark escrito a mano. Para regenerar los UML, consultar `../uml/README.md`.

Tras generar los PDF, renderizar todas las páginas con `pdftoppm` y revisar legibilidad, tablas y diagramas. La validación pypdf no sustituye la inspección visual. No publicar un informe con URL GitHub inventada ni afirmar pruebas manuales que todavía no consten.
