package es.uclm.esi.iso2.servidor;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.IGestorAgendasService;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.prescripciones.ISolicitudPruebaService;
import es.uclm.sescam.contratos.prescripciones.SolicitudPruebaDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.solicitudes.IGestorSolicitudesMedicoService;

public class GestorSolicitudesMedicoServiceImpl implements IGestorSolicitudesMedicoService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PENDIENTE", "CITADA", "EN_REALIZACION", "REALIZADA", "CANCELADA"
    );

    private final IGestorRolesService gestorRoles;
    private final ISolicitudPruebaService solicitudService;
    private final IGestorAgendasService gestorAgendas;
    private final Map<String, SolicitudPruebaDTO> solicitudesMedico = new LinkedHashMap<>();

    public GestorSolicitudesMedicoServiceImpl() {
        this(new GestorRolesServiceImpl(),
                new SolicitudPruebaServiceImpl(),
                new GestorAgendasServiceImpl(new GestorRolesServiceImpl()));
    }

    public GestorSolicitudesMedicoServiceImpl(IGestorRolesService gestorRoles,
                                              ISolicitudPruebaService solicitudService,
                                              IGestorAgendasService gestorAgendas) {
        if (gestorRoles == null || solicitudService == null || gestorAgendas == null) {
            throw new IllegalArgumentException("Los servicios de roles, prescripciones y agendas son obligatorios");
        }
        this.gestorRoles = gestorRoles;
        this.solicitudService = solicitudService;
        this.gestorAgendas = gestorAgendas;
    }

    private void verificarPermisoMedico(String tokenSSO) {
        if (tokenSSO == null || tokenSSO.isBlank()) {
            throw new SecurityException("Token SSO obligatorio");
        }
        if (!gestorRoles.tieneAcceso(tokenSSO, Rol.MEDICO)) {
            throw new SecurityException("Acceso denegado: se requiere rol MEDICO o ADMINISTRADOR");
        }
    }

    @Override
    public SolicitudPruebaDTO registrarSolicitudMedico(String pacienteId, String medicoId, String codigoPrueba,
                                                       int prioridad, String justificacionClinica, String tokenSSO) {
        verificarPermisoMedico(tokenSSO);
        SolicitudPruebaDTO creada = solicitudService.crearSolicitudPrueba(
                pacienteId, medicoId, codigoPrueba, prioridad, justificacionClinica
        );
        solicitudesMedico.put(creada.getSolicitudId(), creada);
        return creada;
    }

    @Override
    public List<SolicitudPruebaDTO> consultarSolicitudesMedico(String medicoId, String filtroEstado, String tokenSSO) {
        verificarPermisoMedico(tokenSSO);
        if (medicoId == null || medicoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del médico es obligatorio");
        }

        String medicoBuscado = medicoId.trim();
        String estadoBuscado = (filtroEstado != null && !filtroEstado.isBlank())
                ? filtroEstado.trim().toUpperCase()
                : null;

        if (estadoBuscado != null && !ESTADOS_VALIDOS.contains(estadoBuscado)) {
            throw new IllegalArgumentException("Estado de solicitud no válido: " + filtroEstado);
        }

        return solicitudesMedico.values().stream()
                .filter(s -> s.getMedicoId().equalsIgnoreCase(medicoBuscado))
                .filter(s -> estadoBuscado == null || estadoBuscado.equalsIgnoreCase(s.getEstado()))
                .collect(Collectors.toList());
    }

    @Override
    public SolicitudPruebaDTO actualizarEstadoSolicitud(String solicitudId, String nuevoEstado,
                                                        String motivo, String tokenSSO) {
        verificarPermisoMedico(tokenSSO);
        if (solicitudId == null || solicitudId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la solicitud es obligatorio");
        }
        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new IllegalArgumentException("El nuevo estado es obligatorio");
        }

        String estadoNormalizado = nuevoEstado.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(estadoNormalizado)) {
            throw new IllegalArgumentException("Estado de solicitud no válido: " + nuevoEstado);
        }

        SolicitudPruebaDTO solicitud = solicitudesMedico.get(solicitudId.trim());
        if (solicitud == null) {
            throw new IllegalStateException("No existe ninguna solicitud con ID: " + solicitudId);
        }
        if ("REALIZADA".equalsIgnoreCase(solicitud.getEstado()) || "CANCELADA".equalsIgnoreCase(solicitud.getEstado())) {
            throw new IllegalStateException("No se puede modificar una solicitud ya finalizada o cancelada");
        }

        if ("CANCELADA".equals(estadoNormalizado)) {
            SolicitudPruebaDTO cancelada = solicitudService.cancelarSolicitud(solicitud.getSolicitudId(), motivo);
            solicitudesMedico.put(cancelada.getSolicitudId(), cancelada);
            return cancelada;
        }

        solicitud.setEstado(estadoNormalizado);
        return solicitud;
    }

    @Override
    public AgendaDiariaDTO consultarAgendaRealizacion(String medicoRealizadorId, LocalDate fecha, String tokenSSO) {
        verificarPermisoMedico(tokenSSO);
        if (medicoRealizadorId == null || medicoRealizadorId.isBlank() || fecha == null) {
            throw new IllegalArgumentException("El médico realizador y la fecha son obligatorios");
        }
        return gestorAgendas.consultarAgendaMedico(medicoRealizadorId.trim(), fecha, tokenSSO);
    }

    @Override
    public List<TramoHorarioDTO> consultarHuecosDisponiblesRealizacion(String medicoRealizadorId, LocalDate fecha, String tokenSSO) {
        AgendaDiariaDTO agenda = consultarAgendaRealizacion(medicoRealizadorId, fecha, tokenSSO);
        if (agenda == null || !agenda.isAbierta() || agenda.getTramos() == null) {
            return List.of();
        }
        return agenda.getTramos().stream()
                .filter(TramoHorarioDTO::isDisponible)
                .collect(Collectors.toList());
    }
}