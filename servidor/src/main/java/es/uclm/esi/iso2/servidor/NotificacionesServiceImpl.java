package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.citacion.INotificacionesService;
import es.uclm.sescam.contratos.citacion.NotificacionDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.IAuditService;

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
    private final IGestorRolesService gestorRoles;
    private final String tokenSSO;
    private final IAuditService auditService;

    public NotificacionesServiceImpl() {
        this(new GestorRolesServiceImpl(), "token-sso-admision-ADMISION-default", new AuditServiceImpl());
    }

    public NotificacionesServiceImpl(IGestorRolesService gestorRoles, String tokenSSO) {
        this(gestorRoles, tokenSSO, new AuditServiceImpl());
    }

    public NotificacionesServiceImpl(IGestorRolesService gestorRoles, String tokenSSO, IAuditService auditService) {
        if (gestorRoles == null) {
            throw new IllegalArgumentException("El servicio de gestión de roles no puede ser nulo");
        }
        this.gestorRoles = gestorRoles;
        this.tokenSSO = tokenSSO;
        this.auditService = auditService;
    }

    private void verificarPermisoEnvioNotificacion() {
        boolean esAdmision = gestorRoles.tieneAcceso(tokenSSO, Rol.ADMISION);
        boolean esMedico = gestorRoles.tieneAcceso(tokenSSO, Rol.MEDICO);
        if (!esAdmision && !esMedico) {
            registrarAuditoria("SISTEMA", "ACCESO_DENEGADO_NOTIFICACIONES", false);
            throw new SecurityException("Acceso denegado: Se requiere rol ADMISION o MEDICO para enviar notificaciones.");
        }
    }

    @Override
    public NotificacionDTO enviarNotificacion(String pacienteId, String tipoAviso, String titulo,
                                              String mensaje, String referenciaId) {
        verificarPermisoEnvioNotificacion();

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
        registrarAuditoria(pacienteId, "ENVIAR_NOTIFICACION:" + nuevoId, true);
        return notificacion;
    }

    @Override
    public List<NotificacionDTO> listarNotificacionesPaciente(String pacienteId) {
        if (tokenSSO == null || tokenSSO.isBlank()) {
            throw new SecurityException("Token de sesión obligatorio para consultar notificaciones");
        }
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
        if (tokenSSO == null || tokenSSO.isBlank()) {
            throw new SecurityException("Token de sesión obligatorio para gestionar notificaciones");
        }
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
        registrarAuditoria(pacienteId, "LEER_NOTIFICACION:" + notificacion.getId(), true);
        return notificacion;
    }

    private void registrarAuditoria(String usuario, String accion, boolean exito) {
        if (auditService != null) {
            String actor = (usuario != null && !usuario.isBlank()) ? usuario.trim() : "SISTEMA";
            auditService.registrarEvento(actor, accion, exito, "127.0.0.1");
        }
    }
}