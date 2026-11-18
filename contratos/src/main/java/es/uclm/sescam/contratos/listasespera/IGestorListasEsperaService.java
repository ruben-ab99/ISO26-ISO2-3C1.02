package es.uclm.sescam.contratos.listasespera;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface IGestorListasEsperaService {
    SolicitudEsperaDTO registrarEnListaEspera(String pacienteId, String tipoPrueba, PrioridadEspera prioridad, String tokenSSO);
    List<SolicitudEsperaDTO> consultarListaEspera(String tipoPrueba, String tokenSSO);
    boolean actualizarPrioridad(String solicitudId, PrioridadEspera nuevaPrioridad, String tokenSSO);
    int contarHuecosLibres(String medicoId, LocalDate fecha, String tokenSSO);
    SolicitudEsperaDTO asignarHuecoLibre(String solicitudId, String medicoId, LocalDate fecha,
                                         LocalTime horaInicio, LocalTime horaFin, String tokenSSO);
}