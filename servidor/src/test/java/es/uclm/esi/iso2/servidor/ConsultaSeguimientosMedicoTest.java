package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.prescripciones.IPrescripcionSeguimientoService;
import es.uclm.sescam.contratos.prescripciones.PrescripcionSeguimientoDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de la consulta de solo lectura añadida en la It7 para CU-11 (ADR-017):
 * IPrescripcionSeguimientoService.listarSeguimientosMedico.
 * Técnicas: clases de equivalencia (médico con / sin seguimientos, identificador vacío).
 */
class ConsultaSeguimientosMedicoTest {

    @Test
    @DisplayName("Devuelve solo los seguimientos prescritos por el médico indicado")
    void devuelveLosSeguimientosDelMedico() {
        PrescripcionSeguimientoServiceImpl servicio = new PrescripcionSeguimientoServiceImpl();
        LocalDate inicio = LocalDate.of(2027, 1, 11);
        servicio.prescribirSeguimiento("PAC-1", "MED-1", "RX-TORAX", 30, 2, inicio, "Control de nódulo");
        servicio.prescribirSeguimiento("PAC-2", "MED-1", "TAC-ABDO", 60, 1, inicio, "Revisión oncológica");
        servicio.prescribirSeguimiento("PAC-3", "MED-2", "RX-TORAX", 90, 1, inicio, "Cribado");

        List<PrescripcionSeguimientoDTO> delMedico1 = servicio.listarSeguimientosMedico("MED-1");

        assertEquals(2, delMedico1.size());
        assertTrue(delMedico1.stream().allMatch(s -> "MED-1".equals(s.getMedicoId())));
        assertEquals(2, servicio.listarSeguimientosMedico("  MED-1 ").size());
        assertEquals(1, servicio.listarSeguimientosMedico("MED-2").size());
        assertTrue(servicio.listarSeguimientosMedico("MED-3").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("El identificador del médico es obligatorio")
    void medicoObligatorio(String medicoId) {
        PrescripcionSeguimientoServiceImpl servicio = new PrescripcionSeguimientoServiceImpl();

        assertThrows(IllegalArgumentException.class, () -> servicio.listarSeguimientosMedico(medicoId));
    }

    @Test
    @DisplayName("Una implementación que no sobrescribe la consulta devuelve una lista vacía (método default)")
    void metodoPorDefectoDevuelveListaVacia() {
        IPrescripcionSeguimientoService minima = new IPrescripcionSeguimientoService() {
            @Override
            public PrescripcionSeguimientoDTO prescribirSeguimiento(String pacienteId, String medicoId, String codigoPrueba,
                                                                    int periodicidadDias, int numeroRevisiones,
                                                                    LocalDate fechaInicio, String indicacionesClinicas) {
                return null;
            }

            @Override
            public PrescripcionSeguimientoDTO consultarSeguimiento(String seguimientoId) {
                return null;
            }

            @Override
            public List<PrescripcionSeguimientoDTO> listarSeguimientosPaciente(String pacienteId) {
                return List.of();
            }

            @Override
            public PrescripcionSeguimientoDTO cancelarSeguimiento(String seguimientoId, String motivo) {
                return null;
            }
        };

        assertTrue(minima.listarSeguimientosMedico("MED-1").isEmpty());
    }
}
