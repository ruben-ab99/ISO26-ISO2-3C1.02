package es.uclm.esi.iso2.cliente.paciente;

import java.time.LocalDate;
import java.util.List;

import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.citacion.CitaDTO;
import es.uclm.sescam.contratos.citacion.IGestorCitasPacienteService;

public class PortalPacienteCitasControlador {

    private final IGestorCitasPacienteService servicioCitasPaciente;
    private final String pacienteIdSesion;
    private final String tokenSesion;

    public PortalPacienteCitasControlador(IGestorCitasPacienteService servicioCitasPaciente,
                                          String pacienteIdSesion,
                                          String tokenSesion) {
        if (servicioCitasPaciente == null) {
            throw new IllegalArgumentException("El servicio de citas del paciente es obligatorio");
        }
        if (pacienteIdSesion == null || pacienteIdSesion.isBlank()) {
            throw new IllegalArgumentException("El identificador del paciente en sesión es obligatorio");
        }
        if (tokenSesion == null || tokenSesion.isBlank()) {
            throw new IllegalArgumentException("El token de sesión SSO es obligatorio");
        }
        this.servicioCitasPaciente = servicioCitasPaciente;
        this.pacienteIdSesion = pacienteIdSesion.trim();
        this.tokenSesion = tokenSesion.trim();
    }

    public List<CitaDTO> obtenerTodasMisCitas() {
        return servicioCitasPaciente.consultarMisCitas(pacienteIdSesion, null, tokenSesion);
    }

    public List<CitaDTO> obtenerMisCitasPorEstado(String estado) {
        return servicioCitasPaciente.consultarMisCitas(pacienteIdSesion, estado, tokenSesion);
    }

    public CitaDTO verDetalleCita(String citaId) {
        return servicioCitasPaciente.consultarDetalleCitaPropia(pacienteIdSesion, citaId, tokenSesion);
    }

    public List<TramoHorarioDTO> buscarHuecosParaCambio(String medicoId, LocalDate fecha) {
        return servicioCitasPaciente.consultarHuecosDisponiblesCambio(medicoId, fecha, tokenSesion);
    }

    public CitaDTO solicitarReprogramacion(String citaId, LocalDate nuevaFecha, String nuevaFranja) {
        return servicioCitasPaciente.reprogramarCitaPropia(pacienteIdSesion, citaId, nuevaFecha, nuevaFranja, tokenSesion);
    }

    public CitaDTO solicitarCancelacion(String citaId, String motivo) {
        return servicioCitasPaciente.cancelarCitaPropia(pacienteIdSesion, citaId, motivo, tokenSesion);
    }

    public String formatearResumenCitas() {
        List<CitaDTO> citas = obtenerTodasMisCitas();
        if (citas.isEmpty()) {
            return "No tienes citas radiológicas registradas actualmente.";
        }
        StringBuilder sb = new StringBuilder("=== MIS CITAS RADIOLÓGICAS ===\n");
        for (CitaDTO c : citas) {
            sb.append(String.format("- [%s] Prueba: %s | Fecha: %s (%s) | Estado: %s%n",
                    c.getCitaId(), c.getCodigoPrueba(), c.getFecha(), c.getFranjaHoraria(), c.getEstado()));
        }
        return sb.toString();
    }
}