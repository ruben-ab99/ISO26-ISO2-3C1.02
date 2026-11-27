package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestionCitasService;
import es.uclm.sescam.contratos.roles.IGestorRolesService;
import es.uclm.sescam.contratos.roles.Rol;
import es.uclm.sescam.contratos.security.IAuditService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GestionCitasServiceImpl implements IGestionCitasService {

    private final Map<String, CitaDTO> repositorioCitas = new LinkedHashMap<>();
    private final INotificadorCitas notificador;
    private final IGestorRolesService gestorRoles;
    private final String tokenSSO;
    private final IAuditService auditService;

    public GestionCitasServiceImpl() {
        this((pacienteId, citaId, tipoEvento, detalle) -> {},
                new GestorRolesServiceImpl(),
                "token-sso-admision-ADMISION-default",
                new AuditServiceImpl());
    }

    public GestionCitasServiceImpl(INotificadorCitas notificador) {
        this(notificador,
                new GestorRolesServiceImpl(),
                "token-sso-admision-ADMISION-default",
                new AuditServiceImpl());
    }

    public GestionCitasServiceImpl(INotificadorCitas notificador, IGestorRolesService gestorRoles, String tokenSSO) {
        this(notificador, gestorRoles, tokenSSO, new AuditServiceImpl());
    }

    public GestionCitasServiceImpl(INotificadorCitas notificador, IGestorRolesService gestorRoles,
                                   String tokenSSO, IAuditService auditService) {
        if (notificador == null) {
            throw new IllegalArgumentException("El notificador de citas no puede ser nulo");
        }
        if (gestorRoles == null) {
            throw new IllegalArgumentException("El servicio de gestión de roles no puede ser nulo");
        }
        this.notificador = notificador;
        this.gestorRoles = gestorRoles;
        this.tokenSSO = tokenSSO;
        this.auditService = auditService;
    }

    private void verificarPermisoGestionCitas() {
        boolean esAdmision = gestorRoles.tieneAcceso(tokenSSO, Rol.ADMISION);
        boolean esMedico = gestorRoles.tieneAcceso(tokenSSO, Rol.MEDICO);
        if (!esAdmision && !esMedico) {
            registrarAuditoria("SISTEMA", "ACCESO_DENEGADO_GESTION_CITAS", false);
            throw new SecurityException("Acceso denegado: Se requiere rol ADMISION o MEDICO.");
        }
    }

    @Override
    public CitaDTO programarCita(String pacienteId, String medicoId, String codigoPrueba,
                                 LocalDate fecha, String franjaHoraria) {
        verificarPermisoGestionCitas();

        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
        if (medicoId == null || medicoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del médico es obligatorio");
        }
        if (codigoPrueba == null || codigoPrueba.isBlank()) {
            throw new IllegalArgumentException("El código de la prueba diagnóstica es obligatorio");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de la cita es obligatoria");
        }
        if (franjaHoraria == null || franjaHoraria.isBlank()) {
            throw new IllegalArgumentException("La franja horaria es obligatoria");
        }

        String medicoLimpio = medicoId.trim();
        String franjaLimpia = franjaHoraria.trim();

        if (existeConflictoHorario(medicoLimpio, fecha, franjaLimpia, null)) {
            throw new IllegalStateException("La franja horaria ya está ocupada para ese médico y fecha");
        }

        String nuevaCitaId = "CIT-" + UUID.randomUUID().toString().substring(0, 8);
        CitaDTO cita = new CitaDTO(
                nuevaCitaId,
                pacienteId.trim(),
                medicoLimpio,
                codigoPrueba.trim().toUpperCase(),
                fecha,
                franjaLimpia,
                "PROGRAMADA",
                "",
                LocalDateTime.now()
        );

        repositorioCitas.put(nuevaCitaId, cita);
        notificador.notificarCambioCita(
                cita.getPacienteId(),
                nuevaCitaId,
                "PROGRAMADA",
                "Cita programada para el " + fecha + " en franja " + franjaLimpia
        );
        registrarAuditoria(medicoLimpio, "PROGRAMAR_CITA:" + nuevaCitaId, true);
        return cita;
    }

    @Override
    public CitaDTO consultarCita(String citaId) {
        if (tokenSSO == null || tokenSSO.isBlank()) {
            throw new SecurityException("Token de sesión obligatorio para consultar citas");
        }
        if (citaId == null || citaId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la cita es obligatorio");
        }
        CitaDTO cita = repositorioCitas.get(citaId.trim());
        if (cita == null) {
            throw new IllegalStateException("No existe ninguna cita con ID: " + citaId);
        }
        return cita;
    }

    @Override
    public List<CitaDTO> listarCitasPaciente(String pacienteId) {
        if (tokenSSO == null || tokenSSO.isBlank()) {
            throw new SecurityException("Token de sesión obligatorio para listar citas");
        }
        if (pacienteId == null || pacienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente es obligatorio");
        }
        String pacienteBuscado = pacienteId.trim();
        List<CitaDTO> resultado = new ArrayList<>();
        for (CitaDTO cita : repositorioCitas.values()) {
            if (cita.getPacienteId().equals(pacienteBuscado)) {
                resultado.add(cita);
            }
        }
        return resultado;
    }

    @Override
    public List<CitaDTO> listarCitasEntreFechas(LocalDate desde, LocalDate hasta) {
        if (tokenSSO == null || tokenSSO.isBlank()) {
            throw new SecurityException("Token de sesión obligatorio para listar citas");
        }
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin");
        }
        List<CitaDTO> resultado = new ArrayList<>();
        for (CitaDTO cita : repositorioCitas.values()) {
            if (cita.getFecha() != null && !cita.getFecha().isBefore(desde) && !cita.getFecha().isAfter(hasta)) {
                resultado.add(cita);
            }
        }
        return resultado;
    }

    @Override
    public CitaDTO reprogramarCita(String citaId, LocalDate nuevaFecha, String nuevaFranja) {
        verificarPermisoGestionCitas();

        if (citaId == null || citaId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la cita es obligatorio");
        }
        if (nuevaFecha == null) {
            throw new IllegalArgumentException("La nueva fecha es obligatoria");
        }
        if (nuevaFranja == null || nuevaFranja.isBlank()) {
            throw new IllegalArgumentException("La nueva franja horaria es obligatoria");
        }

        CitaDTO cita = consultarCita(citaId);
        if ("CANCELADA".equals(cita.getEstado())) {
            throw new IllegalStateException("No se puede reprogramar una cita cancelada");
        }

        String franjaLimpia = nuevaFranja.trim();
        if (existeConflictoHorario(cita.getMedicoId(), nuevaFecha, franjaLimpia, cita.getCitaId())) {
            throw new IllegalStateException("La nueva franja horaria no está disponible");
        }

        cita.setFecha(nuevaFecha);
        cita.setFranjaHoraria(franjaLimpia);
        cita.setEstado("REPROGRAMADA");
        cita.setFechaUltimaActualizacion(LocalDateTime.now());

        notificador.notificarCambioCita(
                cita.getPacienteId(),
                cita.getCitaId(),
                "REPROGRAMADA",
                "Cita reprogramada al " + nuevaFecha + " en franja " + franjaLimpia
        );
        registrarAuditoria(cita.getMedicoId(), "REPROGRAMAR_CITA:" + cita.getCitaId(), true);
        return cita;
    }

    @Override
    public CitaDTO cancelarCita(String citaId, String motivo) {
        verificarPermisoGestionCitas();

        if (citaId == null || citaId.isBlank()) {
            throw new IllegalArgumentException("El identificador de la cita es obligatorio");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo de cancelación es obligatorio");
        }

        CitaDTO cita = consultarCita(citaId);
        if ("CANCELADA".equals(cita.getEstado())) {
            throw new IllegalStateException("La cita ya se encuentra cancelada");
        }

        cita.setEstado("CANCELADA");
        cita.setMotivoCancelacion(motivo.trim());
        cita.setFechaUltimaActualizacion(LocalDateTime.now());

        notificador.notificarCambioCita(
                cita.getPacienteId(),
                cita.getCitaId(),
                "CANCELADA",
                "Cita cancelada. Motivo: " + motivo.trim()
        );
        registrarAuditoria(cita.getMedicoId(), "CANCELAR_CITA:" + cita.getCitaId(), true);
        return cita;
    }

    private boolean existeConflictoHorario(String medicoId, LocalDate fecha, String franjaHoraria, String citaIdIgnorar) {
        for (CitaDTO existente : repositorioCitas.values()) {
            if ("CANCELADA".equals(existente.getEstado())) {
                continue;
            }
            if (citaIdIgnorar != null && existente.getCitaId().equals(citaIdIgnorar)) {
                continue;
            }
            if (existente.getMedicoId().equalsIgnoreCase(medicoId)
                    && existente.getFecha().equals(fecha)
                    && existente.getFranjaHoraria().equalsIgnoreCase(franjaHoraria)) {
                return true;
            }
        }
        return false;
    }

    private void registrarAuditoria(String usuario, String accion, boolean exito) {
        if (auditService != null) {
            String actor = (usuario != null && !usuario.isBlank()) ? usuario.trim() : "SISTEMA";
            auditService.registrarEvento(actor, accion, exito, "127.0.0.1");
        }
    }
}