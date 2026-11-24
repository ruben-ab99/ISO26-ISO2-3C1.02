package es.uclm.sescam.contratos.portal;

import java.util.List;

public interface IComunicacionPacienteService {
    IncidenciaPacienteDTO registrarIncidencia(String pacienteId, String referenciaCitaOPruebaId, String categoria, String descripcion);
    List<IncidenciaPacienteDTO> listarMisIncidencias(String pacienteId);
    EncuestaSatisfaccionDTO responderEncuesta(String pacienteId, String citaId, int puntuacionGlobal, int puntuacionAtencion, String comentarios);
    List<EncuestaSatisfaccionDTO> listarMisEncuestas(String pacienteId);
}