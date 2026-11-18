package es.uclm.esi.iso2.servidor;

import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestionCitasService;

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

    public GestionCitasServiceImpl() {
        this.notificador = (pacienteId, citaId, tipoEvento, detalle) -> {};
    }

    public GestionCitasServiceImpl(INotificadorCitas notificador) {
        if (notificador == null) {
            throw new IllegalArgumentException("El notificador de citas no puede ser nulo");
        }
        this.notificador = notificador;
    }

    @Override
    public CitaDTO programarCita(String pacienteId, String medicoId, String codigoPrueba,
                                 LocalDate fecha, String franjaHoraria) {
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
        return cita;
    }

    @Override
    public CitaDTO consultarCita(String citaId) {
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
    public CitaDTO reprogramarCita(String citaId, LocalDate nuevaFecha, String nuevaFranja) {
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
        return cita;
    }

    @Override
    public CitaDTO cancelarCita(String citaId, String motivo) {
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
}