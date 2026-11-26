package es.uclm.sescam.contratos.analitica;

import java.time.LocalDate;

public interface IEstadisticasSolicitudesService {
    EstadisticasSolicitudesDTO consultarEstadisticasSolicitudes(String medicoId, LocalDate desde, LocalDate hasta, String tokenSSO);
}