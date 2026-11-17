package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.citacion.INotificacionesService;
import es.uclm.sescam.contratos.citacion.NotificacionDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class NotificacionesServiceImpl implements INotificacionesService {

    private static final Set<String> TIPOS_AVISO_VALIDOS = Set.of(
            "CITA", "PREPARACION_PREVIA", "INFORME_DISPONIBLE"
    );

    private final Map<String, NotificacionDTO> repositorioNotificaciones = new LinkedHashMap<>();

    @Override
    public NotificacionDTO enviarNotificacion(String pacienteId, String tipoAviso, String titulo,
                                              String mensaje, String referenciaId) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
        if (tipoAviso == null || tipoAviso.isBlank()) {
            throw new IllegalArgumentException("El tipo de aviso es obligatorio");
        }
        String tipoNormalizado = tipoAviso.trim().toUpperCase();
        if (!TIPOS_AVISO_VALIDOS.contains(tipoNormalizado)) {
            throw new IllegalArgumentException("Tipo de aviso no soportado: " + tipoAviso);
        }
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El título de la notificación es obligatorio");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje de la notificación es obligatorio");
        }

        String nuevoId = "NOT-" + UUID.randomUUID().toString().substring(0, 8);
        String refLimpia = (referenciaId != null) ? referenciaId.trim() : "";

        NotificacionDTO notificacion = new NotificacionDTO(
                nuevoId,
                pacienteId.trim(),
                tipoNormalizado,
                titulo.trim(),
                mensaje.trim(),
                refLimpia,
                false,
                LocalDateTime.now()
        );

        repositorioNotificaciones.put(nuevoId, notificacion);
        return notificacion;
    }

    @Override
    public List<NotificacionDTO> listarNotificacionesPaciente(String pacienteId) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
        String pacienteBuscado = pacienteId.trim();
        List<NotificacionDTO> resultado = new ArrayList<>();
        for (NotificacionDTO notificacion : repositorioNotificaciones.values()) {
            if (notificacion.getPacienteId().equals(pacienteBuscado)) {
                resultado.add(notificacion);
            }
        }
        return resultado;
    }

    @Override
    public NotificacionDTO marcarComoLeida(String notificacionId, String pacienteId) {
        if (notificacionId == null || notificacionId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la notificación es obligatorio");
        }
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }

        NotificacionDTO notificacion = repositorioNotificaciones.get(notificacionId.trim());
        if (notificacion == null) {
            throw new IllegalStateException("No existe ninguna notificación con ID: " + notificacionId);
        }
        if (!notificacion.getPacienteId().equals(pacienteId.trim())) {
            throw new IllegalStateException("La notificación no pertenece al paciente indicado");
        }

        notificacion.setLeida(true);
        return notificacion;
    }
}