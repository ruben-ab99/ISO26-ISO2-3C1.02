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