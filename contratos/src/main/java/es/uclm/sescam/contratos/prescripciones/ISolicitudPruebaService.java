package es.uclm.sescam.contratos.prescripciones;

import java.util.List;

public interface ISolicitudPruebaService {
    SolicitudPruebaDTO crearSolicitudPrueba(String pacienteId, String medicoId, String codigoPrueba, int prioridad, String justificacionClinica);
    SolicitudPruebaDTO consultarSolicitud(String solicitudId);
    List<SolicitudPruebaDTO> listarSolicitudesPaciente(String pacienteId);
    SolicitudPruebaDTO cancelarSolicitud(String solicitudId, String motivo);

    default List<SolicitudPruebaDTO> listarSolicitudesMedico(String medicoId) {
        return List.of();
    }
}