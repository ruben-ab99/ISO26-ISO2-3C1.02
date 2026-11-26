# 🏛️ Registro de Decisiones de Arquitectura (DECISIONS.md)

Este documento registra de forma cronológica y trazable las Decisiones de Arquitectura de Software (ADR) adoptadas en el proyecto del **Sistema Integral de Gestión de Radiodiagnóstico e Imagen Médica (SESCAM)**.

---

## 📌 ADR-001: Proyecto Maven multimódulo con POM padre, servidor y cliente

- **Fecha:** 19/10/2026
- **Iteración:** It0
- **Origen / Trazabilidad:** Acta 1 · PR #3 · Issue #3
- **Estado:** Aprobado

### 🎯 Contexto y Problema
El enunciado exige una arquitectura cliente-servidor con posibilidad de soportar distintos tipos de clientes y componentes backend que puedan evolucionar de forma independiente.

### 💡 Decisión Tomada
Crear una estructura Maven multimódulo con un POM padre (`radiodiagnostico`) que centraliza las versiones y dependencias, y dos submódulos principales: `servidor` (lógica de negocio e integración SESCAM) y `cliente` (interfaz de usuario).

### 🔄 Alternativas Valoradas
1. Tener repositorios independientes para cliente y servidor.
2. Un único módulo monolítico Maven.

### 🚀 Motivo y Consecuencias
- **Motivo:** Permite construir y verificar todo el proyecto con un único comando (`mvn clean verify`) asegurando coherencia en las versiones.
- **Consecuencias:** Desacoplamiento entre capa de presentación y backend, control estricto del ciclo de vida de Maven y necesidad de mantener alineado el POM padre.

---

## 📌 ADR-002: Módulo de contratos e interfaces desacopladas

- **Fecha:** 25/10/2026
- **Iteración:** It0
- **Origen / Trazabilidad:** Acta 2 · Plan de Configuración (1.4) · Issue #3
- **Estado:** Aprobado

### 🎯 Contexto y Problema
El caso de uso de calidad CU-29 exige modularidad y mantenibilidad, garantizando que los módulos no dependan directamente de las implementaciones concretas de otros módulos.

### 💡 Decisión Tomada
Aplicar el principio de *diseño por contrato*. Las interfaces Java y DTOs compartidos se definen primero (diseño por el desarrollador humano), y las clases concretas implementan dichas interfaces (desarrolladas por programadores o asistidas por IA).

### 🔄 Alternativas Valoradas
1. Invocación directa entre clases concretas de distintas capas.
2. Definición de contratos en tiempo de compilación dentro del propio módulo ejecutor.

### 🚀 Motivo y Consecuencias
- **Motivo:** Cumple el principio de inversión de dependencias de SOLID e impide que cambios en la implementación rompan otros módulos.
- **Consecuencias:** Facilidad para aplicar stubs/mocks en pruebas unitarias e integración segura asistida por agentes de IA.

---

## 📌 ADR-003: Mapeo 1 Requisito Funcional : 1 Caso de Uso (24 CUs funcionales)

- **Fecha:** 19/10/2026
- **Iteración:** It0
- **Origen / Trazabilidad:** Acta 1 · Issue #4 · Wiki Requisitos y casos de uso
- **Estado:** Aprobado

### 🎯 Contexto y Problema
El catálogo del SESCAM enumera 21 funcionalidades por actor. Existen además necesidades transversales no explícitas como autenticación global, visualización DICOM y catálogo de pruebas.

### 💡 Decisión Tomada
Asociar 1 Requisito Funcional a 1 Caso de Uso (CU-01 a CU-21) e incorporar 3 casos de uso transversales funcionales: **CU-22 Autenticarse (SSO)** (inclusión en todos los CU), **CU-23 Visualizar imágenes DICOM** (extensión de CU-09) y **CU-24 Consultar catálogo de pruebas** (inclusión en CU-06).

### 🔄 Alternativas Valoradas
1. Agrupar múltiples requisitos funcionales en un único caso de uso complejo.
2. Considerar la autenticación como un mero detalle de implementación sin CU.

### 🚀 Motivo y Consecuencias
- **Motivo:** Mantiene la trazabilidad directa 1:1 entre los requisitos del enunciado y el diseño funcional.
- **Consecuencias:** Se formalizan 24 casos de uso funcionales. CU-22 se prioriza en Elaboración (It1) al ser prerrequisito de seguridad.

---

## 📌 ADR-004: Agrupación de Casos de Uso por módulo Maven e iteración

- **Fecha:** 31/10/2026
- **Iteración:** It0
- **Origen / Trazabilidad:** Acta 3 · Issue #10 · Wiki PUD (supuesto 4)
- **Estado:** Aprobado

### 🎯 Contexto y Problema
Ejecutar 24 iteraciones individuales (1 CU : 1 iteración) para casos de uso pequeños (de 4 a 11 horas) generaba una sobrecarga inasumible de gestión, reuniones y releases.

### 💡 Decisión Tomada
Agrupar los casos de uso que comparten dominio y prioridad dentro de una misma iteración que produce un módulo Maven cohesionado (7 iteraciones de construcción/elaboración, It1 a It7, más It0 e ItN).

### 🔄 Alternativas Valoradas
1. Realizar 24 iteraciones independientes de 1 CU cada una.
2. Hacer solo 2 iteraciones gigantescas (una para backend y otra para frontend).

### 🚀 Motivo y Consecuencias
- **Motivo:** Minimiza la sobrecarga de gestión sin perder la trazabilidad por caso de uso en la estimación de horas.
- **Consecuencias:** Releases funcionales incrementales (v0.1.0 a v0.7.0) con alta cohesión y mínimo acoplamiento entre iteraciones.

---

## 📌 ADR-005: Supuestos de equipo polivalente y reparto del trabajo Humano / IA

- **Fecha:** 31/10/2026 (Actualizado 06/11/2026)
- **Iteración:** It0
- **Origen / Trazabilidad:** Acta 3 · Issues #10 y #17 · Wiki PUD
- **Estado:** Aprobado

### 🎯 Contexto y Problema
El equipo consta de 6 integrantes con tarifa de 20 €/h. Se requiere planificar la dedicación y el apoyo de herramientas de IA manteniendo el control de calidad.

### 💡 Decisión Tomada
Establecer un equipo polivalente donde **quien implementa un módulo nunca lo prueba**. Los desarrolladores humanos deciden, diseñan las interfaces y auditan; los agentes de IA ayudan a implementar y generar pruebas unitarias.

### 🔄 Alternativas Valoradas
1. Especialización fija de roles (programador puro vs. tester puro).
2. Generación y fusión directa de código por IA sin intervención ni revisión humana.

### 🚀 Motivo y Consecuencias
- **Motivo:** Cumple la normativa académica y optimiza los tiempos de desarrollo (-20% R/A/D, -55% implementación, -40% pruebas).
- **Consecuencias:** Cobertura de pruebas obligatoria y control de calidad riguroso entre pares.

---

## 📌 ADR-006: Reserva de contingencia del 15% y licencias de IA a 0 €

- **Fecha:** 31/10/2026
- **Iteración:** It0
- **Origen / Trazabilidad:** Acta 3 · Wiki PUD (supuestos 8 y 9) · Planes de Gestión
- **Estado:** Aprobado

### 🎯 Contexto y Problema
Es necesario estimar el presupuesto del proyecto corrigiendo el sesgo de optimismo de las herramientas de IA y contemplando los costes de licencias.

### 💡 Decisión Tomada
Añadir una reserva de contingencia del 15% en horas sobre el desarrollo funcional y emplear herramientas de IA con licencias educativas o gratuitas (Copilot, Gemini, Claude), fijando el coste de licencias en 0 €.

### 🔄 Alternativas Valoradas
1. Contratar licencias comerciales pagadas.
2. No incluir margen de contingencia para imprevistos.

### 🚀 Motivo y Consecuencias
- **Motivo:** Evita desviaciones presupuestarias y absorbe el tiempo consumido en reuniones y correcciones de código.
- **Consecuencias:** Presupuesto acotado estrictamente a horas/hombre (20 €/h).

---

## 📌 ADR-007: Estrategia de ramificación Git Flow y gestión de configuración

- **Fecha:** 19/10/2026
- **Iteración:** It0
- **Origen / Trazabilidad:** Acta 1 · Plan de Configuración (1.5) · Issue #1
- **Estado:** Aprobado

### 🎯 Contexto y Problema
Un equipo de 6 personas trabajando sobre la misma base de código requiere reglas claras para evitar conflictos de integración y cambios no auditados.

### 💡 Decisión Tomada
Adoptar el flujo **Git Flow**: ramas estables `main` y `develop`, y ramas temporales `<issue>-cu<xx>-<descripcion>-<modulo>-v<version>`. Se prohiben los commits directos a `main` y `develop`. Se requiere Pull Request (PR) obligatorio con validación de CI y aprobación por revisión de un compañero.

### 🔄 Alternativas Valoradas
1. Commit directo a la rama `main` (Trunk-Based Development sin revisión).
2. Integración semanal por envío de parches.

### 🚀 Motivo y Consecuencias
- **Motivo:** Garantiza que el código en `develop` y `main` siempre compile y haya pasado pruebas.
- **Consecuencias:** Trazabilidad total de cada cambio vinculado a un Issue de GitHub (`Closes #X`).

---

## 📌 ADR-008: Requisitos no funcionales como casos de uso de calidad (ISO 25010)

- **Fecha:** 02/11/2026
- **Iteración:** It0
- **Origen / Trazabilidad:** Acta 3 · Issues #14, #15, #16 · Wiki Planes de Gestión
- **Estado:** Aprobado

### 🎯 Contexto y Problema
El modelo ISO 25010 exige incorporar la calidad (*Quality by Design*) desde la fase de análisis y diseño, manteniendo la trazabilidad con las iteraciones del PUD.

### 💡 Decisión Tomada
Formalizar 5 casos de uso de calidad transversales con pesos definidos: **CU-25 Seguridad (30%)**, **CU-26 Fiabilidad (25%)**, **CU-27 Compatibilidad (20%)**, **CU-29 Mantenibilidad (15%)** y **CU-28 Portabilidad (10%)**.

### 🔄 Alternativas Valoradas
1. Tratar los requisitos no funcionales como simples notas al pie de página.
2. Dejar la verificación de calidad para la iteración final (ItN).

### 🚀 Motivo y Consecuencias
- **Motivo:** Permite auditar la calidad de forma objetiva con métricas (cobertura $\ge 75\%$, 0 accesos no autorizados, stubs DICOM/HL7).
- **Consecuencias:** Los CUs de calidad se evalúan continuamente durante las iteraciones de desarrollo.

---

## 📌 ADR-009: Auditoría humana obligatoria del código generado por IA ("Man in the Loop")

- **Fecha:** 19/10/2026
- **Iteración:** It0
- **Origen / Trazabilidad:** Acta 1 · Plan de Configuración (1.9) · Plan de Calidad
- **Estado:** Aprobado

### 🎯 Contexto y Problema
El uso de asistentes de IA corre el riesgo de introducir alucinaciones, código no optimizado, vulnerabilidades de seguridad o deuda técnica no detectada.

### 💡 Decisión Tomada
Establecer la norma estricta de que **la IA propone y el humano audita**. Ningún código, test o configuración generado por IA se integrará en la rama principal sin haber sido ejecutado localmente (`mvn clean verify`) e inspeccionado por el equipo.

### 🔄 Alternativas Valoradas
1. Aceptar las sugerencias de la IA sin verificación previa.
2. Prohibir totalmente el uso de IA en la asignatura.

### 🚀 Motivo y Consecuencias
- **Motivo:** Asegura la calidad del producto final y el aprendizaje activo del alumnado.
- **Consecuencias:** El proceso de revisión humana impone un límite de velocidad que evita la acumulación de deuda técnica.

---

## 📌 ADR-010: Stack tecnológico unificado y ejecutable (Java 21 LTS)

- **Fecha:** 31/10/2026
- **Iteración:** It0
- **Origen / Trazabilidad:** Acta 2 · Enunciado de la asignatura · README.md
- **Estado:** Aprobado

### 🎯 Contexto y Problema
Evitar discrepancias entre entornos de desarrollo locales (Windows/Linux/Mac) y el entorno de integración continua (GitHub Actions).

### 💡 Decisión Tomada
Fijar **Java 21 LTS** como versión estándar del compilador y runtime en el POM padre y en los workflows de CI (`maven.yml`), utilizando Maven 3.x, JUnit 5 para pruebas unitarias y Visual Paradigm para el modelado UML/SysML.
Integración de JaCoCo (0.8.12) y maven-site-plugin en el ciclo de construcción para la auditoría automática de una cobertura minima de 75%.

### 🔄 Alternativas Valoradas
1. Utilizar versiones tempranas o no-LTS de Java (ej. Java 25).
2. Permitir que cada miembro del equipo use una versión distinta de JDK.

### 🚀 Motivo y Consecuencias
- **Motivo:** Java 21 LTS garantiza estabilidad, compatibilidad a largo plazo y soporte completo en las GitHub Actions.
- **Consecuencias:** Construcciones reproducibles y sin errores de versión entre desarrolladores.


---

## 📌 ADR-011: Baja lógica (Soft Delete) para el ciclo de vida de pacientes (CU-12)

- **Fecha:** 16/11/2026

- **Iteración:** It3

- **Origen / Trazabilidad:** Issue CU-12 · Wiki Casos de Uso · Plan de Calidad (CU-26)

- **Estado:** Aprobado


### 🎯 Contexto y Problema
En el caso de uso CU-12 (Gestionar pacientes) es necesario permitir la baja de pacientes en el sistema, pero un paciente puede tener asociados estudios radiológicos (DICOM), citas o informes médicos previos que no deben quedar huérfanos.

### 💡 Decisión Tomada
Implementar una baja lógica (*Soft Delete*) mediante el atributo booleano `activo = false` y el registro obligatorio de un `motivoBaja` en `PacienteDTO`, impidiendo futuras modificaciones sobre fichas inactivas pero conservando sus datos en el repositorio.

### 🔄 Alternativas Valoradas
1. Borrado físico del registro (`remove` en memoria o base de datos).

2. Permitir eliminar pacientes únicamente si no tienen historial previo, bloqueando la baja en caso contrario.

### 🚀 Motivo y Consecuencias
- **Motivo:** Garantiza la integridad referencial y la trazabilidad clínica exigidas por el atributo de fiabilidad (CU-26) sin perder el histórico radiológico del SESCAM.

- **Consecuencias:** Se evita la pérdida accidental de datos clínicos y duplicados de DNI activos, a costa de tener que filtrar el estado `activo` en las operaciones de alta y modificación.


---

## 📌 ADR-012: Inyección de dependencias por constructor para aislamiento en pruebas unitarias

- **Fecha:** 16/11/2026

- **Iteración:** It3

- **Origen / Trazabilidad:** Issue CU-12 · ADR-002 · Plan de Calidad (CU-29)

- **Estado:** Aprobado


### 🎯 Contexto y Problema
Los servicios de negocio desarrollados a partir de la Iteración 3 (como `GestorPacientesServiceImpl`) dependen transversalmente del control de acceso y roles (`IGestorRolesService`) implementado en iteraciones anteriores.

### 💡 Decisión Tomada
Inyectar las interfaces de servicios colaboradores (`IGestorRolesService`) a través del constructor de la implementación en `servidor`, prohibiendo instanciaciones directas (`new`) acopladas a clases concretas.

### 🔄 Alternativas Valoradas
1. Instanciar directamente `new GestorRolesServiceImpl()` dentro del servicio de pacientes.

2. Utilizar variables estáticas o patrones Singleton globales para acceder a la gestión de roles.

### 🚀 Motivo y Consecuencias
- **Motivo:** Refuerza el principio de inversión de dependencias (ADR-002) y facilita el aislamiento de componentes exigido en mantenibilidad (CU-29).

- **Consecuencias:** Permite emplear *stubs* ligeros en JUnit 5 (`GestorPacientesServiceImplTest`) para alcanzar una cobertura $\ge 70\%$ sin depender de la configuración real de usuarios ni tokens SSO.

---

## 📌 ADR-013: Discretización de agendas médicas en tramos horarios con bloqueo lógico (CU-14)

- **Fecha:** 17/11/2026
- **Iteración:** It4
- **Origen / Trazabilidad:** Issue #55 · CU-14 · Plan de Calidad (CU-26)
- **Estado:** Aprobado

### 🎯 Contexto y Problema
El caso de uso CU-14 requiere configurar la disponibilidad diaria de los médicos y permitir el bloqueo de franjas horarias por incidencias o sesiones clínicas sin perder la trazabilidad ni permitir reservas sobre agendas cerradas.

### 💡 Decisión Tomada
Modelar cada jornada en `AgendaDiariaDTO` como una lista ordenada de tramos discretos (`TramoHorarioDTO`) generados automáticamente según la duración configurada por hueco, aplicando bloqueo lógico (`disponible = false` con `motivoBloqueo`) y cierre de agenda (`abierta = false`) que impide futuras modificaciones.

### 🔄 Alternativas Valoradas
1. Almacenar únicamente la hora de inicio y fin de la jornada calculando huecos dinámicamente en cada consulta.
2. Eliminar físicamente los tramos bloqueados de la lista de la agenda.

### 🚀 Motivo y Consecuencias
- **Motivo:** Simplifica la detección de solapamientos, conserva el motivo de auditoría de cada bloqueo y evita inconsistencias al asignar citas.
- **Consecuencias:** La duración de los huecos queda fijada en la apertura de la agenda diaria y cualquier bloqueo requiere coincidir con un tramo disponible existente.

---

## 📌 ADR-014: Ordenación por prioridad clínica + FIFO e integración por interfaz en listas de espera (CU-15)

- **Fecha:** 23/11/2026
- **Iteración:** It4
- **Origen / Trazabilidad:** Issue #58 · CU-15 · ADR-002 · ADR-012
- **Estado:** Aprobado

### 🎯 Contexto y Problema
En el CU-15 (Gestionar listas de espera y huecos) es necesario priorizar pacientes según urgencia médica y asignarles huecos libres en las agendas del CU-14 sin acoplar `GestorListasEsperaServiceImpl` a la implementación interna de agendas.

### 💡 Decisión Tomada
Definir el enumerado `PrioridadEspera` con pesos ordenables (`URGENTE(1)`, `PREFERENTE(2)`, `ORDINARIA(3)`), ordenar las consultas pendientes por nivel de prioridad y orden cronológico de registro (`fechaRegistro` - FIFO), e inyectar `IGestorAgendasService` por constructor para reservar y bloquear el hueco de forma atómica al atender la solicitud.

### 🔄 Alternativas Valoradas
1. Ordenación estrictamente cronológica (FIFO puro) sin niveles de prioridad clínica.
2. Duplicar el estado de disponibilidad de las agendas dentro del servicio de listas de espera.

### 🚀 Motivo y Consecuencias
- **Motivo:** Garantiza equidad clínica dentro de cada nivel de urgencia y mantiene una única fuente de verdad sobre los huecos libres de agenda.
- **Consecuencias:** La asignación de un hueco marca automáticamente la solicitud como `atendida = true`, impidiendo reasignaciones duplicadas.

---

## 📌 ADR-015: Automatización de métricas de cobertura de código con JaCoCo en Maven

- **Fecha:** 17/11/2026
- **Iteración:** It4
- **Origen / Trazabilidad:** Issue #55 · ADR-008 · Plan de Calidad (CU-29)
- **Estado:** Aprobado

### 🎯 Contexto y Problema
El ADR-008 establece como criterio de calidad un umbral mínimo de cobertura de código ($\ge 75\%$) en las pruebas unitarias, pero ejecutar únicamente JUnit con Surefire no proporciona métricas verificables ni informes de cobertura por clase.

### 💡 Decisión Tomada
Configurar `jacoco-maven-plugin` (`0.8.12`) en el `pom.xml` padre enlazado al ciclo de vida de `maven-surefire-plugin` (`prepare-agent`, `report` en fase `test` y `check` en fase `verify`) junto con `maven-site-plugin` para generar informes HTML navegables.

### 🔄 Alternativas Valoradas
1. Revisar la cobertura manualmente solo desde el IDE local de cada desarrollador.
2. Posponer la medición de cobertura hasta la iteración final de transición (ItN).

### 🚀 Motivo y Consecuencias
- **Motivo:** Permite auditar objetivamente en cada Pull Request que los nuevos servicios superan el umbral de calidad exigido.
- **Consecuencias:** Cualquier cambio en el POM padre obliga a mantener sincronizado `${argLine}` entre JaCoCo y Surefire.

---

## 📌 ADR-016: Generación periódica de citas y cancelación en cascada en seguimientos clínicos (CU-07)

- **Fecha:** 23/11/2026
- **Iteración:** It5
- **Origen / Trazabilidad:** Issue #71 · CU-07 · ADR-002 · ADR-012
- **Estado:** Aprobado

### 🎯 Contexto y Problema
El caso de uso CU-07 (Prescribir prueba de seguimiento) requiere pautar controles periódicos (ej. nódulos, revisiones oncológicas o cribados) generando automáticamente la necesidad de nuevas citas (`CU-07 <<include>> CU-13`) sin acoplar el módulo `prescripciones` a la implementación interna de `citacion` ni dejar citas huérfanas si se anula el seguimiento.

### 💡 Decisión Tomada
Inyectar por constructor las interfaces `ICatalogoPruebasService` e `IGestionCitasService` en `PrescripcionSeguimientoServiceImpl`, calcular automáticamente las fechas de revisión (`fechaInicio + i * periodicidadDias`) programando cada cita a través del contrato de citación, almacenar sus identificadores en `PrescripcionSeguimientoDTO` y propagar la cancelación en cascada a las citas asociadas cuando se anula el seguimiento.

### 🔄 Alternativas Valoradas
1. Instanciar directamente `GestionCitasServiceImpl` dentro de `prescripciones` o duplicar el almacenamiento de citas.
2. Registrar únicamente el seguimiento sin invocar el servicio de citación, dejando las citas sin programar.

### 🚀 Motivo y Consecuencias
- **Motivo:** Mantiene el desacoplamiento entre dominios exigido por CU-29 (ADR-002), permite probar el servicio de forma aislada con stubs/implementaciones en memoria (ADR-012) y garantiza consistencia clínica entre seguimientos y citas.
- **Consecuencias:** La programación de un seguimiento valida previamente que la prueba exista y esté activa en el catálogo (CU-24) y requiere disponibilidad horaria para cada fecha calculada.

---

## 📌 ADR-017: Consultas de solo lectura como métodos `default` en contratos de otros dominios (CU-11, CU-16)

- **Fecha:** 26/11/2026
- **Iteración:** It7
- **Origen / Trazabilidad:** CU-11 · CU-16 · ADR-002 · ADR-012
- **Estado:** Propuesto

### 🎯 Contexto y Problema
Los informes de la It7 necesitan datos que hoy solo existen dentro de otros módulos: las citas y agendas de un periodo (CU-16, Generar informes operativos) y los seguimientos prescritos por un médico (CU-11, Consultar estadísticas de solicitudes). Los contratos `IGestionCitasService`, `IGestorAgendasService` e `IPrescripcionSeguimientoService` no exponen esas consultas, y añadir métodos abstractos rompería la compilación de todos los stubs que ya usan los tests de sus responsables.

### 💡 Decisión Tomada
Añadir a esos contratos tres consultas de solo lectura como métodos `default` que devuelven `List.of()`: `listarCitasEntreFechas(desde, hasta)`, `listarAgendasEntreFechas(desde, hasta, tokenSSO)` y `listarSeguimientosMedico(medicoId)`. Las implementaciones reales (`GestionCitasServiceImpl`, `GestorAgendasServiceImpl`, `PrescripcionSeguimientoServiceImpl`) las sobrescriben filtrando sus datos en memoria, con tests propios (`ConsultasPeriodoCitasAgendasTest`, `ConsultaSeguimientosMedicoTest`). Los servicios de analítica solo dependen de las interfaces.

### 🔄 Alternativas Valoradas
1. Declarar los métodos como abstractos y actualizar todos los stubs de los tests de otros compañeros.
2. Acceder directamente a las implementaciones o a sus estructuras internas desde los servicios de analítica.
3. Duplicar en el módulo de analítica un almacenamiento propio de citas, agendas y seguimientos.

### 🚀 Motivo y Consecuencias
- **Motivo:** Mantiene el desacoplamiento por interfaces (ADR-002) y la compatibilidad con los stubs existentes (ADR-012) sin tocar los tests de otros casos de uso.
- **Consecuencias:** Un stub que no sobrescriba la consulta devuelve una lista vacía, por lo que el informe sale a cero en lugar de fallar. Los responsables de citación, agendas y prescripciones deben mantener estas consultas si cambian su almacenamiento.

---

## 📌 ADR-018: Criterios de cálculo de las métricas de analítica (CU-11, CU-16, CU-21)

- **Fecha:** 26/11/2026
- **Iteración:** It7
- **Origen / Trazabilidad:** CU-11 · CU-16 · CU-21 · ADR-017
- **Estado:** Propuesto

### 🎯 Contexto y Problema
Los informes de analítica calculan medias, porcentajes y tiempos de espera a partir de datos incompletos (solicitudes sin cita, agendas sin tramos, periodos vacíos). Sin un criterio común cada servicio podría dividir entre cero, mezclar unidades o devolver valores distintos para la misma situación, y los tests no tendrían un resultado esperado claro.

### 💡 Decisión Tomada
- **Redondeo y vacíos:** porcentajes y medias se redondean a dos decimales; si no hay elementos el resultado es `0.0`. Los tiempos se miden en días naturales con `LocalDate` y un `Clock` inyectado.
- **Espera hasta cita (CU-11):** a cada solicitud no cancelada se le asigna, por orden de fecha de solicitud, la primera cita no cancelada del mismo paciente y prueba con fecha igual o posterior, sin reutilizar citas. Las solicitudes sin cita cuentan aparte con los días transcurridos hasta hoy.
- **Adherencia al seguimiento (CU-11):** revisiones realizadas / revisiones vencidas (citas no canceladas con fecha ≤ hoy), comprobando la realización en los registros de pruebas. Sin revisiones vencidas la adherencia es del 100 %, porque no hay ninguna incumplida.
- **Ocupación (CU-16):** tramos no disponibles / tramos totales de las agendas del periodo, global y por médico. La espera se mide sobre las solicitudes no atendidas de la lista de espera, y la cancelación como citas canceladas / citas del periodo.
- **Estadísticas globales (CU-21):** se calculan a partir del registro de auditoría y del catálogo, y se exportan en CSV con separador `;` y columnas `seccion;clave;valor`.
- **Errores de colaboradores (CU-11):** las excepciones de datos (`IllegalArgumentException`, `IllegalStateException`) de otros servicios no invalidan el informe y ese dato se omite; las `SecurityException` sí se propagan.

### 🔄 Alternativas Valoradas
1. Devolver `NaN` o lanzar excepción cuando no hay datos.
2. Considerar 0 % de adherencia cuando aún no ha vencido ninguna revisión.
3. Abortar el informe completo ante cualquier error de un servicio colaborador.

### 🚀 Motivo y Consecuencias
- **Motivo:** Da resultados deterministas y comparables entre informes y permite fijar en los tests valores exactos con un reloj controlado.
- **Consecuencias:** Un informe puede mostrar ceros cuando faltan datos, por lo que debe leerse junto a sus contadores (`totalTramos`, `revisionesVencidas`, etc.). Cualquier cambio de criterio exige actualizar este ADR y los tests asociados.
