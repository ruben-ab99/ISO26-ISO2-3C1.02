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
Adoptar el flujo **Git Flow**: ramas estables `main` y `develop`, y ramas temporales `feature/<issue>-<descripcion>`. Se prohiben los commits directos a `main` y `develop`. Se requiere Pull Request (PR) obligatorio con validación de CI y aprobación por revisión de un compañero.

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
- **Motivo:** Permite auditar la calidad de forma objetiva con métricas (cobertura $\ge 70\%$, 0 accesos no autorizados, stubs DICOM/HL7).
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

### 🔄 Alternativas Valoradas
1. Utilizar versiones tempranas o no-LTS de Java (ej. Java 25).
2. Permitir que cada miembro del equipo use una versión distinta de JDK.

### 🚀 Motivo y Consecuencias
- **Motivo:** Java 21 LTS garantiza estabilidad, compatibilidad a largo plazo y soporte completo en las GitHub Actions.
- **Consecuencias:** Construcciones reproducibles y sin errores de versión entre desarrolladores.
