package es.uclm.esi.iso2.servidor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.IGestorAgendasService;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestionCitasService;
import es.uclm.sescam.contratos.citacion.IGestorCitasPacienteService;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;

public class GestorCitasPacienteServiceImpl implements IGestorCitasPacienteService {

    private static final Set<String> ESTADOS_CITA_VALIDOS = Set.of(
            "PROGRAMADA", "REPROGRAMADA", "CANCELADA"
    );

    private final IGestorRolesService gestorRoles;
    private final IGestionCitasService gestionCitas;
    private final IGestorAgendasService gestorAgendas;

    public GestorCitasPacienteServiceImpl() {
        this(
                new GestorRolesServiceImpl(),
                new GestionCitasServiceImpl(),
                new GestorAgendasServiceImpl(new GestorRolesServiceImpl())
        );
    }

    public GestorCitasPacienteServiceImpl(IGestorRolesService gestorRoles,
                                          IGestionCitasService gestionCitas,
                                          IGestorAgendasService gestorAgendas) {
        if (gestorRoles == null || gestionCitas == null || gestorAgendas == null) {
            throw new IllegalArgumentException("Los servicios de roles, citación y agendas son obligatorios");
        }
        this.gestorRoles = gestorRoles;
        this.gestionCitas = gestionCitas;
        this.gestorAgendas = gestorAgendas;
    }

    private void verificarPermisoPaciente(String tokenSSO) {
        if (tokenSSO == null || tokenSSO.isBlank()) {
            throw new SecurityException("Token SSO obligatorio");
        }
        if (!gestorRoles.tieneAcceso(tokenSSO, Rol.PACIENTE)) {
            throw new SecurityException("Acceso denegado: se requiere rol PACIENTE");
        }
    }

    private void validarPacienteId(String pacienteId) {
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
    }

    private CitaDTO obtenerYValidarPropiedadCita(String pacienteId, String citaId) {
        validarPacienteId(pacienteId);
        if (citaId == null || citaId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la cita es obligatorio");
        }
        CitaDTO cita = gestionCitas.consultarCita(citaId.trim());
        if (cita.getPacienteId() == null || !cita.getPacienteId().trim().equalsIgnoreCase(pacienteId.trim())) {
            throw new SecurityException("Acceso denegado: la cita no pertenece al paciente autenticado");
        }
        return cita;
    }

    @Override
    public List<CitaDTO> consultarMisCitas(String pacienteId, String filtroEstado, String tokenSSO) {
        verificarPermisoPaciente(tokenSSO);
        validarPacienteId(pacienteId);

        String estadoNormalizado = null;
        if (filtroEstado != null && !filtroEstado.isBlank()) {
            estadoNormalizado = filtroEstado.trim().toUpperCase();
            if (!ESTADOS_CITA_VALIDOS.contains(estadoNormalizado)) {
                throw new IllegalArgumentException("Estado de cita no válido: " + filtroEstado);
            }
        }

        final String filtroFinal = estadoNormalizado;
        return gestionCitas.listarCitasPaciente(pacienteId.trim()).stream()
                .filter(c -> filtroFinal == null || filtroFinal.equalsIgnoreCase(c.getEstado()))
                .collect(Collectors.toList());
    }

    @Override
    public CitaDTO consultarDetalleCitaPropia(String pacienteId, String citaId, String tokenSSO) {
        verificarPermisoPaciente(tokenSSO);
        return obtenerYValidarPropiedadCita(pacienteId, citaId);
    }

    @Override
    public List<TramoHorarioDTO> consultarHuecosDisponiblesCambio(String medicoId, LocalDate fecha, String tokenSSO) {
        verificarPermisoPaciente(tokenSSO);
        if (medicoId == null || medicoId.isBlank() || fecha == null) {
            throw new IllegalArgumentException("El identificador del médico y la fecha son obligatorios");
        }
        AgendaDiariaDTO agenda = gestorAgendas.consultarAgendaMedico(
                medicoId.trim(), fecha, "token-sso-portal-paciente-ADMISION-0000"
        );
        if (agenda == null || !agenda.isAbierta() || agenda.getTramos() == null) {
            return List.of();
        }
        return agenda.getTramos().stream()
                .filter(TramoHorarioDTO::isDisponible)
                .collect(Collectors.toList());
    }

    @Override
    public CitaDTO reprogramarCitaPropia(String pacienteId, String citaId, LocalDate nuevaFecha,
                                         String nuevaFranja, String tokenSSO) {
        verificarPermisoPaciente(tokenSSO);
        CitaDTO cita = obtenerYValidarPropiedadCita(pacienteId, citaId);

        if ("CANCELADA".equalsIgnoreCase(cita.getEstado())) {
            throw new IllegalStateException("No se puede reprogramar una cita ya cancelada");
        }
        if (nuevaFecha == null || nuevaFranja == null || nuevaFranja.isBlank()) {
            throw new IllegalArgumentException("La nueva fecha y la nueva franja horaria son obligatorias");
        }

        return gestionCitas.reprogramarCita(cita.getCitaId(), nuevaFecha, nuevaFranja.trim());
    }

    @Override
    public CitaDTO cancelarCitaPropia(String pacienteId, String citaId, String motivo, String tokenSSO) {
        verificarPermisoPaciente(tokenSSO);
        CitaDTO cita = obtenerYValidarPropiedadCita(pacienteId, citaId);

        if ("CANCELADA".equalsIgnoreCase(cita.getEstado())) {
            throw new IllegalStateException("La cita ya se encuentra cancelada");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de cancelación es obligatorio");
        }

        return gestionCitas.cancelarCita(cita.getCitaId(), motivo.trim());
    }
}