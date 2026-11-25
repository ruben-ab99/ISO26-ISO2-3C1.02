package es.uclm.sescam.contratos.prescripciones;

import java.time.LocalDate;
import java.util.List;

public interface IPrescripcionSeguimientoService {
    PrescripcionSeguimientoDTO prescribirSeguimiento(String pacienteId, String medicoId, String codigoPrueba,
                                                     int periodicidadDias, int numeroRevisiones,
                                                     LocalDate fechaInicio, String indicacionesClinicas);
    PrescripcionSeguimientoDTO consultarSeguimiento(String seguimientoId);
    List<PrescripcionSeguimientoDTO> listarSeguimientosPaciente(String pacienteId);
    PrescripcionSeguimientoDTO cancelarSeguimiento(String seguimientoId, String motivo);

    // Consulta de solo lectura para la adherencia al seguimiento de CU-11 (ADR-017).
    default List<PrescripcionSeguimientoDTO> listarSeguimientosMedico(String medicoId) {
        return List.of();
    }
}