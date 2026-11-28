package es.uclm.sescam.contratos.agendas;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface IGestorAgendasService {
    AgendaDiariaDTO configurarAgendaDiaria(String medicoId, LocalDate fecha, LocalTime horaInicio,
                                           LocalTime horaFin, int duracionHuecoMinutos, String tokenSSO);
    AgendaDiariaDTO consultarAgendaMedico(String medicoId, LocalDate fecha, String tokenSSO);
    boolean bloquearTramoAgenda(String agendaId, LocalTime horaInicioTramo, LocalTime horaFinTramo,
                                String motivoBloqueo, String tokenSSO);
    boolean cerrarAgendaDiaria(String agendaId, String tokenSSO);

    // Consulta de solo lectura para los informes operativos de CU-16 (ADR-017).
    default List<AgendaDiariaDTO> listarAgendasEntreFechas(LocalDate desde, LocalDate hasta, String tokenSSO) {
        return List.of();
    }
}