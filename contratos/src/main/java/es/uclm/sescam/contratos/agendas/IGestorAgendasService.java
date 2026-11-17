package es.uclm.sescam.contratos.agendas;

import java.time.LocalDate;
import java.time.LocalTime;

public interface IGestorAgendasService {
    AgendaDiariaDTO configurarAgendaDiaria(String medicoId, LocalDate fecha, LocalTime horaInicio,
                                           LocalTime horaFin, int duracionHuecoMinutos, String tokenSSO);
    AgendaDiariaDTO consultarAgendaMedico(String medicoId, LocalDate fecha, String tokenSSO);
    boolean bloquearTramoAgenda(String agendaId, LocalTime horaInicioTramo, LocalTime horaFinTramo,
                                String motivoBloqueo, String tokenSSO);
    boolean cerrarAgendaDiaria(String agendaId, String tokenSSO);
}