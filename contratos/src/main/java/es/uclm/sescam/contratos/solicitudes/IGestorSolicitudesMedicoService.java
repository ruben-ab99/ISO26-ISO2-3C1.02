package es.uclm.sescam.contratos.solicitudes;

import java.time.LocalDate;
import java.util.List;

import es.uclm.sescam.contratos.agendas.AgendaDiariaDTO;
import es.uclm.sescam.contratos.agendas.TramoHorarioDTO;
import es.uclm.sescam.contratos.prescripciones.SolicitudPruebaDTO;

public interface IGestorSolicitudesMedicoService {
    SolicitudPruebaDTO registrarSolicitudMedico(String pacienteId, String medicoId, String codigoPrueba,
                                                int prioridad, String justificacionClinica, String tokenSSO);
    List<SolicitudPruebaDTO> consultarSolicitudesMedico(String medicoId, String filtroEstado, String tokenSSO);
    SolicitudPruebaDTO actualizarEstadoSolicitud(String solicitudId, String nuevoEstado, String motivo, String tokenSSO);
    AgendaDiariaDTO consultarAgendaRealizacion(String medicoRealizadorId, LocalDate fecha, String tokenSSO);
    List<TramoHorarioDTO> consultarHuecosDisponiblesRealizacion(String medicoRealizadorId, LocalDate fecha, String tokenSSO);
}