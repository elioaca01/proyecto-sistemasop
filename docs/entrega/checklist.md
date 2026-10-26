# Checklist de entrega — ÁvilaOS

## Comprobado

- [x] Build del estudiante: 25454 comprobaciones y BUILD SUCCESS (25 de octubre).
- [x] Arranque y escenario local en ventana real: ciclo12, completados2, CPUglobal50%, respuesta0.5.
- [x] B1 local: producido/consumido1/1, ocupación0, semáforos1/0/1.
- [x] El estudiante confirma espera promedio6 en Métricas → Buffers y esperas.
- [x] Pruebas automáticas de parámetros, archivos inválidos, guardado seguro, reinicio, red, políticas, métricas y cierre; evidencia por módulo.
- [x] Copia limpia por git archive de 774e580, sin target previo: clean package con JDK25, 25550 comprobaciones y BUILD SUCCESS. Esto verifica construcción por terminal.

## Prueba manual final en NetBeans

- [ ] Guardar escenario inicial con nombre sin extensión; comprobar .json y confirmación de sobrescritura. Usar un archivo de prueba.
- [ ] Cargar ese guardado: pausa y ciclo0; repetir local12.
- [ ] En una copia del JSON, borrar el final; cargarla estando pausado. Debe conservar ciclo/escenario y permitir otro Paso.
- [ ] Cargar remoto: computador2 PC1,1,1,2,3,4,5,6 tras ocho pasos; CPUglobal37.5%, producido1, sin episodios de semáforo.
- [ ] Desplazar tabla para ver deadlines/Jain y cambiar tamaño de ventana; comprobar que los controles sigan accesibles.
- [ ] Cerrar por ventana: comprobar que termine la ejecución de Maven. No confundir ocultar con cerrar.
- [ ] Debug Project: poner breakpoint en Kernel.onTick y comprobar que detiene/reanuda.
- [ ] Abrir una copia limpia del paquete en NetBeans, seleccionar JDK25, Clean and Build y Run. La caché/dependencias se descargan desde Maven si es la primera vez.

## Documentos y repositorio

- [x] Informe PDF de10páginas, dos UML y resumen de defensa de2páginas generados y revisados visualmente.
- [x] Comparaciones reales de políticas/local-remoto medidas con cargas equivalentes y condiciones documentadas.
- [x] Revisión independiente final sin hallazgos críticos/importantes; mejora menor de clasificación binaria de PDF registrada para después.
- [x] Empaquetador probado y ZIP local con fuentes versionadas, PDF y manifest.json de hashes.
- [x] El estudiante creó el repositorio privado con el nombre definitivo proyecto-sistemasop y proporcionó su URL SSH.
- [x] GitHub conectado a elioaca01 y creación privada autorizada por el estudiante.
- [x] Repositorio real: https://github.com/elioaca01/proyecto-sistemasop. Acceso SSH comprobado; consulta sin autenticación HTTP404, coherente con repositorio privado.
- [x] Subir historial real y las 13 ramas, incluida codex/final-delivery con la versión completa. Sin reescribir historia ni fusionar ramas.
- [ ] Habilitar este repositorio en la integración GitHub: actualmente responde404 aunque el acceso SSH funciona. Registrar Issues/PR descriptivos e integrar ramas con aprobación, sin inventar integraciones pasadas.
- [ ] Comprobar acceso de los correctores al repositorio y que README explica la ejecución.
- [ ] Enviar PDF y enlace por el canal académico y agregar al spreadsheet antes del viernes30 a las07:00 de Venezuela. Lo realiza el estudiante.
- [ ] Practicar defensa individual con el resumen y abrir las guías por módulo ante dudas.

El estudiante autorizó preparar el repositorio privado indicado. El envío académico lo realiza el estudiante; el informe y el ZIP local no equivalen a ese envío.
