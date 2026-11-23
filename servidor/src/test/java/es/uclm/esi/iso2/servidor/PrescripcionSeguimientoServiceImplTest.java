package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestionCitasService;
import es.uclm.sescam.contratos.prescripciones.PrescripcionSeguimientoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PrescripcionSeguimientoServiceImplTest {

    private IGestionCitasService gestionCitasService;
    private PrescripcionSeguimientoServiceImpl servicio;

    @BeforeEach
    void setUp() {
        gestionCitasService = new GestionCitasServiceImpl();
        servicio = new PrescripcionSeguimientoServiceImpl(
                new CatalogoPruebasServiceImpl(), gestionCitasService, new AuditServiceImpl()
        );
    }

    @Test
    @DisplayName("Prescribir seguimiento válido crea la pauta y genera automáticamente las citas periódicas")
    void prescribirSeguimientoValidoGeneraCitasPeriodicas() {
        // Preparar
        LocalDate inicio = LocalDate.of(2026, 12, 1);

        // Ejecutar
        PrescripcionSeguimientoDTO seg = servicio.prescribirSeguimiento(
                "PAC-001", "MED-010", "tac-abdo", 30, 3, inicio, "Control periódico de nódulo pulmonar"
        );

        // Comprobar
        assertNotNull(seg);
        assertTrue(seg.getSeguimientoId().startsWith("SEG-"));
        assertEquals("TAC-ABDO", seg.getCodigoPrueba());
        assertEquals("ACTIVO", seg.getEstado());
        assertEquals(3, seg.getCitasGeneradasIds().size());

        List<CitaDTO> citasPaciente = gestionCitasService.listarCitasPaciente("PAC-001");
        assertEquals(3, citasPaciente.size());
        assertEquals(LocalDate.of(2026, 12, 1), citasPaciente.get(0).getFecha());
        assertEquals(LocalDate.of(2026, 12, 31), citasPaciente.get(1).getFecha());
        assertEquals(LocalDate.of(2027, 1, 30), citasPaciente.get(2).getFecha());
    }

    @Test
    @DisplayName("Prescribir seguimiento selecciona franja alternativa si la primera está ocupada")
    void prescribirSeguimientoUsaFranjaAlternativaSiHayConflicto() {
        // Preparar: ocupamos la franja 09:00-09:30 para ese médico y fecha
        LocalDate fecha = LocalDate.of(2026, 12, 10);
        gestionCitasService.programarCita("PAC-999", "MED-010", "RX-TORAX", fecha, "09:00-09:30");

        // Ejecutar
        PrescripcionSeguimientoDTO seg = servicio.prescribirSeguimiento(
                "PAC-002", "MED-010", "RX-TORAX", 90, 1, fecha, "Revisión trimestral"
        );

        // Comprobar
        CitaDTO citaGenerada = gestionCitasService.consultarCita(seg.getCitasGeneradasIds().get(0));
        assertEquals("10:00-10:30", citaGenerada.getFranjaHoraria());
    }

    @Test
    @DisplayName("Validación de parámetros inválidos y dependencias nulas lanza excepción")
    void validacionParametrosInvalidos() {
        LocalDate hoy = LocalDate.of(2026, 12, 1);

        assertThrows(IllegalArgumentException.class, () -> new PrescripcionSeguimientoServiceImpl(null, gestionCitasService));
        assertThrows(IllegalArgumentException.class, () -> new PrescripcionSeguimientoServiceImpl(new CatalogoPruebasServiceImpl(), null));

        assertThrows(IllegalArgumentException.class, () -> servicio.prescribirSeguimiento(null, "MED-1", "RX-TORAX", 30, 2, hoy, "Indicación"));
        assertThrows(IllegalArgumentException.class, () -> servicio.prescribirSeguimiento("PAC-1", " ", "RX-TORAX", 30, 2, hoy, "Indicación"));
        assertThrows(IllegalArgumentException.class, () -> servicio.prescribirSeguimiento("PAC-1", "MED-1", "", 30, 2, hoy, "Indicación"));
        assertThrows(IllegalArgumentException.class, () -> servicio.prescribirSeguimiento("PAC-1", "MED-1", "RX-TORAX", 0, 2, hoy, "Indicación"));
        assertThrows(IllegalArgumentException.class, () -> servicio.prescribirSeguimiento("PAC-1", "MED-1", "RX-TORAX", 30, 0, hoy, "Indicación"));
        assertThrows(IllegalArgumentException.class, () -> servicio.prescribirSeguimiento("PAC-1", "MED-1", "RX-TORAX", 30, 25, hoy, "Indicación"));
        assertThrows(IllegalArgumentException.class, () -> servicio.prescribirSeguimiento("PAC-1", "MED-1", "RX-TORAX", 30, 2, null, "Indicación"));
        assertThrows(IllegalArgumentException.class, () -> servicio.prescribirSeguimiento("PAC-1", "MED-1", "RX-TORAX", 30, 2, hoy, " "));
        assertThrows(IllegalStateException.class, () -> servicio.prescribirSeguimiento("PAC-1", "MED-1", "NO-EXISTE", 30, 2, hoy, "Indicación"));
    }

    @Test
    @DisplayName("Consultar y listar seguimientos por paciente funcionan y validan entradas")
    void consultarYListarSeguimientos() {
        LocalDate inicio = LocalDate.of(2026, 12, 5);
        PrescripcionSeguimientoDTO seg = servicio.prescribirSeguimiento(
                "PAC-010", "MED-020", "ECO-ABDO", 60, 2, inicio, "Seguimiento hepático"
        );

        assertEquals(seg.getSeguimientoId(), servicio.consultarSeguimiento(seg.getSeguimientoId()).getSeguimientoId());
        assertEquals(1, servicio.listarSeguimientosPaciente("PAC-010").size());
        assertTrue(servicio.listarSeguimientosPaciente("PAC-SIN-DATOS").isEmpty());

        assertThrows(IllegalArgumentException.class, () -> servicio.consultarSeguimiento(" "));
        assertThrows(IllegalStateException.class, () -> servicio.consultarSeguimiento("SEG-INEXISTENTE"));
        assertThrows(IllegalArgumentException.class, () -> servicio.listarSeguimientosPaciente(null));
    }

    @Test
    @DisplayName("Cancelar seguimiento anula la prescripción y cancela en cascada sus citas generadas")
    void cancelarSeguimientoCancelaCitasAsociadas() {
        LocalDate inicio = LocalDate.of(2026, 12, 15);
        PrescripcionSeguimientoDTO seg = servicio.prescribirSeguimiento(
                "PAC-020", "MED-030", "RM-CRANEO", 180, 2, inicio, "Control post-quirúrgico"
        );

        PrescripcionSeguimientoDTO cancelado = servicio.cancelarSeguimiento(seg.getSeguimientoId(), "Alta médica del paciente");

        assertEquals("CANCELADO", cancelado.getEstado());
        assertEquals("Alta médica del paciente", cancelado.getMotivoCancelacion());
        for (String citaId : cancelado.getCitasGeneradasIds()) {
            assertEquals("CANCELADA", gestionCitasService.consultarCita(citaId).getEstado());
        }

        assertThrows(IllegalStateException.class, () -> servicio.cancelarSeguimiento(seg.getSeguimientoId(), "Duplicado"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelarSeguimiento(null, "Motivo"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cancelarSeguimiento(seg.getSeguimientoId(), " "));
    }
}