# ÁvilaOS

Proyecto 1 de Sistemas Operativos: simulador de sistema operativo distribuido.

Requiere JDK 25 y Apache NetBeans. El desarrollo sigue el [diseño aprobado](docs/superpowers/specs/2026-10-18-avilaos-design.md) y la [hoja de ruta](docs/superpowers/plans/2026-10-19-avilaos-roadmap.md).

## Abrir y ejecutar en NetBeans

1. En **File > Open Project**, seleccionar esta carpeta, `proyecto-sistemas-op`. NetBeans reconocerá el archivo `pom.xml`.
2. En **Tools > Java Platforms**, registrar Temurin 25 si no aparece. En este Mac está en `/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home`.
3. En las propiedades del proyecto, **Build > Compile**, seleccionar JDK 25 como plataforma Java. El `pom.xml` fija además `release=25`.
4. Ejecutar **Clean and Build**. La salida debe mostrar `javac [debug release 25]`, `OK: N comprobaciones` y `BUILD SUCCESS`.
5. Ejecutar **Run Project** para abrir la ventana ÁvilaOS. La acción **Debug Project** inicia el mismo programa con el depurador de NetBeans.

La primera construcción descarga plugins de Maven y necesita conexión. Los fuentes del módulo no tienen dependencias externas de ejecución.

## Verificación desde terminal

Con JDK 25 seleccionado y Maven disponible: `mvn test`, `mvn package` y `mvn compile exec:exec@run-app`.

En este Mac, Maven viene incluido en NetBeans. Este comando selecciona Java 25 solo para su ejecución y usa una caché local ignorada por Git:

```sh
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home \
  '/Applications/Apache NetBeans.app/Contents/Resources/netbeans/java/maven/bin/mvn' \
  -Dmaven.repo.local=.maven-cache test
```

Para empaquetar, sustituir `test` por `package`; para abrir la ventana, por `compile exec:exec@run-app`. La aplicación se ejecuta en una JVM separada de Maven para que su cierre no termine el proceso de construcción.

Las pruebas son ejecutables Java con comprobaciones propias; no requieren JUnit ni `-ea`. Maven puede mostrar `Tests run: 0` en el bloque de Surefire: el resultado de nuestras pruebas aparece después, en `exec (core-tests)`, como **`OK: N comprobaciones`**. Si una comprobación falla, la construcción también falla.

## Alcance actual

- Lista enlazada con ordenamiento estable, cola FIFO y buffer circular propios.
- Definiciones validadas de CPU bound, I/O bound, productores y consumidores.
- PCB con estados, PC/MAR, contexto, memoria asignada, contadores de servicio y reglas de deadline.
- Ventana inicial de Swing. El motor de simulación, planificadores, memoria real del clúster, E/S y semáforos simulados pertenecen a los siguientes módulos.

`ProcessDefinition` conserva los atributos iniciales inmutables; `ProcessControlBlock` mantiene el estado vivo del proceso. Apropiar o bloquear conserva PC/MAR y la RAM asignada; ejecutar una instrucción avanza PC/MAR. El núcleo del siguiente módulo decidirá cuándo invocar estas operaciones y controlará la RAM real.

El deadline es relativo al ciclo de llegada. Llegar en ciclo 5 con plazo 1 permite ejecutar en ciclo 5 y vence al cierre de ese ciclo si el proceso no termina. Los bloqueos no detienen el avance del plazo global.
