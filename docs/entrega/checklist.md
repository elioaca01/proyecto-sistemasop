# Checklist de entrega — ÁvilaOS

## Comprobado

- [x] Build del estudiante: 25454 comprobaciones y BUILD SUCCESS (25 de octubre).
- [x] Arranque y escenario local en ventana real: ciclo12, completados2, CPUglobal50%, respuesta0.5.
- [x] B1 local: producido/consumido1/1, ocupación0, semáforos1/0/1.
- [x] El estudiante confirma espera promedio6 en Métricas → Buffers y esperas.
- [x] Pruebas automáticas de parámetros, archivos inválidos, guardado seguro, reinicio, red, políticas, métricas y cierre; evidencia por módulo.

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

- [ ] Informe PDF, dos UML y resumen de defensa de dos páginas revisados; completar al generarlos.
- [ ] Comparaciones reales de políticas/local-remoto y condiciones documentadas; completar al medirlas.
- [ ] Repositorio GitHub elegido, cuenta conectada y URL real disponible.
- [ ] Subir historial real y ramas; registrar Issues y PR descriptivos sin inventar trabajo de otras personas ni integraciones pasadas.
- [ ] Comprobar acceso de los correctores al repositorio y que README explica la ejecución.
- [ ] Enviar PDF y enlace por el canal académico y agregar al spreadsheet antes del viernes30 a las07:00 de Venezuela. Lo realiza el estudiante.
- [ ] Practicar defensa individual con el resumen y abrir las guías por módulo ante dudas.

No publicar ni enviar automáticamente en nombre del estudiante. El informe y el ZIP local no equivalen a envío académico.
