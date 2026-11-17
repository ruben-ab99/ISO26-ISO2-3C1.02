package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.IGestorAgendasService;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GestorAgendasServiceImpl implements IGestorAgendasService {

    private final Map<String, AgendaDiariaDTO> agendas = new HashMap<>();
    private final IGestorRolesService gestorRoles;

    public GestorAgendasServiceImpl(IGestorRolesService gestorRoles) {
        if (gestorRoles == null) {
            throw new IllegalArgumentException("El servicio de gestión de roles no puede ser nulo");
        }
        this.gestorRoles = gestorRoles;
    }

    private void verificarPermisoAgenda(String tokenSSO) {
        boolean esAdmision = gestorRoles.tieneAcceso(tokenSSO, Rol.ADMISION);
        boolean esMedico = gestorRoles.tieneAcceso(tokenSSO, Rol.MEDICO);
        if (!esAdmision && !esMedico) {
            throw new SecurityException("Acceso denegado: Se requiere rol ADMISION, MEDICO o ADMINISTRADOR.");
        }
    }

    @Override
    public AgendaDiariaDTO configurarAgendaDiaria(String medicoId, LocalDate fecha, LocalTime horaInicio,
                                                  LocalTime horaFin, int duracionHuecoMinutos, String tokenSSO) {
        verificarPermisoAgenda(tokenSSO);

        if (medicoId == null || medicoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del médico es obligatorio");
        }
        if (fecha == null || horaInicio == null || horaFin == null) {
            throw new IllegalArgumentException("La fecha y las horas de inicio y fin son obligatorias");
        }
        if (!horaInicio.isBefore(horaFin)) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la hora de fin");
        }
        if (duracionHuecoMinutos < 5) {
            throw new IllegalArgumentException("La duración mínima de cada tramo debe ser de 5 minutos");
        }

        boolean yaExiste = agendas.values().stream()
                .anyMatch(a -> a.isAbierta() && a.getMedicoId().equalsIgnoreCase(medicoId.trim()) && a.getFecha().equals(fecha));
        if (yaExiste) {
            throw new IllegalStateException("El médico " + medicoId + " ya tiene una agenda abierta para la fecha " + fecha);
        }

        List<TramoHorarioDTO> tramosGenerados = new ArrayList<>();
        LocalTime cursor = horaInicio;
        while (!cursor.plusMinutes(duracionHuecoMinutos).isAfter(horaFin)) {
            LocalTime finTramo = cursor.plusMinutes(duracionHuecoMinutos);
            tramosGenerados.add(new TramoHorarioDTO(cursor, finTramo, true, null));
            cursor = finTramo;
        }

        if (tramosGenerados.isEmpty()) {
            throw new IllegalArgumentException("El rango horario es insuficiente para generar al menos un hueco de " + duracionHuecoMinutos + " minutos");
        }

        String agendaId = "AGD-" + UUID.randomUUID().toString().substring(0, 8);
        AgendaDiariaDTO nuevaAgenda = new AgendaDiariaDTO(
                agendaId,
                medicoId.trim(),
                fecha,
                horaInicio,
                horaFin,
                duracionHuecoMinutos,
                true,
                tramosGenerados
        );

        agendas.put(agendaId, nuevaAgenda);
        return nuevaAgenda;
    }

    @Override
    public AgendaDiariaDTO consultarAgendaMedico(String medicoId, LocalDate fecha, String tokenSSO) {
        if (tokenSSO == null || tokenSSO.isBlank()) {
            throw new SecurityException("Token de sesión obligatorio para consultar agendas");
        }
        if (medicoId == null || medicoId.isBlank() || fecha == null) {
            throw new IllegalArgumentException("El médico y la fecha son obligatorios");
        }

        return agendas.values().stream()
                .filter(a -> a.getMedicoId().equalsIgnoreCase(medicoId.trim()) && a.getFecha().equals(fecha))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe agenda configurada para el médico " + medicoId + " en fecha " + fecha));
    }

    @Override
    public boolean bloquearTramoAgenda(String agendaId, LocalTime horaInicioTramo, LocalTime horaFinTramo,
                                       String motivoBloqueo, String tokenSSO) {
        verificarPermisoAgenda(tokenSSO);

        if (agendaId == null || agendaId.isBlank()) {
            throw new IllegalArgumentException("El ID de la agenda es obligatorio");
        }
        if (horaInicioTramo == null || horaFinTramo == null || !horaInicioTramo.isBefore(horaFinTramo)) {
            throw new IllegalArgumentException("El rango horario a bloquear no es válido");
        }
        if (motivoBloqueo == null || motivoBloqueo.isBlank()) {
            throw new IllegalArgumentException("Debe indicarse un motivo para bloquear el tramo");
        }

        AgendaDiariaDTO agenda = agendas.get(agendaId);
        if (agenda == null) {
            throw new IllegalStateException("No existe ninguna agenda con ID: " + agendaId);
        }
        if (!agenda.isAbierta()) {
            throw new IllegalStateException("No se pueden bloquear tramos en una agenda cerrada");
        }

        boolean algunBloqueado = false;
        for (TramoHorarioDTO tramo : agenda.getTramos()) {
            // Comprueba si el tramo solapa con el intervalo de bloqueo
            boolean solapa = tramo.getHoraInicio().isBefore(horaFinTramo) && tramo.getHoraFin().isAfter(horaInicioTramo);
            if (solapa && tramo.isDisponible()) {
                tramo.setDisponible(false);
                tramo.setMotivoBloqueo(motivoBloqueo.trim());
                algunBloqueado = true;
            }
        }
        return algunBloqueado;
    }

    @Override
    public boolean cerrarAgendaDiaria(String agendaId, String tokenSSO) {
        verificarPermisoAgenda(tokenSSO);

        if (agendaId == null || agendaId.isBlank()) {
            throw new IllegalArgumentException("El ID de la agenda es obligatorio");
        }

        AgendaDiariaDTO agenda = agendas.get(agendaId);
        if (agenda == null) {
            throw new IllegalStateException("No existe ninguna agenda con ID: " + agendaId);
        }
        if (!agenda.isAbierta()) {
            return false;
        }

        agenda.setAbierta(false);
        return true;
    }
}