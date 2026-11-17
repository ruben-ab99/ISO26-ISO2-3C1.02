package es.uclm.sescam.contratos.citacion;

import java.time.LocalDate;
import java.util.List;

public interface IGestionCitasService {
    CitaDTO programarCita(String pacienteId, String medicoId, String codigoPrueba, LocalDate fecha, String franjaHoraria);
    CitaDTO consultarCita(String citaId);
    List<CitaDTO> listarCitasPaciente(String pacienteId);
    CitaDTO reprogramarCita(String citaId, LocalDate nuevaFecha, String nuevaFranja);
    CitaDTO cancelarCita(String citaId, String motivo);
}