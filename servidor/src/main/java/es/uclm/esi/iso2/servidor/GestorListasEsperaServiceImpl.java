package es.uclm.esi.iso2.servidor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.IGestorAgendasService;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.listasespera.IGestorListasEsperaService;
import es.uclm.sescam.contratos.listasespera.PrioridadEspera;
import es.uclm.sescam.contratos.listasespera.SolicitudEsperaDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;

public class GestorListasEsperaServiceImpl implements IGestorListasEsperaService {

    private final IGestorRolesService gestorRoles;
    private final IGestorAgendasService gestorAgendas;
    private final Map<String, SolicitudEsperaDTO> solicitudes = new LinkedHashMap<>();

    public GestorListasEsperaServiceImpl(IGestorRolesService gestorRoles, IGestorAgendasService gestorAgendas) {
        if (gestorRoles == null || gestorAgendas == null) {
            throw new IllegalArgumentException("Los servicios de roles y agendas son obligatorios");
        }
        this.gestorRoles = gestorRoles;
        this.gestorAgendas = gestorAgendas;
    }

    private void verificarPermisoAdmision(String tokenSSO) {
        if (tokenSSO == null || tokenSSO.isBlank()) {
            throw new SecurityException("Token SSO obligatorio");
        }
        Rol rol = gestorRoles.extraerRolDeToken(tokenSSO);
        if (rol != Rol.ADMISION && rol != Rol.ADMINISTRADOR) {
            throw new SecurityException("Acceso denegado: se requiere rol ADMISION o ADMINISTRADOR");
        }
    }

    @Override
    public SolicitudEsperaDTO registrarEnListaEspera(String pacienteId, String tipoPrueba,
                                                     PrioridadEspera prioridad, String tokenSSO) {
        verificarPermisoAdmision(tokenSSO);

        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
        if (tipoPrueba == null || tipoPrueba.isBlank()) {
            throw new IllegalArgumentException("El tipo de prueba es obligatorio");
        }
        if (prioridad == null) {
            throw new IllegalArgumentException("La prioridad es obligatoria");
        }

        boolean yaEnEspera = solicitudes.values().stream()
                .anyMatch(s -> !s.isAtendida()
                        && s.getPacienteId().equalsIgnoreCase(pacienteId.trim())
                        && s.getTipoPrueba().equalsIgnoreCase(tipoPrueba.trim()));
        if (yaEnEspera) {
            throw new IllegalStateException("El paciente ya tiene una solicitud pendiente para esa prueba");
        }

        String id = "ESP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        SolicitudEsperaDTO nueva = new SolicitudEsperaDTO(
                id,
                pacienteId.trim(),
                tipoPrueba.trim(),
                prioridad,
                LocalDateTime.now(),
                false
        );
        solicitudes.put(id, nueva);
        return nueva;
    }

    @Override
    public List<SolicitudEsperaDTO> consultarListaEspera(String tipoPrueba, String tokenSSO) {
        verificarPermisoAdmision(tokenSSO);

        return solicitudes.values().stream()
                .filter(s -> !s.isAtendida())
                .filter(s -> tipoPrueba == null || tipoPrueba.isBlank()
                        || s.getTipoPrueba().equalsIgnoreCase(tipoPrueba.trim()))
                .sorted(Comparator
                        .comparingInt((SolicitudEsperaDTO s) -> s.getPrioridad().getNivel())
                        .thenComparing(SolicitudEsperaDTO::getFechaRegistro))
                .collect(Collectors.toList());
    }

    @Override
    public boolean actualizarPrioridad(String solicitudId, PrioridadEspera nuevaPrioridad, String tokenSSO) {
        verificarPermisoAdmision(tokenSSO);

        if (solicitudId == null || solicitudId.isBlank() || nuevaPrioridad == null) {
            throw new IllegalArgumentException("Identificador de solicitud y nueva prioridad obligatorios");
        }

        SolicitudEsperaDTO solicitud = solicitudes.get(solicitudId);
        if (solicitud == null) {
            throw new IllegalStateException("No existe la solicitud indicada");
        }
        if (solicitud.isAtendida() || solicitud.getPrioridad() == nuevaPrioridad) {
            return false;
        }

        solicitud.setPrioridad(nuevaPrioridad);
        return true;
    }

    @Override
    public int contarHuecosLibres(String medicoId, LocalDate fecha, String tokenSSO) {
        verificarPermisoAdmision(tokenSSO);

        AgendaDiariaDTO agenda = gestorAgendas.consultarAgendaMedico(medicoId, fecha, tokenSSO);
        if (!agenda.isAbierta() || agenda.getTramos() == null) {
            return 0;
        }
        return (int) agenda.getTramos().stream()
                .filter(TramoHorarioDTO::isDisponible)
                .count();
    }

    @Override
    public SolicitudEsperaDTO asignarHuecoLibre(String solicitudId, String medicoId, LocalDate fecha,
                                                LocalTime horaInicio, LocalTime horaFin, String tokenSSO) {
        verificarPermisoAdmision(tokenSSO);

        if (solicitudId == null || solicitudId.isBlank() || medicoId == null || medicoId.isBlank()
                || fecha == null || horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException("Datos de solicitud, médico, fecha y franja horaria obligatorios");
        }

        SolicitudEsperaDTO solicitud = solicitudes.get(solicitudId);
        if (solicitud == null) {
            throw new IllegalStateException("No existe la solicitud en lista de espera");
        }
        if (solicitud.isAtendida()) {
            throw new IllegalStateException("La solicitud ya ha sido atendida");
        }

        AgendaDiariaDTO agenda = gestorAgendas.consultarAgendaMedico(medicoId, fecha, tokenSSO);
        if (!agenda.isAbierta()) {
            throw new IllegalStateException("La agenda del médico está cerrada para esa fecha");
        }

        boolean huecoReservado = gestorAgendas.bloquearTramoAgenda(
                agenda.getAgendaId(),
                horaInicio,
                horaFin,
                "Asignado lista espera: " + solicitud.getSolicitudId(),
                tokenSSO
        );
        if (!huecoReservado) {
            throw new IllegalStateException("El hueco solicitado no está libre en la agenda");
        }

        solicitud.setAtendida(true);
        solicitud.setAgendaAsignadaId(agenda.getAgendaId());
        solicitud.setFechaAsignada(fecha);
        solicitud.setHoraAsignada(horaInicio);
        return solicitud;
    }
}