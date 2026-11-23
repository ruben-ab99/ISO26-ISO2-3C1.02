package es.uclm.sescam.contratos.prescripciones;

import java.util.List;

public interface IConsultaSeguimientoPacienteService {
    List<PrescripcionSeguimientoDTO> listarMisSeguimientos(String pacienteId);
    List<PrescripcionSeguimientoDTO> listarMisSeguimientosActivos(String pacienteId);
    PrescripcionSeguimientoDTO consultarDetalleMiSeguimiento(String seguimientoId, String pacienteId);
}
