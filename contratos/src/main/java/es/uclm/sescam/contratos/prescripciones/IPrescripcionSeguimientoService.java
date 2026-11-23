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
}