package es.uclm.sescam.contratos.citacion;

import java.util.List;

public interface INotificacionesService {
    NotificacionDTO enviarNotificacion(String pacienteId, String tipoAviso, String titulo, String mensaje, String referenciaId);
    List<NotificacionDTO> listarNotificacionesPaciente(String pacienteId);
    NotificacionDTO marcarComoLeida(String notificacionId, String pacienteId);
}