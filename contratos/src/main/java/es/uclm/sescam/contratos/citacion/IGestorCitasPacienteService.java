package es.uclm.sescam.contratos.citacion;

import java.time.LocalDate;
import java.util.List;

import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;

public interface IGestorCitasPacienteService {

    List<CitaDTO> consultarMisCitas(String pacienteId, String filtroEstado, String tokenSSO);

    CitaDTO consultarDetalleCitaPropia(String pacienteId, String citaId, String tokenSSO);

    List<TramoHorarioDTO> consultarHuecosDisponiblesCambio(String medicoId, LocalDate fecha, String tokenSSO);

    CitaDTO reprogramarCitaPropia(String pacienteId, String citaId, LocalDate nuevaFecha, String nuevaFranja, String tokenSSO);

    CitaDTO cancelarCitaPropia(String pacienteId, String citaId, String motivo, String tokenSSO);
}