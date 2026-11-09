# 🏛️ Registro de Decisiones de Arquitectura (DECISIONS.md)

Este documento registra todas las decisiones arquitecturales, técnicas y de diseño tomadas durante el desarrollo del sistema del SESCAM, justificando su impacto en el Proceso Unificado de Desarrollo (PUD).

---

## 📌 ADR-001: Configuración de Arquitectura Multimódulo en Maven

* **Fecha:** 19/10/2026
* **Estado:** Aprobado

### 🎯 Contexto y Problema
El enunciado del proyecto exige una arquitectura cliente-servidor para dar soporte a distintos tipos de clientes y gestión de un hospital. Además, se requiere un desarrollo desacoplado mediante el Proceso Unificado de Desarrollo (PUD), donde los componentes evolucionen de forma independiente.

### 💡 Decisión Tomada
Se acuerda estructurar el proyecto mediante un **esquema multimódulo en Maven** compuesto por:
1. **Proyecto Padre (POM raíz):** Centraliza la gestión de versiones, plugins y dependencias globales del proyecto.
2. **Módulo Servidor (`servidor` / backend):** Contiene la lógica de negocio, controladores, modelos de dominio e integración con los servicios centrales del SESCAM.
3. **Módulo Cliente (`cliente` / frontend):** Contiene la interfaz gráfica y cliente de la aplicación.

### 🚀 Consecuencias e Impacto
* **Ventajas:**
  * Minimiza el acoplamiento entre la capa de presentación y la lógica de negocio (cumpliendo con el modelo MVC y la programación orientada a interfaces).
  * Simplifica la compilación, empaquetado y gestión de dependencias desde la raíz del proyecto.
  * Permite que la IA implemente los contratos de forma aislada en cada módulo sin romper el resto de la aplicación.
* **Inconvenientes / Retos:**
  * Requiere un control estricto de la estructura de paquetes y del ciclo de vida de Maven por parte de los expertos designados.

---

## 📌 ADR-002: Catálogo de 24 casos de uso funcionales (21 del enunciado + 3 transversales)

* **Fecha:** 19/10/2026 · **Iteración:** It0
* **Origen:** issue [#4](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/issues/4) · wiki [Requisitos y casos de uso](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/wiki/Requisitos-y-casos-de-uso)
* **Estado:** Aprobado

### 🎯 Contexto y Problema
El enunciado enumera 21 funcionalidades agrupadas por tipo de usuario. Hay necesidades que no aparecen como viñeta pero que el sistema exige: identificar al usuario, visualizar imágenes DICOM desde la web y consultar el catálogo de tipos de prueba.

### 💡 Decisión Tomada
* Cada viñeta del enunciado es un requisito funcional y un caso de uso (1 RF : 1 CU): CU-01 a CU-21.
* Se añaden tres casos de uso transversales: **CU-22 Autenticarse (SSO)**, requisito previo de todos los demás; **CU-23 Visualizar imágenes DICOM**, que extiende CU-09 (el enunciado pide visores compatibles con DICOM); y **CU-24 Consultar catálogo de pruebas**, base de CU-06 y mantenido por el administrador (CU-17).

### 🚀 Consecuencias e Impacto
* 24 casos de uso funcionales con trazabilidad directa al enunciado.
* CU-22 condiciona la seguridad de todo el sistema, por lo que se planifica en la primera iteración de Elaboración.

---

## 📌 ADR-003: Requisitos de calidad como casos de uso transversales (CU-25 a CU-29, ISO 25010)

* **Fecha:** 02/11/2026 · **Iteración:** It0
* **Origen:** [Acta 3](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/wiki/Acta-3) · issues [#14](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/issues/14), [#15](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/issues/15), [#16](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/issues/16) · wiki [Planes de Gestión](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/wiki/Planes-de-Gesti%C3%B3n) (apartado 2)
* **Estado:** Aprobado

### 🎯 Contexto y Problema
El enunciado pide derivar requisitos de calidad según ISO 25010 (*quality by design*) e incorporarlos al diseño, manteniendo la trazabilidad con la planificación.

### 💡 Decisión Tomada
Se priorizan cinco características y cada una se formaliza como un caso de uso de calidad transversal, con un peso en el modelo de calidad: **CU-25 Seguridad (30 %)**, **CU-26 Fiabilidad (25 %)**, **CU-27 Compatibilidad (20 %)**, **CU-29 Mantenibilidad (15 %)** y **CU-28 Portabilidad (10 %)**. Adecuación funcional, eficiencia de desempeño y capacidad de uso se vigilan sin caso de uso propio.

### 🚀 Consecuencias e Impacto
* Los casos de uso de calidad no tienen iteración propia: sus horas (31,5 h con IA) se suman a las iteraciones donde se trabajan (reparto en el PUD, apartado «Casos de uso de calidad»).
* Cada característica tiene un criterio medible y un mecanismo de verificación definidos en el Plan de Calidad.

---

## 📌 ADR-004: Agrupar los casos de uso por módulo Maven (n CU : 1 iteración)

* **Fecha:** 31/10/2026 · **Iteración:** It0
* **Origen:** issue [#10](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/issues/10) · wiki [PUD](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/wiki/PUD) (supuesto 4)
* **Estado:** Aprobado

### 🎯 Contexto y Problema
El enunciado sugiere 1 CU : 1 iteración, lo que daría 24 iteraciones de entre 4 y 11 horas, poco operativas con la dedicación del equipo.

### 💡 Decisión Tomada
Cada iteración agrupa los casos de uso de la misma prioridad que forman un módulo Maven cohesionado: 7 iteraciones de casos de uso (It1–It7), más It0 e ItN. Elaboración: It1 (CU-22, CU-18, CU-20) e It2 (CU-09, CU-23, CU-10). Construcción: It3 a It7.

### 🚀 Consecuencias e Impacto
* Módulos con máxima cohesión y mínimo acoplamiento, y releases con sentido funcional (v0.1.0 a v0.7.0).
* La trazabilidad por caso de uso se mantiene en la tabla de horas del PUD.

---

## 📌 ADR-005: Supuestos de planificación (equipo, dedicación, coste y reserva)

* **Fecha:** 31/10/2026 (actualizado el 06/11/2026) · **Iteración:** It0
* **Origen:** issues [#10](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/issues/10) y [#17](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/issues/17) · wiki [PUD](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/wiki/PUD) (secciones 1 y 3)
* **Estado:** Aprobado

### 🎯 Contexto y Problema
Para calcular coste, agenda y fecha final hay que fijar los parámetros del equipo. El enunciado fija It0 = 1.000 € e ItN = 2.000 €, y una semana de 40 h para un consultor.

### 💡 Decisión Tomada
* 6 personas polivalentes, sin especialidad fija, a **20 €/h**.
* Dedicación de **10 h/semana por persona** (60 h/semana de equipo), como dedicación realista de estudiantes. Es una desviación consciente de las 40 h del enunciado.
* Reserva de contingencia del **15 %** sobre las horas funcionales, para cubrir el sesgo de optimismo de la IA y las reuniones.
* It0 e ItN con el coste fijo del enunciado, más las horas de los casos de uso de calidad que se hacen en ellas.

### 🚀 Consecuencias e Impacto
* Resultado: 393,5 h y 7.870 € con IA (491 h y 9.820 € sin IA), con fin del desarrollo previsto el 19/11/2026.
* Si la dedicación real difiere, las fechas se desplazan (ver ADR-007).

---

## 📌 ADR-006: Herramientas y modelos de IA

* **Fecha:** 31/10/2026 · **Iteración:** It0
* **Origen:** wiki [PUD](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/wiki/PUD) (supuestos 7 y 9) · wiki [Planes de Gestión](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/wiki/Planes-de-Gesti%C3%B3n) (apartado 1.9)
* **Estado:** Aprobado

### 🎯 Contexto y Problema
El enunciado permite apoyarse en agentes de IA (AI4SE), pero exige que los humanos decidan, diseñen las interfaces y auditen, y que se consideren los costes de licencias.

### 💡 Decisión Tomada
* Se usan GitHub Copilot y Gemini con licencia de estudiante y Claude con plan gratuito: **coste de licencias 0 €**.
* Los humanos deciden en requisitos, análisis y diseño; los agentes implementan y ejecutan pruebas. Estimación del efecto: −20 % en R, A y D; −55 % en implementación (revisión humana incluida); −40 % en pruebas.
* Ningún agente aprueba ni fusiona: todo código generado pasa por pull request revisado por un humano distinto del autor.

### 🚀 Consecuencias e Impacto
* El desarrollo funcional baja de 301,5 h a 184,45 h (hipótesis a recalibrar con horas reales).
* Se anotará qué modelo genera cada módulo para contrastar la hipótesis del −55 %.

---

## 📌 ADR-007: Plan de recuperación por el retraso de la It0

* **Fecha:** 08/11/2026 · **Iteración:** It0 → It1
* **Origen:** wiki [PUD](https://github.com/ruben-ab99/ISO26-ISO2-3C1.02/wiki/PUD) (sección 6)
* **Estado:** Propuesto, pendiente de ratificar en el Acta 4

### 🎯 Contexto y Problema
A 8/11 no hay código de ninguna iteración de construcción: la It0 se prolongó hasta noviembre y la reunión de arranque fue el 18/10. Quedan 340,5 h del plan, con la misma fecha final (15/12) que las prácticas de pruebas y mantenimiento.

### 💡 Decisión Tomada
Subir la dedicación a **15 h/semana por persona** desde el 9/11: It1 el 11/11, It2 el 13/11 … It7 el 25/11 e ItN el 3/12. Punto de control el 22/11: si It1 a It4 no están cerradas, se reduce el alcance de It6 e It7.

**Alternativas valoradas:** mantener 10 h/semana (fin hacia el 16/12, después de la entrega) o subir a 20 h/semana (fin hacia el 27/11, el doble de la dedicación planificada).

### 🚀 Consecuencias e Impacto
* Quedan casi dos semanas para las prácticas de pruebas y mantenimiento.
* Las horas reales se anotan en cada issue desde el 9/11 para recalibrar.

---
