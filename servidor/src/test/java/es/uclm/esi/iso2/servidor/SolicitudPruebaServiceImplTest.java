package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.prescripciones.SolicitudPruebaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SolicitudPruebaServiceImplTest {

    private SolicitudPruebaServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new SolicitudPruebaServiceImpl();
    }

    @Test
    @DisplayName("Constructor rechaza servicio de catálogo nulo")
    void constructorConCatalogoNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new SolicitudPruebaServiceImpl(null),
                "Debe exigir una instancia válida de ICatalogoPruebasService");
    }

    @Test
    @DisplayName("Crear solicitud válida recupera datos del catálogo y asigna nivel de prioridad")
    void crearSolicitudPruebaValida() {
        // Preparar
        String paciente = "PAC-01";
        String medico = "MED-05";

        // Ejecutar
        SolicitudPruebaDTO urgente = servicio.crearSolicitudPrueba(
                paciente, medico, "tac-abdo", 1, "Sospecha de apendicitis aguda"
        );
        SolicitudPruebaDTO preferente = servicio.crearSolicitudPrueba(
                paciente, medico, "RM-CRANEO", 2, "Cefalea persistente con signos de alarma"
        );
        SolicitudPruebaDTO ordinaria = servicio.crearSolicitudPrueba(
                paciente, medico, "RX-TORAX", 5, "Control preoperatorio rutinario"
        );

        // Comprobar
        assertNotNull(urgente);
        assertTrue(urgente.getSolicitudId().startsWith("SOL-"));
        assertEquals("TAC-ABDO", urgente.getCodigoPrueba());
        assertEquals("TAC Abdominal con Contraste IV", urgente.getNombrePrueba());
        assertEquals("URGENTE", urgente.getNivelPrioridad());
        assertEquals("PENDIENTE", urgente.getEstado());
        assertNotNull(urgente.getFechaSolicitud());

        assertEquals("PREFERENTE", preferente.getNivelPrioridad());
        assertEquals("ORDINARIA", ordinaria.getNivelPrioridad());
    }

    @Test
    @DisplayName("Crear solicitud con prueba inexistente o dada de baja en catálogo lanza excepción")
    void crearSolicitudConPruebaNoDisponibleEnCatalogo() {
        assertThrows(IllegalStateException.class, () ->
                servicio.crearSolicitudPrueba("PAC-01", "MED-01", "PRUEBA-INVENTADA", 3, "Justificación"));
        assertThrows(IllegalStateException.class, () ->
                servicio.crearSolicitudPrueba("PAC-01", "MED-01", "RX-OBSOLETA", 3, "Justificación"));
    }

    @Test
    @DisplayName("Crear solicitud con parámetros obligatorios inválidos o prioridad fuera de rango lanza excepción")
    void crearSolicitudConParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitudPrueba(null, "MED-1", "RX-TORAX", 3, "Justif"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitudPrueba("  ", "MED-1", "RX-TORAX", 3, "Justif"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitudPrueba("PAC-1", null, "RX-TORAX", 3, "Justif"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitudPrueba("PAC-1", "", "RX-TORAX", 3, "Justif"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitudPrueba("PAC-1", "MED-1", null, 3, "Justif"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitudPrueba("PAC-1", "MED-1", " ", 3, "Justif"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitudPrueba("PAC-1", "MED-1", "RX-TORAX", 0, "Justif"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitudPrueba("PAC-1", "MED-1", "RX-TORAX", 6, "Justif"));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitudPrueba("PAC-1", "MED-1", "RX-TORAX", 3, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.crearSolicitudPrueba("PAC-1", "MED-1", "RX-TORAX", 3, "   "));
    }

    @Test
    @DisplayName("Consultar y listar solicitudes del paciente devuelve las registradas y valida errores")
    void consultarYListarSolicitudesPaciente() {
        // Preparar
        SolicitudPruebaDTO s1 = servicio.crearSolicitudPrueba("PAC-10", "MED-01", "ECO-ABDO", 4, "Dolor abdominal");
        servicio.crearSolicitudPrueba("PAC-10", "MED-02", "RX-TORAX", 3, "Revisión");

        // Ejecutar
        SolicitudPruebaDTO consultada = servicio.consultarSolicitud(s1.getSolicitudId());
        List<SolicitudPruebaDTO> listaPaciente = servicio.listarSolicitudesPaciente("PAC-10");
        List<SolicitudPruebaDTO> listaVacia = servicio.listarSolicitudesPaciente("PAC-99");

        // Comprobar
        assertEquals(s1.getSolicitudId(), consultada.getSolicitudId());
        assertEquals(2, listaPaciente.size());
        assertTrue(listaVacia.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> servicio.consultarSolicitud(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarSolicitud(" "));
        assertThrows(IllegalStateException.class, () -> servicio.consultarSolicitud("SOL-NO-EXISTE"));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarSolicitudesPaciente(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarSolicitudesPaciente("  "));
    }

    @Test
    @DisplayName("Cancelar solicitud cambia estado a CANCELADA y bloquea cancelaciones repetidas")
    void cancelarSolicitudValidaEInvalida() {
        // Preparar
        SolicitudPruebaDTO s1 = servicio.crearSolicitudPrueba("PAC-20", "MED-01", "TAC-ABDO", 2, "Estudio digestivo");

        // Ejecutar
        SolicitudPruebaDTO cancelada = servicio.cancelarSolicitud(s1.getSolicitudId(), "Duplicada por error clínico");

        // Comprobar
        assertEquals("CANCELADA", cancelada.getEstado());
        assertEquals("Duplicada por error clínico", cancelada.getMotivoCancelacion());

        assertThrows(IllegalStateException.class, () -> servicio.cancelarSolicitud(s1.getSolicitudId(), "Otro motivo"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelarSolicitud(null, "Motivo"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelarSolicitud(" ", "Motivo"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelarSolicitud(s1.getSolicitudId(), null));
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelarSolicitud(s1.getSolicitudId(), "  "));
    }
}